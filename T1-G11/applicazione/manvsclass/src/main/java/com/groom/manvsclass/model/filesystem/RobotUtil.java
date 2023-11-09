package com.groom.manvsclass.model.filesystem;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputFilter.Config;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.web.multipart.MultipartFile;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

public class RobotUtil {

	public static int LineCoverage(String path) {
		Element line = null;
		String linecoverage= null;
		try {
			
			File cov = new File(path);
			
			Document doc = Jsoup.parse(cov, null, "", Parser.xmlParser());
			line = doc.getElementsByTag("coverage").get(3);
			linecoverage = String.valueOf(line).substring(32, 35);
			
			linecoverage = linecoverage.split("%", 0)[0];
	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return Integer.valueOf(linecoverage) ;
	}

	public static int LineCoverageE(String path) {
		Float elemento = 0.0f;
		
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            // Leggi la prima riga (la riga 1 è la seconda riga nel conteggio base 1)
			String firstLine = br.readLine();
            
		 	// Leggi la seconda riga
			String secondLine = br.readLine();
 
			if (secondLine != null) {
				// Dividi la seconda riga in elementi separati da virgole
				String[] elements = secondLine.split(",");
				
				// Prendo il valore di copertura per linea
				elemento = Float.parseFloat(elements[2]);
			}
        } catch (IOException e) {
            e.printStackTrace();
        }

		return Math.round(elemento * 100);
	}

    public static void generateAndSaveRobots(String fileName, String cname, MultipartFile multipartFile) throws IOException {
        // RANDOOP - T9			    
		Path directory = Paths.get("/VolumeT9/app/FolderTree/" + cname + "/" + cname + "SourceCode");
		
		try {
			// Verifica se la directory esiste già
			if (!Files.exists(directory)) {
				// Crea la directory
				Files.createDirectories(directory);
				System.out.println("La directory è stata creata con successo.");
			} else {
				System.out.println("La directory esiste già.");
			}
		} catch (Exception e) {
			System.out.println("Errore durante la creazione della directory: " + e.getMessage());
		}

		try (InputStream inputStream = multipartFile.getInputStream()) {
			Path filePath = directory.resolve(fileName);
			System.out.println(filePath.toString());
			Files.copy(inputStream,filePath,StandardCopyOption.REPLACE_EXISTING);
			inputStream.close();
		}

        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command("java", "-jar", "Task9-G19-0.0.1-SNAPSHOT.jar");
        processBuilder.directory(new File("/VolumeT9/app/"));
		
		System.out.println("Prova");
    
        Process process = processBuilder.start();
 
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null)
            System.out.println(line);
			
        reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while ((line = reader.readLine()) != null)
            System.out.println(line);

        try {
			int exitCode = process.waitFor();

			System.out.println("ERRORE CODE: " + exitCode);
		} catch (InterruptedException e) {
			System.out.println(e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		File resultsDir = new File("/VolumeT9/app/FolderTree/" + cname + "/RobotTest/RandoopTest");

		int liv = 0; //livelli di robot prodotti da randoop

        File results [] = resultsDir.listFiles();
        for(File result : results) {
			int score = LineCoverage(result.getAbsolutePath() + "/coveragetot.xml");

			System.out.println(result.toString().substring(result.toString().length() - 7, result.toString().length() - 5));
			int livello = Integer.parseInt(result.toString().substring(result.toString().length() - 7, result.toString().length() - 5));

			System.out.println("La copertura del livello " + String.valueOf(livello) + " è: " + String.valueOf(score));

			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpPost httpPost = new HttpPost("http://t4-g18-app-1:3000/robots");

			JSONArray arr = new JSONArray();

			JSONObject rob = new JSONObject();
			rob.put("scores", String.valueOf(score));
			rob.put("type", "randoop");
			rob.put("difficulty", String.valueOf(livello));
			rob.put("testClassId", cname);

			arr.put(rob);

			JSONObject obj = new JSONObject();
			obj.put("robots", arr);

			StringEntity jsonEntity = new StringEntity(obj.toString(), ContentType.APPLICATION_JSON);

			httpPost.setEntity(jsonEntity);

			HttpResponse response = httpClient.execute(httpPost);

			if(livello > liv)
				liv = livello;

		}

        // EVOSUITE - T8
		// TODO: RICHIEDE AGGIUSTAMENTI IN T8
		Path directoryE = Paths.get("/VolumeT8/FolderTreeEvo/" + cname + "/" + cname + "SourceCode");

		try {
			// Verifica se la directory esiste già
			if (!Files.exists(directoryE)) {
				// Crea la directory
				Files.createDirectories(directoryE);
				System.out.println("La directory è stata creata con successo.");
			} else {
				System.out.println("La directory esiste già.");
			}
		} catch (Exception e) {
			System.out.println("Errore durante la creazione della directory: " + e.getMessage());
		}

		try (InputStream inputStream = multipartFile.getInputStream()) {
			Path filePath = directoryE.resolve(fileName);
			System.out.println(filePath.toString());
			Files.copy(inputStream,filePath,StandardCopyOption.REPLACE_EXISTING);
		}

		ProcessBuilder processBuilderE = new ProcessBuilder();

        processBuilderE.command("bash", "robot_generazione.sh", cname, "\"\"", "/VolumeT9/app/FolderTree/" + cname + "/" + cname + "SourceCode", String.valueOf(liv));
        processBuilderE.directory(new File("/VolumeT8/Prototipo2.0/"));

		Process processE = processBuilderE.start();

		BufferedReader readerE = new BufferedReader(new InputStreamReader(processE.getInputStream()));
        String lineE;
        while ((lineE = readerE.readLine()) != null)
            System.out.println(lineE);
			
        readerE = new BufferedReader(new InputStreamReader(processE.getErrorStream()));
        while ((lineE = readerE.readLine()) != null)
            System.out.println(lineE);

        try {
			int exitCode = processE.waitFor();

			System.out.println("ERRORE CODE: " + exitCode);
		} catch (InterruptedException e) {
			System.out.println(e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		File resultsDirE = new File("/VolumeT8/FolderTreeEvo/" + cname + "/RobotTest/EvoSuiteTest");

        File resultsE [] = resultsDirE.listFiles();
        for(File result : resultsE) {
			int score = LineCoverageE(result.getAbsolutePath() + "/TestReport/statistics.csv");

			System.out.println(result.toString().substring(result.toString().length() - 7, result.toString().length() - 5));
			int livello = Integer.parseInt(result.toString().substring(result.toString().length() - 7, result.toString().length() - 5));

			System.out.println("La copertura del livello " + String.valueOf(livello) + " è: " + String.valueOf(score));

			HttpClient httpClient = HttpClientBuilder.create().build();
			HttpPost httpPost = new HttpPost("http://t4-g18-app-1:3000/robots");

			JSONArray arr = new JSONArray();

			JSONObject rob = new JSONObject();
			rob.put("scores", String.valueOf(score));
			rob.put("type", "evosuite");
			rob.put("difficulty", String.valueOf(livello));
			rob.put("testClassId", cname);

			arr.put(rob);

			JSONObject obj = new JSONObject();
			obj.put("robots", arr);

			StringEntity jsonEntity = new StringEntity(obj.toString(), ContentType.APPLICATION_JSON);

			httpPost.setEntity(jsonEntity);

			HttpResponse response = httpClient.execute(httpPost);

		}

    }
    

}
