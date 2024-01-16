package com.g2.t5;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import static org.hamcrest.Matchers.notNullValue;

import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.g2.Model.Game;

@RunWith(SpringRunner.class)
@WebMvcTest(GuiController.class)
public class GuiControllerTest {
    @MockBean
    private GameDataWriter gameDataWriter;

    @MockBean
    private Game game;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @Test
    public void testSaveGameNotNull() throws Exception {
        Mockito.when(gameDataWriter.saveGame(game)).thenReturn(new JSONObject()
        .put("game_id", 1)
        .put("round_id", 1)
        .put("turn_id", 1)
        );

        Mockito.when(gameDataWriter.saveGameCSV(game, 1)).thenReturn(true);

        RestAssuredMockMvc
        .given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header("X-UserID", "1")
            .body(new JSONObject().put("playerId", 1).toString())
        .when()
            .post("/api/save-data")
        .then()
            .statusCode(200)
            .body("game_id", notNullValue())
            .body("round_id", notNullValue())
            .body("turn_id", notNullValue());
    }

    @Test
    public void testSaveGameEqualValues() throws Exception {
        Mockito.when(gameDataWriter.saveGame(game)).thenReturn(new JSONObject()
        .put("game_id", 1)
        .put("round_id", 1)
        .put("turn_id", 1)
        );

        Mockito.when(gameDataWriter.saveGameCSV(game, 1)).thenReturn(true);

        RestAssuredMockMvc
        .given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .header("X-UserID", "1")
            .body(new JSONObject().put("playerId", 1).toString())
        .when()
            .post("/api/save-data")
        .then()
            .statusCode(200)
            .body("game_id", Matchers.equalTo(1))
            .body("round_id", Matchers.equalTo(1))
            .body("turn_id", Matchers.equalTo(1));
    }
}

/*
 * // Definisci il corpo della tua richiesta JSON
 * String requestBody = "{ \"playerId\": 1 }"; // Sostituisci con i valori
 * appropriati
 * 
 * // Esegui la richiesta HTTP POST simulata
 * RestAssuredMockMvc.given()
 * .contentType(MediaType.APPLICATION_JSON_VALUE)
 * .header("X-UserID", "1") // Sostituisci con il valore appropriato
 * .body(requestBody)
 * .when()
 * .post("/api/save-data")
 * .then()
 * .statusCode(200)
 * .body("game_id", notNullValue())
 * .body("round_id", notNullValue())
 * .body("turn_id", notNullValue());
 */
