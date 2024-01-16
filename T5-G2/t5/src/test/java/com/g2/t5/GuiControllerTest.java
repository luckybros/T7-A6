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
    private Game game;

    @MockBean
    private GameDataWriter gameDataWriter;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @Test
    public void testReceiveGameVariables() throws Exception {
        RestAssuredMockMvc
                .given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new JSONObject()
                        .put("classe", "range")
                        .put("robot", "randoop")
                        .put("difficulty", "1")
                        .toString())
                .when()
                .post("/api/sendGameVariables")
                .then()
                .statusCode(200);
    }

    @Test
    public void testSaveGameNotNull() throws Exception {
        Mockito.when(gameDataWriter.saveGame(game)).thenReturn(new JSONObject()
                .put("game_id", 1)
                .put("round_id", 1)
                .put("turn_id", 1));

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
                .put("turn_id", 1));

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

    @Test
    public void testUpdateGameNotNull() throws Exception {
        Mockito.when(gameDataWriter.updateGame(game, 1)).thenReturn(true);

        Mockito.when(gameDataWriter.updateGameCSV(game, 1)).thenReturn(true);

        Mockito.when(gameDataWriter.createNextTurnCSV(game, 1)).thenReturn(true);

        RestAssuredMockMvc
                .given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-UserID", "1")
                .body(new JSONObject()
                        .put("playerId", 1)
                        .put("turnId", 1)
                        .toString())
                .when()
                .post("/api/update-data")
                .then()
                .statusCode(200)
                .body("turn_id", notNullValue());
    }

    @Test
    public void testUpdateGameEqualValues() throws Exception {
        Mockito.when(gameDataWriter.updateGame(game, 1)).thenReturn(true);

        Mockito.when(gameDataWriter.updateGameCSV(game, 1)).thenReturn(true);

        Mockito.when(gameDataWriter.createNextTurnCSV(game, 1)).thenReturn(true);

        RestAssuredMockMvc
                .given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-UserID", "1")
                .body(new JSONObject()
                        .put("playerId", 1)
                        .put("turnId", 1)
                        .toString())
                .when()
                .post("/api/update-data")
                .then()
                .statusCode(200)
                .body("turn_id", Matchers.equalTo(2));
    }
}
