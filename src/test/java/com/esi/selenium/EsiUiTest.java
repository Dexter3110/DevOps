package com.esi.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EsiUiTest extends BaseUiTest {

    @Test
    @DisplayName("Journey 1: Dashboard loads and ENV badge is visible")
    void testDashboardLoadsAndEnvBadgeVisible() {
        driver.get(baseUrl);

        // 1. Verify dashboard view title
        WebElement viewTitle = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("viewTitle")));
        assertEquals("Dashboard", viewTitle.getText(), "View title should be Dashboard");

        // 2. Verify ENV badge is visible and populated
        WebElement envBadge = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("envBadge")));
        assertTrue(envBadge.isDisplayed(), "ENV badge should be displayed");

        // Wait until text is loaded (not empty and contains ENV:)
        wait.until(ExpectedConditions.textMatches(By.id("envBadge"), java.util.regex.Pattern.compile("^ENV:.*")));
        String badgeText = envBadge.getText();
        assertTrue(badgeText.startsWith("ENV:"), "Badge should start with 'ENV:' but was: " + badgeText);

        // 3. Verify statistics cards are displayed
        assertTrue(driver.findElement(By.id("statEmployees")).isDisplayed(), "Employees stat card should be visible");
        assertTrue(driver.findElement(By.id("statSkills")).isDisplayed(), "Skills stat card should be visible");
        assertTrue(driver.findElement(By.id("statAssignments")).isDisplayed(), "Assignments stat card should be visible");
        assertTrue(driver.findElement(By.id("statExpiring")).isDisplayed(), "Expiring alerts card should be visible");
    }

    @Test
    @DisplayName("Journey 2: Add a new employee and see it in the list")
    void testAddNewEmployeeAndVerifyInList() {
        driver.get(baseUrl);

        // 1. Navigate to Employees view
        navigateToView("employees", "Employees");

        // 2. Open Add Employee modal
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addEmployeeBtn"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modalOverlay")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("employeeForm")));

        // 3. Populate unique employee data
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String firstName = "John_" + uid;
        String lastName = "Tester_" + uid;
        String email = "john.tester." + uid + "@company.org";
        String department = "Engineering";
        String designation = "Automation Engineer " + uid;
        String hireDate = "2024-01-10";

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#employeeForm [name='firstName']"))).sendKeys(firstName);
        driver.findElement(By.cssSelector("#employeeForm [name='lastName']")).sendKeys(lastName);
        driver.findElement(By.cssSelector("#employeeForm [name='email']")).sendKeys(email);
        driver.findElement(By.cssSelector("#employeeForm [name='department']")).sendKeys(department);
        driver.findElement(By.cssSelector("#employeeForm [name='designation']")).sendKeys(designation);
        driver.findElement(By.cssSelector("#employeeForm [name='hireDate']")).sendKeys(hireDate);

        // 4. Submit form and wait for modal to close
        driver.findElement(By.cssSelector("#employeeForm button[type='submit']")).click();
        waitForModalToClose();

        // 5. Verify the new employee appears in the table
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("employeesTable"), email));

        String tableText = driver.findElement(By.id("employeesTable")).getText();
        assertTrue(tableText.contains(firstName + " " + lastName), "Table should contain employee full name");
        assertTrue(tableText.contains(email), "Table should contain employee email");
        assertTrue(tableText.contains(department), "Table should contain employee department");
        assertTrue(tableText.contains(designation), "Table should contain employee designation");
    }

    @Test
    @DisplayName("Journey 3: Add a skill to the catalogue then search for it")
    void testAddSkillToCatalogueAndSearch() {
        driver.get(baseUrl);

        // 1. Navigate to Skill Catalogue view
        navigateToView("skills", "Skill Catalogue");

        // 2. Open Add Skill modal
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addSkillBtn"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modalOverlay")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("skillForm")));

        // 3. Populate unique skill data
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String skillName = "Terraform_" + uid;
        String category = "Infrastructure";
        String description = "Infrastructure as Code automation " + uid;

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#skillForm [name='name']"))).sendKeys(skillName);
        driver.findElement(By.cssSelector("#skillForm [name='category']")).sendKeys(category);
        new Select(driver.findElement(By.cssSelector("#skillForm [name='status']"))).selectByValue("ACTIVE");
        driver.findElement(By.cssSelector("#skillForm [name='description']")).sendKeys(description);

        // 4. Submit form and wait for modal to close
        driver.findElement(By.cssSelector("#skillForm button[type='submit']")).click();
        waitForModalToClose();

        // 5. Verify the skill appears in the catalogue table
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("skillsTable"), skillName));

        // 6. Navigate to Search view and search for the new skill by keyword
        navigateToView("search", "Search");
        WebElement keywordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("#searchSkillsForm input[name='keyword']")));
        keywordInput.clear();
        keywordInput.sendKeys(skillName);
        driver.findElement(By.cssSelector("#searchSkillsForm button[type='submit']")).click();

        // 7. Verify search results contain the newly added skill
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("searchSkillsTable"), skillName));

        String searchTableText = driver.findElement(By.id("searchSkillsTable")).getText();
        assertTrue(searchTableText.contains(skillName), "Search results should contain skill name: " + skillName);
        assertTrue(searchTableText.contains(category), "Search results should contain skill category: " + category);
    }

    @Test
    @DisplayName("Journey 4: Assign a skill to an employee and verify the assignment appears")
    void testAssignSkillToEmployeeAndVerifyAssignment() {
        driver.get(baseUrl);

        String uid = UUID.randomUUID().toString().substring(0, 8);

        // Step A: Create dedicated unique employee for assignment
        navigateToView("employees", "Employees");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addEmployeeBtn"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modalOverlay")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("employeeForm")));

        String empFirst = "Alex_" + uid;
        String empLast = "Assignee_" + uid;
        String empFullName = empFirst + " " + empLast;
        String empEmail = "alex.assignee." + uid + "@testcorp.org";
        String empDept = "Operations";

        driver.findElement(By.cssSelector("#employeeForm [name='firstName']")).sendKeys(empFirst);
        driver.findElement(By.cssSelector("#employeeForm [name='lastName']")).sendKeys(empLast);
        driver.findElement(By.cssSelector("#employeeForm [name='email']")).sendKeys(empEmail);
        driver.findElement(By.cssSelector("#employeeForm [name='department']")).sendKeys(empDept);
        driver.findElement(By.cssSelector("#employeeForm [name='designation']")).sendKeys("Systems Engineer");
        driver.findElement(By.cssSelector("#employeeForm button[type='submit']")).click();
        waitForModalToClose();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("employeesTable"), empEmail));

        // Step B: Create dedicated unique skill for assignment
        navigateToView("skills", "Skill Catalogue");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addSkillBtn"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modalOverlay")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("skillForm")));

        String skillName = "Ansible_" + uid;
        String skillCategory = "Configuration Mgmt";

        driver.findElement(By.cssSelector("#skillForm [name='name']")).sendKeys(skillName);
        driver.findElement(By.cssSelector("#skillForm [name='category']")).sendKeys(skillCategory);
        driver.findElement(By.cssSelector("#skillForm button[type='submit']")).click();
        waitForModalToClose();
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("skillsTable"), skillName));

        // Step C: Assign skill to employee
        navigateToView("assignments", "Skill Assignments");
        wait.until(ExpectedConditions.elementToBeClickable(By.id("addAssignmentBtn"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("modalOverlay")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("assignmentForm")));

        // Select employee from dropdown
        Select empSelect = new Select(wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#assignmentForm [name='employeeId']"))));
        empSelect.getOptions().stream()
                .filter(opt -> opt.getText().contains(empFullName))
                .findFirst()
                .ifPresentOrElse(
                        opt -> empSelect.selectByVisibleText(opt.getText()),
                        () -> fail("Employee option not found in dropdown: " + empFullName)
                );

        // Select skill from dropdown
        Select skillSelect = new Select(driver.findElement(By.cssSelector("#assignmentForm [name='skillId']")));
        skillSelect.getOptions().stream()
                .filter(opt -> opt.getText().contains(skillName))
                .findFirst()
                .ifPresentOrElse(
                        opt -> skillSelect.selectByVisibleText(opt.getText()),
                        () -> fail("Skill option not found in dropdown: " + skillName)
                );

        // Fill proficiency and details
        new Select(driver.findElement(By.cssSelector("#assignmentForm [name='proficiencyLevel']"))).selectByValue("ADVANCED");
        driver.findElement(By.cssSelector("#assignmentForm [name='yearsOfExperience']")).clear();
        driver.findElement(By.cssSelector("#assignmentForm [name='yearsOfExperience']")).sendKeys("4");

        String certName = "Certified Specialist " + uid;
        driver.findElement(By.cssSelector("#assignmentForm [name='certificationName']")).sendKeys(certName);
        driver.findElement(By.id("verifiedCheck")).click();

        // Submit form
        driver.findElement(By.cssSelector("#assignmentForm button[type='submit']")).click();
        waitForModalToClose();

        // Step D: Verify the assignment appears in the table
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("assignmentsTable"), empFullName));

        String assignmentsText = driver.findElement(By.id("assignmentsTable")).getText();
        assertTrue(assignmentsText.contains(empFullName), "Assignments table should contain employee name");
        assertTrue(assignmentsText.contains(skillName), "Assignments table should contain skill name");
        assertTrue(assignmentsText.contains("ADVANCED"), "Assignments table should contain proficiency ADVANCED");
        assertTrue(assignmentsText.contains(certName), "Assignments table should contain certification name");
        assertTrue(assignmentsText.contains("Verified"), "Assignments table should show Verified status");
    }
}