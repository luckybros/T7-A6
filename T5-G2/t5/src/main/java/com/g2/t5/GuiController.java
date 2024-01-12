package com.g2.t5;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.g2.Model.ClassUT;
import com.g2.Model.Game;

import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.*;

import java.text.SimpleDateFormat;
import java.util.*;

@CrossOrigin
@Controller
public class GUIController {

    // Player p1 = Player.getInstance();
    // long globalID;
    // String valueclass = "NULL";
    // String valuerobot = "NULL";
    // private Integer myClass = null;
    // private Integer myRobot = null;
    // private Map<Integer, String> hashMap = new HashMap<>();
    // private Map<Integer, String> hashMap2 = new HashMap<>();
    // private final FileController fileController;

    private RestTemplate restTemplate;

    private GameDataWriter gameDataWriter = new GameDataWriter();
    private Game g = new Game();

    @Autowired
    public GUIController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // @GetMapping("/login")
    // public String loginPage() {
    // return "login"; // Nome del template Thymeleaf per la pagina1.html
    // }

    public List<String> getLevels(String className) {
        List<String> result = new ArrayList<String>();

        int i;
        for (i = 1; i < 11; i++) {
            try {
                restTemplate.getForEntity("http://t4-g18-app-1:3000/robots?testClassId=" + className
                        + "&type=randoop&difficulty=" + String.valueOf(i), Object.class);
            } catch (Exception e) {
                break;
            }

            result.add(String.valueOf(i));
        }

        for (int j = i; j - i + 1 < i; j++) {
            try { // aggiunto
                restTemplate.getForEntity("http://t4-g18-app-1:3000/robots?testClassId=" + className
                        + "&type=evosuite&difficulty=" + String.valueOf(j - i + 1), Object.class);
            } catch (Exception e) {
                break;
            }

            result.add(String.valueOf(j));
        }

        return result;
    }

    public List<ClassUT> getClasses() {
        ResponseEntity<List<ClassUT>> responseEntity = restTemplate.exchange("http://manvsclass-controller-1:8080/home",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<ClassUT>>() {
                });

        return responseEntity.getBody();
    }

    @GetMapping("/main")
    public String GuiController(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
        formData.add("jwt", jwt);

        Boolean isAuthenticated = restTemplate.postForObject("http://t23-g1-app-1:8080/validateToken", formData,
                Boolean.class);

        if (isAuthenticated == null || !isAuthenticated)
            return "redirect:/login";

        // fileController.listFilesInFolder("/app/AUTName/AUTSourceCode");
        // int size = fileController.getClassSize();

        List<ClassUT> classes = getClasses();

        Map<Integer, String> hashMap = new HashMap<>();
        Map<Integer, List<MyData>> robotList = new HashMap<>();
        // Map<Integer, List<String>> evosuiteLevel = new HashMap<>();

        for (int i = 0; i < classes.size(); i++) {
            String valore = classes.get(i).getName();

            List<String> levels = getLevels(valore);
            System.out.println(levels);

            List<String> evo = new ArrayList<>(); // aggiunto
            for (int j = 0; j < levels.size(); j++) { // aggiunto
                if (j >= levels.size() / 2)
                    evo.add(j, levels.get(j - (levels.size() / 2)));
                else {
                    evo.add(j, levels.get(j + (levels.size() / 2)));
                }
            }
            System.out.println(evo);

            List<MyData> struttura = new ArrayList<>();

            for (int j = 0; j < levels.size(); j++) {
                MyData strutt = new MyData(levels.get(j), evo.get(j));
                struttura.add(j, strutt);
            }
            for (int j = 0; j < struttura.size(); j++)
                System.out.println(struttura.get(j).getList1());
            hashMap.put(i, valore);
            robotList.put(i, struttura);
            // evosuiteLevel.put(i, evo);
        }

        model.addAttribute("hashMap", hashMap);

        // hashMap2 = com.g2.Interfaces.t8.RobotList();

        model.addAttribute("hashMap2", robotList);

        // model.addAttribute("evRobot", evosuiteLevel); //aggiunto
        return "main";
    }

    @GetMapping("/report")
    public String reportPage(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
        formData.add("jwt", jwt);

        Boolean isAuthenticated = restTemplate.postForObject("http://t23-g1-app-1:8080/validateToken", formData,
                Boolean.class);

        if (isAuthenticated == null || !isAuthenticated)
            return "redirect:/login";
        // valueclass = hashMap.get(myClass);
        // valuerobot = hashMap2.get(myRobot);

        // System.out.println("IL VALORE DEL ROBOT " + valuerobot + " " + myRobot);
        // System.out.println("Il VALORE DELLA CLASSE " + valueclass + " " + myClass);
        // model.addAttribute("classe", valueclass);
        // model.addAttribute("robot", valuerobot);
        return "report";
    }

    // A3- T4
    private String getCurrentDateTime() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd:MM:yyyy-HH:mm:ss");
        String formattedDate = dateFormat.format(currentDate);

        return formattedDate;
    }

    @PostMapping("/sendGameVariables")
    public ResponseEntity<String> receiveGameVariables(@RequestParam("classe") String classe,
            @RequestParam("robot") String robot, @RequestParam("difficulty") String difficulty) {

        System.out.println("Classe ricevuta: " + classe);
        System.out.println("Robot ricevuto: " + robot);
        System.out.println("Difficoltà ricevuta: " + difficulty);

        g.setTestedClass(classe);
        g.setRobot(robot);
        g.setDifficulty(difficulty);

        g.setCreatedAt(getCurrentDateTime());

        return ResponseEntity.ok("Dati ricevuti con successo");
    }

    @PostMapping("/save-data")
    public ResponseEntity<String> saveGame(@RequestParam("playerId") long playerId,
            HttpServletRequest request) {

        if (!request.getHeader("X-UserID").equals(String.valueOf(playerId)))
            return ResponseEntity.badRequest().body("Unauthorized");

        g.setPlayerId(playerId);
        g.setStartedAt(getCurrentDateTime());

        JSONObject ids = gameDataWriter.saveGame(g);

        if (ids == null)
            return ResponseEntity.badRequest().body("Bad Request");

        long gameID = ids.getLong("game_id");
        int roundID = ids.getInt("round_id");
        int turnID = ids.getInt("turn_id");

        g.setId(gameID);
        g.setRound(roundID);

        boolean saved = gameDataWriter.saveGameCSV(g, turnID);

        if (!saved)
            return ResponseEntity.internalServerError().body("Game not saved in filesystem");

        return ResponseEntity.ok(ids.toString());
    }

    @PostMapping("/update-data")
    public ResponseEntity<String> updateGame(@RequestParam("playerId") long playerId,
            @RequestParam("turnID") int turnID,
            HttpServletRequest request) {

        if (!request.getHeader("X-UserID").equals(String.valueOf(playerId)))
            return ResponseEntity.badRequest().body("Unauthorized");

        g.setUpdatedAt(getCurrentDateTime());

        gameDataWriter.updateGame(g);

        boolean updated = gameDataWriter.updateGameCSV(g, turnID);

        if (!updated)
            return ResponseEntity.internalServerError().body("Game not updated in filesystem");

        boolean newTurnCreated = gameDataWriter.createNextTurnCSV(g, turnID);

        if (!newTurnCreated)
            return ResponseEntity.internalServerError().body("New turn not created in filesystem");

        JSONObject nextTurn = new JSONObject();

        nextTurn.put("turnId", turnID + 1);

        return ResponseEntity.ok(nextTurn.toString());
    }
    // A3-T4

    @GetMapping("/editor")
    public String editorPage(Model model, @CookieValue(name = "jwt", required = false) String jwt) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<String, String>();
        formData.add("jwt", jwt);

        Boolean isAuthenticated = restTemplate.postForObject("http://t23-g1-app-1:8080/validateToken", formData,
                Boolean.class);

        if (isAuthenticated == null || !isAuthenticated)
            return "redirect:/login";
        // model.addAttribute("robot", valuerobot);
        // model.addAttribute("classe", valueclass);

        // model.addAttribute("gameIDj", globalID);

        return "editor";
    }

    // @PostMapping("/download")
    // public ResponseEntity<Resource> downloadFile(@RequestParam("elementId")
    // String elementId) {
    // // Effettua la logica necessaria per ottenere il nome del file
    // // a partire dall'elementId ricevuto, ad esempio, recuperandolo dal database
    // System.out.println("elementId : " + elementId);
    // String filename = elementId;
    // System.out.println("filename : " + filename);
    // String basePath = "/app/AUTName/AUTSourceCode/";
    // String filePath = basePath + filename + ".java";
    // System.out.println("filePath : " + filePath);
    // Resource fileResource = new FileSystemResource(filePath);

    // HttpHeaders headers = new HttpHeaders();
    // headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" +
    // filename + ".java");
    // headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");

    // return ResponseEntity.ok()
    // .headers(headers)
    // .body(fileResource);
    // }

    // @GetMapping("/change_password")
    // public String showChangePasswordPage() {
    // return "change_password";
    // }

    // @PostMapping("/login-variabiles")
    // public ResponseEntity<String> receiveLoginData(@RequestParam("var1") String
    // username,
    // @RequestParam("var2") String password) {

    // System.out.println("username : " + username);
    // System.out.println("password : " + password);

    // p1.setUsername(username);
    // p1.setPassword(password);

    // // Salva i valori in una variabile o esegui altre operazioni necessarie
    // if (com.g2.Interfaces.t2_3.verifyLogin(username, password)) {
    // return ResponseEntity.ok("Dati ricevuti con successo");
    // }

    // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Si è
    // verificato un errore interno");
    // }

}
