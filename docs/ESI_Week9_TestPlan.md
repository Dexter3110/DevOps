# ESI Week 9 Automated UI Test Plan

## 1. Overview and Objective
This document outlines the Automated UI Acceptance Testing strategy for the **Employee Skill Inventory (ESI)** web application. The automated tests validate 4 critical end-to-end user journeys using **Selenium WebDriver (4.x)**, **JUnit 5 Jupiter**, and **WebDriverManager**.

Tests run against a headless Chrome browser instance to ensure 100% compatibility with CI/CD environments (such as Jenkins running as a background Windows service without an active interactive desktop).

---

## 2. Test Architecture and Strategy

### 2.1 Technology Stack
- **Test Framework:** JUnit 5 (Jupiter 5.10.2)
- **Browser Automation:** Selenium WebDriver (`selenium-java` 4.19.1)
- **Driver Management:** `io.github.bonigarcia:webdrivermanager:5.8.0` + Chrome Headless (`--headless=new`)
- **Build & Execution:** Apache Maven Surefire Plugin (v3.1.2)
- **Package Separation:** `com.esi.selenium` (isolated from unit tests)
- **Failure Artifacts:** Automatic screenshot capture via JUnit 5 `TestWatcher` saved to `target/screenshots/<testName>.png`

### 2.2 Maven Surefire Test Execution Strategy
The test suite is partitioned so that regular CI unit test stages (`mvn test`) do not fail when the web application is not yet running:
- **Default `mvn test`**: Excludes `**/*UiTest.java` and executes unit/integration tests (`EsiApplicationTests`).
- **UI Test Execution**: Triggered explicitly via:
  ```bash
  mvn test -Dtest=*UiTest -Dbase.url=http://localhost:8085
  ```

---

## 3. Test Cases: Critical User Journeys

| ID | Journey | Steps | Test Data | Expected Result |
|---|---|---|---|---|
| **TC-UI-01** | **Dashboard Loads & ENV Badge Visible** | 1. Navigate to `${base.url}`<br>2. Wait for view title to load<br>3. Inspect `#envBadge` visibility & text<br>4. Verify statistics cards | `baseUrl = http://localhost:8085` | - View title is `"Dashboard"`<br>- `#envBadge` is visible and text starts with `"ENV:"`<br>- Cards (`statEmployees`, `statSkills`, `statAssignments`, `statExpiring`) are displayed |
| **TC-UI-02** | **Add Employee & Verify in List** | 1. Navigate to `${base.url}`<br>2. Click sidebar `"Employees"` link<br>3. Click `"+ Add Employee"` button<br>4. Fill `#employeeForm` with unique data<br>5. Click submit<br>6. Wait for modal to close<br>7. Inspect `#employeesTable` | Unique ID generated via UUID:<br>- **First Name:** `John_<uid>`<br>- **Last Name:** `Tester_<uid>`<br>- **Email:** `john.tester.<uid>@company.org`<br>- **Department:** `Engineering`<br>- **Designation:** `Automation Engineer <uid>`<br>- **Hire Date:** `2024-01-10` | - Modal closes successfully<br>- `#employeesTable` contains the newly added employee's full name, email, department, and designation |
| **TC-UI-03** | **Add Skill to Catalogue & Search** | 1. Navigate to `${base.url}`<br>2. Click sidebar `"Skill Catalogue"` link<br>3. Click `"+ Add Skill"` button<br>4. Fill `#skillForm` with unique skill data<br>5. Submit form & wait for modal close<br>6. Verify skill in `#skillsTable`<br>7. Click sidebar `"Search"` link<br>8. Enter skill name into `#searchSkillsForm input[name='keyword']`<br>9. Click `"Search"` button<br>10. Inspect `#searchSkillsTable` | Unique ID generated via UUID:<br>- **Name:** `Terraform_<uid>`<br>- **Category:** `Infrastructure`<br>- **Status:** `ACTIVE`<br>- **Description:** `Infrastructure as Code automation <uid>` | - Skill appears in catalogue table<br>- Navigating to search and searching by keyword returns the skill in `#searchSkillsTable` with matching name and category |
| **TC-UI-04** | **Assign Skill to Employee & Verify** | 1. Navigate to `${base.url}`<br>2. Create dedicated unique Employee (`Alex_<uid>`)<br>3. Create dedicated unique Skill (`Ansible_<uid>`)<br>4. Click sidebar `"Skill Assignments"` link<br>5. Click `"+ Assign Skill to Employee"`<br>6. Select employee & skill from dropdowns<br>7. Select proficiency `"ADVANCED"`, experience `4`, cert name, and check `"Verified"`<br>8. Submit `#assignmentForm`<br>9. Wait for modal close<br>10. Inspect `#assignmentsTable` | Unique ID generated via UUID:<br>- **Employee:** `Alex_<uid> Assignee_<uid>`<br>- **Skill:** `Ansible_<uid>` (`Configuration Mgmt`)<br>- **Proficiency:** `ADVANCED`<br>- **Years Exp:** `4`<br>- **Certification:** `Certified Specialist <uid>`<br>- **Verified:** `true` | - Assignment modal closes cleanly<br>- `#assignmentsTable` displays the row matching the employee name, skill name, proficiency level (`ADVANCED`), certification name, and `"Verified"` pill |

---

## 4. Failure Diagnostics & Screenshots
- Implemented in `com.esi.selenium.ScreenshotOnFailureExtension` implementing `org.junit.jupiter.api.extension.TestWatcher`.
- If any test method fails or throws an unhandled assertion/wait exception, a full-page PNG screenshot is automatically captured from the active `ChromeDriver` and saved to:
  ```
  target/screenshots/<testMethodName>.png
  ```
- These artifacts can be archived by Jenkins via `archiveArtifacts artifacts: 'target/screenshots/*.png', allowEmptyArchive: true`.

---

## 5. Local Execution Guide

### Step 1: Start the ESI Application
Open a terminal in the project directory:
```bash
mvn spring-boot:run
```
*(Or run the packaged JAR: `java -jar target/esi-project-0.0.1-SNAPSHOT.jar --server.port=8085`)*

Verify the application is active by browsing to [http://localhost:8085](http://localhost:8085).

### Step 2: Run the Selenium UI Test Suite
Open a second terminal window in the project directory and run:
```bash
mvn test -Dtest=*UiTest -Dbase.url=http://localhost:8085
```

### Step 3: Run Against QA or Different Port
To run against another port (e.g. QA on port 8086):
```bash
mvn test -Dtest=*UiTest -Dbase.url=http://localhost:8086
```

### Step 4: Verify Failure Screenshots (Optional Verification)
Screenshots on test failure are saved to:
```
Project/target/screenshots/
```
