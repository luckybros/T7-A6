package com.g2.t5;

import java.time.Duration;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class T5ApplicationTests {

	public static WebDriver driver;
	public static Duration timeout = Duration.ofMillis(20000);

    // Una volta che avete modificato path, email e password rimuovete il commento a @Test per poterlo eseguire
    //@Test
    public void startGameTest() {
		System.setProperty("webdriver.chrome.driver", "src/test/java/drivers/chromedriver.exe");

		ChromeOptions options = new ChromeOptions();
		options.setBinary("src/test/java/chrome-win64/chrome.exe"); // Modifica path

        HashMap<String, Object> chromePrefs = new HashMap<String, Object>();
        chromePrefs.put("profile.default_content_settings.popups", 0);
        options.setExperimentalOption("prefs", chromePrefs);

		driver = new ChromeDriver(options);
	
		driver.manage().timeouts().implicitlyWait(timeout);
		driver.manage().window().maximize();

        driver.get("http://localhost/login");	
        driver.findElement(By.id("email")).sendKeys("your@email.com"); // Modifica email
        driver.findElement(By.id("password")).sendKeys("yourPassword"); // Modifica password
        driver.findElement(By.cssSelector("input[type=submit]")).click();

		WebDriverWait wait = new WebDriverWait(driver, timeout);

        String urlMain = "http://localhost/main";

		try {
            wait.until(ExpectedConditions.urlToBe(urlMain));
        } catch(TimeoutException e) {
            Assert.fail();
        }

        String urlReport = "http://localhost/report";

		driver.findElement(By.id("0")).click();
        driver.findElement(By.id("0-1")).click();

		WebElement submitButton = driver.findElements(By.cssSelector(".custom-button")).get(1);

        new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.elementToBeClickable(submitButton));
    
        submitButton.click();

        new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.urlToBe(urlReport));

		String urlEditor = "http://localhost/editor";

		submitButton = driver.findElements(By.cssSelector(".custom-button")).get(1);

		new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.elementToBeClickable(submitButton));
    
        submitButton.click();

        new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.urlToBe(urlEditor));

        Assert.assertEquals("Test fallito: i dati non sono stati inviati.", driver.getCurrentUrl(), urlEditor);
    }
}