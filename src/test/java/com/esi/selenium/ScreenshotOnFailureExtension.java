package com.esi.selenium;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Saves target/screenshots/<testMethodName>.png when a test throws.
 *
 * Why TestExecutionExceptionHandler and not TestWatcher:
 * TestWatcher.testFailed() runs AFTER @AfterEach, by which time BaseUiTest.tearDown()
 * has already called driver.quit(), so no screenshot can be taken.
 * handleTestExecutionException() runs while the browser is still open.
 */
public class ScreenshotOnFailureExtension implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        try {
            Object testInstance = context.getRequiredTestInstance();
            if (testInstance instanceof BaseUiTest baseUiTest) {
                WebDriver driver = baseUiTest.getDriver();
                if (driver instanceof TakesScreenshot takesScreenshot) {
                    File screenshotFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
                    Path screenshotsDir = Paths.get("target", "screenshots");
                    Files.createDirectories(screenshotsDir);

                    String testMethodName = context.getRequiredTestMethod().getName();
                    Path targetPath = screenshotsDir.resolve(testMethodName + ".png");
                    Files.copy(screenshotFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[ScreenshotOnFailureExtension] Saved failure screenshot to: "
                            + targetPath.toAbsolutePath());
                }
            }
        } catch (Exception e) {
            // Never let screenshot problems hide the real test failure
            System.err.println("[ScreenshotOnFailureExtension] Could not save screenshot: " + e.getMessage());
        }
        // Rethrow so the test is still reported as failed
        throw throwable;
    }
}