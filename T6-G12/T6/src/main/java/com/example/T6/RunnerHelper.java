package com.example.T6;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import org.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;

public class RunnerHelper {
    public class ScorePair {
        private String outCompile;
        private int coverage;
    
        public ScorePair(String outCompile, int coverage) {
            this.outCompile = outCompile;
            this.coverage = coverage;
        }
    
        public String getOutCompile() {
            return outCompile;
        }
    
        public int getCoverage() {
            return coverage;
        }
    }

    public final HttpClient httpClient = HttpClientBuilder.create().build();
    
    public void saving(JSONObject result, HttpServletRequest request) throws ClientProtocolException, IOException {
        // conclusione e salvataggio partita
        // chiusura turno con vincitore
        HttpPut httpPut = new HttpPut("http://t4-g18-app-1:3000/turns/" + String.valueOf(request.getParameter("turnId")));
    
        JSONObject obj = new JSONObject();
        obj.put("scores", result.getString("score"));
        obj.put("isWinner", result.getString("win"));
    
        String time = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
        obj.put("closedAt", time);
    
        StringEntity jsonEntity = new StringEntity(obj.toString(), ContentType.APPLICATION_JSON);
    
        httpPut.setEntity(jsonEntity);
    
        HttpResponse response = httpClient.execute(httpPut);
        httpPut.releaseConnection();
    
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode > 299) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore in put turn");
        }
        
        // chiusura round
        httpPut = new HttpPut("http://t4-g18-app-1:3000/rounds/" + String.valueOf(request.getParameter("roundId")));
    
        obj = new JSONObject();
    
        obj.put("closedAt", time);
    
        jsonEntity = new StringEntity(obj.toString(), ContentType.APPLICATION_JSON);
    
        httpPut.setEntity(jsonEntity);
    
        response = httpClient.execute(httpPut);
        httpPut.releaseConnection();
    
        statusCode = response.getStatusLine().getStatusCode();
        if (statusCode > 299) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore in put round");
        }
    
        // chiusura gioco
        httpPut = new HttpPut("http://t4-g18-app-1:3000/games/" + String.valueOf(request.getParameter("gameId")));
    
        obj = new JSONObject();
        obj.put("closedAt", time);
    
        jsonEntity = new StringEntity(obj.toString(), ContentType.APPLICATION_JSON);
    
        httpPut.setEntity(jsonEntity);
    
        response = httpClient.execute(httpPut);
        httpPut.releaseConnection();
        
        statusCode = response.getStatusLine().getStatusCode();
        if (statusCode > 299) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore in put game");
        }
    
    }
    
    public int roboScoreNormalGet(URIBuilder builder, HttpServletRequest request) throws URISyntaxException, ClientProtocolException, IOException {
        builder.setParameter("type", request.getParameter("type"));
        builder.setParameter("difficulty", request.getParameter("difficulty"));
    
        HttpGet get = new HttpGet(builder.build());
        HttpResponse response = httpClient.execute(get);
        get.releaseConnection();
        // Verifica lo stato della risposta
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode > 299) {
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore in robots");
        }
    
        // Leggi il contenuto dalla risposta
        HttpEntity entity = response.getEntity();
        String responseBody = EntityUtils.toString(entity);
        JSONObject responseObj = new JSONObject(responseBody);
    
        String score = responseObj.getString("scores");
        Integer roboScore = Integer.parseInt(score);
    
        return roboScore.intValue();
    }
    
    public JSONObject responseBuilderNormal(int roboScore, ScorePair userScore) {
        JSONObject result = new JSONObject();
    
        result.put("outCompile", userScore.getOutCompile());
        result.put("win", userScore.getCoverage() >= roboScore);
        result.put("robotScore", roboScore);
        result.put("score", String.valueOf(userScore.getCoverage()));
    
        return result;
    }
    
    public JSONObject normalRunner(URIBuilder builder, ScorePair userScore, HttpServletRequest request) throws ClientProtocolException, IOException, URISyntaxException {
        int roboScore = roboScoreNormalGet(builder, request);
    
        JSONObject result = responseBuilderNormal(roboScore, userScore);
        saving(result, request);
        return result;
    }
    
    public JSONObject bossRushRunner(URIBuilder builder, ScorePair userScore, HttpServletRequest request) throws ClientProtocolException, IOException, ParseException, URISyntaxException {
        List<Integer> randoopScores = roboScoresBossRushGet(builder, "randoop");
        List<Integer> evosuiteScores = roboScoresBossRushGet(builder, "evosuite");
    
        JSONObject result = responseBuilderBossRush(randoopScores, evosuiteScores, userScore);
        saving(result, request);
        return result;
    }
    
    public JSONObject responseBuilderBossRush(List<Integer> randoopScores, List<Integer> evosuiteScores, ScorePair userScore) {
        JSONObject result = new JSONObject();
    
        int i = 0;
        boolean globalWin = false;
    
        int numberOfBeaten = 0;
        int numberOfUnbeaten = 0;
        for(i = 0; i < randoopScores.size(); i++) {
            if(randoopScores.get(i) >= userScore.getCoverage()) { 
                result.put("beaten"+String.valueOf(numberOfBeaten+1), String.valueOf(i+1) + "&" + String.valueOf(randoopScores.get(i)));
                numberOfBeaten++;
            }
            else{
                result.put("unbeaten"+String.valueOf(numberOfUnbeaten+1), String.valueOf(i+1) + "&" + String.valueOf(randoopScores.get(i)));
                numberOfUnbeaten++;
            }
        }
    
        for(i = 0; i < evosuiteScores.size(); i++) {
            if(evosuiteScores.get(i) >= userScore.getCoverage()) { 
                result.put("beaten"+String.valueOf(numberOfBeaten+1), String.valueOf(i+1+randoopScores.size()) + "&" + String.valueOf(evosuiteScores.get(i)));
                numberOfBeaten++;
            }
            else{
                result.put("unbeaten"+String.valueOf(numberOfUnbeaten+1), String.valueOf(i+1+randoopScores.size()) + "&" + String.valueOf(evosuiteScores.get(i)));
                numberOfUnbeaten++;
            }
        }
    
        if(numberOfBeaten == randoopScores.size() + evosuiteScores.size())
            globalWin = true;
    
        result.put("outCompile", userScore.getOutCompile());
        result.put("score", String.valueOf(userScore.getCoverage()));
        result.put("win", globalWin);
        result.put("numberOfBeaten", numberOfBeaten);
        result.put("numberOfUnbeaten", numberOfUnbeaten);
    
        return result;
    }
    
    public List<Integer> roboScoresBossRushGet(URIBuilder builder, String type) throws URISyntaxException, ParseException, IOException {
        URIBuilder helper = builder;
        helper.setParameter("type", type);

        List<Integer> robot = new ArrayList<>();
        HttpResponse response; 
        for(int i = 1; i < 11; i++) {
            URIBuilder subHelper = helper;
            subHelper.setParameter("difficulty", String.valueOf(i));
    
            HttpGet get = new HttpGet(subHelper.build());
            try {
                response = httpClient.execute(get);
            } catch (Exception e) {
                break; // arriva a questo break fintantochè non trova nulla
            }
            get.releaseConnection();
    
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode > 299) {
                throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Errore in " + type + " " + i);
            }
    
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            JSONObject responseObj = new JSONObject(responseBody);
    
            String score = responseObj.getString("scores");
            Integer roboScore = Integer.parseInt(score);
            robot.add(roboScore);
        }
        return robot;
    }
    
    public ScorePair getUserScore(HttpServletRequest request) throws ClientProtocolException, IOException {
        HttpPost httpPost = new HttpPost("http://remoteccc-app-1:1234/compile-and-codecoverage");
    
        // passa un oggetto JSON con il nome della classe da testare e di testing e il loro codice
        JSONObject obj = new JSONObject();
        obj.put("testingClassName", request.getParameter("testingClassName"));
        obj.put("testingClassCode", request.getParameter("testingClassCode"));
        obj.put("underTestClassName", request.getParameter("underTestClassName"));
        obj.put("underTestClassCode", request.getParameter("underTestClassCode"));
    
        StringEntity jsonEntity = new StringEntity(obj.toString(), ContentType.APPLICATION_JSON);
    
        httpPost.setEntity(jsonEntity);
    
        HttpResponse response = httpClient.execute(httpPost);
    
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode > 299) {
            System.out.println("Errore in compilecodecoverage");
            return new ScorePair("error", -1);
        }
    
        // entity contiene il corpo della risposta contenente la coverage del giocatore
        HttpEntity entity = response.getEntity();
    
        // trasforma la risposta in stringa e crea un oggetto JSON con la risposta,
        // sempre contenente la coverage del giocatore 
        String responseBody = EntityUtils.toString(entity);
        JSONObject responseObj = new JSONObject(responseBody);
    
        String coverage = responseObj.getString("coverage");
        String outCompile = responseObj.getString("outCompile");
        // PRESA DELLO SCORE UTENTE
        // percentuale di coverage dell'utente trasformata in intero
        return new ScorePair(outCompile, ParseUtil.LineCoverage(coverage));
    }
    

}
