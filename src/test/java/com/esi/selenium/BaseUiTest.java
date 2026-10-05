package com.esi.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

@ExtendWith(ScreenshotOnFailureExtension.class)
public abstract class BaseUiTest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String baseUrl;

    @BeforeAll
    public static void setupWebDriverManager() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setUp() {
        this.baseUrl = System.getProperty("base.url", "http://localhost:8085");

        ChromeOptions options = new ChromeOptions();
        // Headless mode is required for CI / Windows service execution
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");

        this.driver = new ChromeDriver(options);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }
        }
    }

    public WebDriver getDriver() {
        return driver;
    }

    /**
     * Switch view by clicking the sidebar navigation button.
     */
    protected void navigateToView(String viewName, String expectedTitle) {
        WebElement navBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("button.nav-link[data-view='" + viewName + "']")));
        navBtn.click();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("viewTitle"), expectedTitle));
    }

    /**
     * Wait for modal overlay to be completely hidden.
     */
    protected void waitForModalToClose() {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("modalOverlay")));
    }
}
