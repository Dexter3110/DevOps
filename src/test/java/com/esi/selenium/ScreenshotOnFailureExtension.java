package com.esi.selenium;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class ScreenshotOnFailureExtension implements TestWatcher {

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        Object testInstance = context.getRequiredTestInstance();
        if (testInstance instanceof BaseUiTest baseUiTest) {
            WebDriver driver = baseUiTest.getDriver();
            if (driver instanceof TakesScreenshot takesScreenshot) {
                try {
                    File screenshotFile = takesScreenshot.getScreenshotAs(OutputType.FILE);
                    Path screenshotsDir = Paths.get("target", "screenshots");
                    Files.createDirectories(screenshotsDir);

                    String testMethodName = context.getRequiredTestMethod().getName();
                    Path targetPath = screenshotsDir.resolve(testMethodName + ".png");
                    Files.copy(screenshotFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("[ScreenshotOnFailureExtension] Saved failure screenshot to: " + targetPath.toAbsolutePath());
                } catch (IOException e) {
                    System.err.println("[ScreenshotOnFailureExtension] Failed to save screenshot: " + e.getMessage());
                }
            }
        }
    }
}
