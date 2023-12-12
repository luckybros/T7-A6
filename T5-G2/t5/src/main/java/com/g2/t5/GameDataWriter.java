package com.g2.t5;

import org.apache.commons.csv.*;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.g2.Model.Game;

public class GameDataWriter {

    private final HttpClient httpClient = HttpClientBuilder.create().build();

    private static String CSV_FILE_PATH = "/app/AUTName/StudentLogin/GameId/GameData.csv";

    public long getGameId() {
        long gameId = -1;

        try {
            Reader reader = new FileReader(CSV_FILE_PATH);
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT);

            List<CSVRecord> records = csvParser.getRecords();

            if (!records.isEmpty()) {
                CSVRecord lastRecord = records.get(records.size() - 1);
                gameId = Long.parseLong(lastRecord.get(0));
            }

            csvParser.close();
            reader.close();
        } catch (IOException e) {
            System.out.println("Errore durante la lettura del file CSV");
            e.printStackTrace();
        }

        return gameId;
    }

    public JSONObject saveGame(Game game) {
        try {
            String time = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            JSONObject obj = new JSONObject();

            obj.put("difficulty", game.getDifficulty());
            obj.put("name", game.getName());
            obj.put("description", game.getDescription());
            obj.put("startedAt", time);

            JSONArray playersArray = new JSONArray();
            playersArray.put(String.valueOf(game.getPlayerId()));

            obj.put("players", playersArray);

            HttpPost httpPost = new HttpPost("http://t4-g18-app-1:3000/games");
            StringEntity jsonEntity = new StringEntity(obj.toString(), ContentType.APPLICATION_JSON);

            httpPost.setEntity(jsonEntity);

            HttpResponse httpResponse = httpClient.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode > 299) {
                System.err.println(EntityUtils.toString(httpResponse.getEntity()));
                return null;
            }

            HttpEntity responseEntity = httpResponse.getEntity();
            String responseBody = EntityUtils.toString(responseEntity);
            JSONObject responseObj = new JSONObject(responseBody);

            Long gameID = responseObj.getLong("id");

            JSONObject round = new JSONObject();
            round.put("gameId", gameID);
            round.put("testClassId", game.getTestedClass());
            round.put("startedAt", time);

            httpPost = new HttpPost("http://t4-g18-app-1:3000/rounds");
            jsonEntity = new StringEntity(round.toString(), ContentType.APPLICATION_JSON);

            httpPost.setEntity(jsonEntity);

            httpResponse = httpClient.execute(httpPost);
            statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode > 299) {
                System.err.println(EntityUtils.toString(httpResponse.getEntity()));
                return null;
            }

            responseEntity = httpResponse.getEntity();
            responseBody = EntityUtils.toString(responseEntity);
            responseObj = new JSONObject(responseBody);

            // salvo il round id che l'Api mi restituisce
            Integer round_id = responseObj.getInt("id");

            JSONObject turn = new JSONObject();

            turn.put("players", playersArray);
            turn.put("roundId", round_id);
            turn.put("startedAt", time);

            httpPost = new HttpPost("http://t4-g18-app-1:3000/turns");
            jsonEntity = new StringEntity(turn.toString(), ContentType.APPLICATION_JSON);

            httpPost.setEntity(jsonEntity);

            httpResponse = httpClient.execute(httpPost);
            statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode > 299) {
                System.err.println(EntityUtils.toString(httpResponse.getEntity()));
                return null;
            }

            responseEntity = httpResponse.getEntity();
            responseBody = EntityUtils.toString(responseEntity);

            JSONArray responseArrayObj = new JSONArray(responseBody);

            // salvo il turn id che l'Api mi restituisce
            Integer turn_id = responseArrayObj.getJSONObject(0).getInt("id");

            JSONObject resp = new JSONObject();
            resp.put("game_id", gameID);
            resp.put("round_id", round_id);
            resp.put("turn_id", turn_id);

            return resp;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public boolean saveGameCSV(Game game) {
        File file = new File(CSV_FILE_PATH);

        try {
            Writer writer = new FileWriter(file, true);
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);

            if (!file.exists()) {
                file.createNewFile();

                csvPrinter.printRecord(
                    "GameID",
                    "Name",
                    "Round",
                    "Class",
                    "Description",
                    "Difficulty",
                    "CreatedAt",
                    "UpdatedAt",
                    "StartedAt",
                    "ClosedAt",
                    "PlayerID",
                    "Robot"
                );
            }

            csvPrinter.printRecord(
                game.getId(),
                game.getName(),
                game.getRound(),
                game.getTestedClass(),
                game.getDescription(),
                game.getDifficulty(),
                game.getCreatedAt().toString(),
                game.getUpdateAt().toString(),
                game.getStartedAt().toString(),
                game.getClosedAt().toString(),
                game.getPlayerId(),
                game.getRobot()      
            );

            csvPrinter.flush();
            csvPrinter.close();
            writer.close();

            System.out.println("Game è stato salvato correttamente nel file CSV.");

            return true;
        } catch (IOException e) {
            System.out.println("Errore durante la scrittura del file CSV.");
            e.printStackTrace();
            return false;
        }

    }
}