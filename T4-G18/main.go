package main

import (
	"context"
	"database/sql"
	"embed"
	"encoding/csv"
	"encoding/json"
	"errors"
	"flag"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"

	"github.com/alarmfox/game-repository/api"
	"github.com/alarmfox/game-repository/api/game"
	"github.com/alarmfox/game-repository/api/robot"
	"github.com/alarmfox/game-repository/api/round"
	"github.com/alarmfox/game-repository/api/turn"
	"github.com/alarmfox/game-repository/limiter"
	"github.com/alarmfox/game-repository/model"
	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/go-chi/cors"
	mw "github.com/go-openapi/runtime/middleware"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	"golang.org/x/sync/errgroup"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"

	"path/filepath"
	"sort"
)

type Configuration struct {
	PostgresUrl     string        `json:"postgresUrl"`
	ListenAddress   string        `json:"listenAddress"`
	ApiPrefix       string        `json:"apiPrefix"`
	DataDir         string        `json:"dataDir"`
	EnableSwagger   bool          `json:"enableSwagger"`
	CleanupInterval time.Duration `json:"cleanupInterval"`
	RateLimiting    struct {
		Burst   int     `json:"burst"`
		MaxRate float64 `json:"maxRate"`
		Enabled bool    `json:"enabled"`
	} `json:"rateLimiting"`
	Authentication struct {
		Enabled      bool   `json:"enabled"`
		HeaderKey    string `json:"headerKey"`
		AuthEndpoint string `json:"authEndpoint"`
		Method       string `json:"method"`
	}
}

//go:embed postman
var postmanDir embed.FS

func main() {
	var (
		configPath = flag.String("config", "config.json", "Path for configuration")
		ctx        = context.Background()
	)
	flag.Parse()

	fcontent, err := os.ReadFile(*configPath)
	if err != nil {
		log.Fatal(err)
	}

	var configuration Configuration
	if err := json.Unmarshal(fcontent, &configuration); err != nil {
		log.Fatal(err)
	}

	makeDefaults(&configuration)

	ctx, canc := signal.NotifyContext(ctx, syscall.SIGTERM, os.Interrupt)
	defer canc()

	if err := run(ctx, configuration); err != nil {
		log.Fatal(err)
	}
}

func run(ctx context.Context, c Configuration) error {

	db, err := gorm.Open(postgres.Open(c.PostgresUrl), &gorm.Config{
		SkipDefaultTransaction: true,
		TranslateError:         true,
	})

	if err != nil {
		return err
	}

	err = db.AutoMigrate(
		&model.Game{},
		&model.Round{},
		&model.Player{},
		&model.Turn{},
		&model.Metadata{},
		&model.PlayerGame{},
		&model.Robot{})

	if err != nil {
		return err
	}
	if err := db.SetupJoinTable(&model.Game{}, "Players", &model.PlayerGame{}); err != nil {
		return err
	}

	if err := os.Mkdir(c.DataDir, os.ModePerm); err != nil && !errors.Is(err, os.ErrExist) {
		return fmt.Errorf("cannot create data directory: %w", err)
	}

	r := chi.NewRouter()

	// basic cors
	r.Use(cors.Handler(cors.Options{
		AllowedOrigins:   []string{"*"},
		AllowedMethods:   []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowedHeaders:   []string{"Content-Type", "Accept", "Authorization"},
		ExposedHeaders:   []string{"Link"},
		AllowCredentials: false,
		MaxAge:           300, // Maximum value not ignored by any of major browsers
	}))

	if c.EnableSwagger {
		r.Group(func(r chi.Router) {
			opts := mw.SwaggerUIOpts{SpecURL: "/public/postman/schemas/index.yaml"}
			sh := mw.SwaggerUI(opts, nil)
			r.Handle("/docs", sh)
			r.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
				http.Redirect(w, r, "/docs", http.StatusMovedPermanently)
			})
		})
	}

	// serving Postman directory for documentation files
	fs := http.FileServer(http.FS(postmanDir))
	r.Mount("/public/", http.StripPrefix("/public/", fs))

	// metrics endpoint
	r.Handle("/metrics", promhttp.Handler())

	clientLimiter := limiter.NewClientLimiter(c.RateLimiting.Burst, c.RateLimiting.MaxRate)
	r.Group(func(r chi.Router) {
		r.Use(middleware.RealIP)
		r.Use(middleware.Logger)
		r.Use(middleware.Recoverer)

		if c.RateLimiting.Enabled {
			r.Use(clientLimiter.Limit)
		}

		if c.Authentication.Enabled {
			r.Use(api.WithJWTAuthentication(api.JWTAuthenticationConfig{
				HeaderKey:    c.Authentication.HeaderKey,
				Method:       c.Authentication.Method,
				AuthEndpoint: c.Authentication.AuthEndpoint,
			}))
		}
		var (

			// game endpoint
			gameController = game.NewController(game.NewRepository(db))

			// round endpoint
			roundController = round.NewController(round.NewRepository(db))

			// turn endpoint
			turnController = turn.NewController(turn.NewRepository(db, c.DataDir))

			// robot endpoint
			robotController = robot.NewController(robot.NewRobotStorage(db))
		)

		r.Mount(c.ApiPrefix, setupRoutes(
			gameController,
			roundController,
			turnController,
			robotController,
		))
	})
	log.Printf("listening on %s", c.ListenAddress)
	g, ctx := errgroup.WithContext(ctx)

	g.Go(func() error {
		return startHttpServer(ctx, r, c.ListenAddress)
	})

	g.Go(func() error {
		for {
			select {
			case <-time.After(c.CleanupInterval):
				_, err := cleanup(db)
				if err != nil {
					log.Print(err)
				}
			case <-ctx.Done():
				return nil
			}
		}
	})

	if c.RateLimiting.Enabled {
		g.Go(func() error {
			for {
				select {
				case <-ctx.Done():
					return nil
				case <-time.After(time.Minute):
					clientLimiter.Cleanup(3 * time.Minute)
				}
			}
		})

	}

	return g.Wait()

}

func startHttpServer(ctx context.Context, r chi.Router, addr string) error {
	server := http.Server{
		Addr:              addr,
		Handler:           r,
		ReadTimeout:       time.Minute,
		WriteTimeout:      time.Minute,
		IdleTimeout:       time.Minute,
		ReadHeaderTimeout: 10 * time.Second,
		MaxHeaderBytes:    1024 * 8,
	}

	errCh := make(chan error)
	defer close(errCh)
	go func() {
		if err := server.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			errCh <- err
		}
	}()

	select {
	case <-ctx.Done():
	case err := <-errCh:
		return err
	}

	ctx, canc := context.WithTimeout(context.Background(), time.Second*10)
	defer canc()

	return server.Shutdown(ctx)
}

func cleanup(db *gorm.DB) (int64, error) {
	var (
		metadata []model.Metadata
		err      error
		n        int64
	)

	err = db.Transaction(func(tx *gorm.DB) error {
		err := tx.
			Where("turn_id IS NULL").
			Find(&metadata).
			Count(&n).
			Error

		if err != nil {
			return err
		}

		var deleted []int64
		for _, m := range metadata {
			if err := os.Remove(m.Path); err != nil && !errors.Is(err, os.ErrNotExist) {
				log.Print(err)
			} else {
				deleted = append(deleted, m.ID)
			}
		}

		return tx.Delete(&[]model.Metadata{}, deleted).Error
	})

	return n, err
}

func makeDefaults(c *Configuration) {
	if c.ApiPrefix == "" {
		c.ApiPrefix = "/"
	}

	if c.ListenAddress == "" {
		c.ListenAddress = "localhost:3000"
	}

	if c.DataDir == "" {
		c.DataDir = "data"
	}

	if int64(c.CleanupInterval) == 0 {
		c.CleanupInterval = time.Hour
	}

}

func setupRoutes(gc *game.Controller, rc *round.Controller, tc *turn.Controller, roc *robot.Controller) *chi.Mux {
	r := chi.NewRouter()

	r.Use(api.WithMaximumBodySize(api.DefaultBodySize))

	r.Route("/games", func(r chi.Router) {
		//Get game
		r.Get("/{id}", api.HandlerFunc(gc.FindByID))

		// List games
		r.Get("/", api.HandlerFunc(gc.List))

		// Create game
		r.With(middleware.AllowContentType("application/json")).
			Post("/", api.HandlerFunc(gc.Create))

		// Update game
		r.With(middleware.AllowContentType("application/json")).
			Put("/{id}", api.HandlerFunc(gc.Update))

		// Delete game
		r.Delete("/{id}", api.HandlerFunc(gc.Delete))

	})

	r.Route("/rounds", func(r chi.Router) {
		// Get round
		r.Get("/{id}", api.HandlerFunc(rc.FindByID))

		// List rounds
		r.Get("/", api.HandlerFunc(rc.List))

		// Create round
		r.With(middleware.AllowContentType("application/json")).
			Post("/", api.HandlerFunc(rc.Create))

		// Update round
		r.With(middleware.AllowContentType("application/json")).
			Put("/{id}", api.HandlerFunc(rc.Update))

		// Delete round
		r.Delete("/{id}", api.HandlerFunc(rc.Delete))

	})

	r.Route("/turns", func(r chi.Router) {
		// Get turn
		r.Get("/{id}", api.HandlerFunc(tc.FindByID))

		// List turn
		r.Get("/", api.HandlerFunc(tc.List))

		// Create turn
		r.With(middleware.AllowContentType("application/json")).
			Post("/", api.HandlerFunc(tc.Create))

		// Update turn
		r.With(middleware.AllowContentType("application/json")).
			Put("/{id}", api.HandlerFunc(tc.Update))

		// Delete turn
		r.Delete("/{id}", api.HandlerFunc(tc.Delete))

		// Get turn file
		r.Get("/{id}/files", api.HandlerFunc(tc.Download))

		// Upload turn file
		r.With(middleware.AllowContentType("application/zip"),
			api.WithMaximumBodySize(api.MaxUploadSize)).
			Put("/{id}/files", api.HandlerFunc(tc.Upload))
	})

	r.Route("/robots", func(r chi.Router) {
		// Get robot with filter
		r.Get("/", api.HandlerFunc(roc.FindByFilter))

		// Create robots in bulk
		r.With(middleware.AllowContentType("application/json")).
			Post("/", api.HandlerFunc(roc.CreateBulk))

		// Delete robots by class id
		r.Delete("/", api.HandlerFunc(roc.Delete))

	})

	return r
}

// AGGIUNTO DA FALINO
// Make sure to update the file path in the filePath variable with the actual path to the CSV file.
// Adjust the code accordingly based on your CSV file's structure and the fields you want to compare.

func loadMetadataFromCSV(filePath string) ([]model.Metadata, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	reader := csv.NewReader(file)
	records, err := reader.ReadAll()
	if err != nil {
		return nil, err
	}

	var metadataSlice []model.Metadata
	for _, record := range records {
		// Assuming your CSV has columns like ID, CreatedAt, UpdatedAt, TurnID, Path, etc.
		idStr := record[0]
		createdAtStr := record[1]
		updatedAtStr := record[2]
		turnIDStr := record[3]
		path := record[4]

		id_metadata, err := strconv.ParseInt(idStr, 10, 64)
		if err != nil {
			return nil, err
		}

		createdAt, err := time.Parse(time.RFC3339, createdAtStr)
		if err != nil {
			return nil, err
		}

		updatedAt, err := time.Parse(time.RFC3339, updatedAtStr)
		if err != nil {
			return nil, err
		}

		var turnID sql.NullInt64
		if turnIDStr != "" {
			turnIDInt, err := strconv.ParseInt(turnIDStr, 10, 64)
			if err != nil {
				return nil, err
			}
			turnID = sql.NullInt64{Int64: turnIDInt, Valid: true}
		}

		// Create a Metadata object based on your CSV structure
		metadata := model.Metadata{
			ID:        id_metadata,
			CreatedAt: createdAt,
			UpdatedAt: updatedAt,
			TurnID:    turnID,
			Path:      path,
			// Add more fields as needed
		}

		metadataSlice = append(metadataSlice, metadata)
	}

	return metadataSlice, nil
}

func compareMetadataWithCSV(db *gorm.DB, c Configuration) error {
	// Assuming your CSV file contains Metadata
	// Update the file path based on your actual file location
	filePath := filepath.Join("your", "new", "file", "path", "metadata.csv")
	fileMetadata, err := loadMetadataFromCSV(filePath)
	if err != nil {
		return err
	}

	var dbMetadata []model.Metadata
	if err := db.Find(&dbMetadata).Error; err != nil {
		return err
	}

	// Sort slices for easier comparison
	sort.Slice(fileMetadata, func(i, j int) bool {
		return fileMetadata[i].ID < fileMetadata[j].ID
	})
	sort.Slice(dbMetadata, func(i, j int) bool {
		return dbMetadata[i].ID < dbMetadata[j].ID
	})

	// Compare the slices
	if !metadataSlicesEqual(fileMetadata, dbMetadata) {
		log.Println("Metadata in the file and database are different.")
	} else {
		log.Println("Metadata in the file and database are identical.")
	}

	return nil
}

func metadataSlicesEqual(a, b []model.Metadata) bool {
	if len(a) != len(b) {
		return false
	}

	for i, metadataA := range a {
		if metadataA.ID != b[i].ID || metadataA.CreatedAt != b[i].CreatedAt ||
			metadataA.UpdatedAt != b[i].UpdatedAt || metadataA.TurnID != b[i].TurnID ||
			metadataA.Path != b[i].Path {
			return false
		}
		// Add more fields for comparison if needed
	}

	return true
}

// FINE AGGIUNTO DA FALINO
