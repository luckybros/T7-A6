package com.g2.t5;

import io.restassured.module.mockmvc.RestAssuredMockMvc;

import static org.hamcrest.Matchers.notNullValue;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GuiController.class)
public class GuiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @Test
    public void testSaveGame() throws Exception {
        // Definisci il corpo della tua richiesta JSON
        String requestBody = "{ \"playerId\": 1 }";  // Sostituisci con i valori appropriati

        // Esegui la richiesta HTTP POST simulata
        RestAssuredMockMvc.given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .header("X-UserID", "1")  // Sostituisci con il valore appropriato
                .body(requestBody)
            .when()
                .post("/save-data")
            .then()
                .statusCode(200)
                .body("game_id", notNullValue())
                .body("round_id", notNullValue())
                .body("turn_id", notNullValue());
    }
}

