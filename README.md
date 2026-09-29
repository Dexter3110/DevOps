# Employee Skill Inventory (ESI) System

[![Java CI with Maven](https://github.com/Dexter3110/DevOps/actions/workflows/ci.yml/badge.svg)](https://github.com/Dexter3110/DevOps/actions/workflows/ci.yml)

The **Employee Skill Inventory (ESI)** system is a Spring Boot application designed to track, manage, and search employee skills, proficiencies, certifications, and skill gaps across departments.

---

## 📁 Project Structure

```text
Project/
├── .github/
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md                        # Issue template for bugs (Week 4)
│   │   └── feature_request.md                   # Issue template for feature requests (Week 4)
│   └── workflows/
│       └── ci.yml                               # GitHub Actions CI Workflow (Week 4)
├── db/
│   └── schema.sql                               # Reference SQL DDL & seed data (Week 4)
├── docs/
│   ├── ESI_Week_1_Documentation.docx            # Documentation for Week 1
│   ├── ESI_Week_2_Documentation.docx            # Documentation for Week 2
│   ├── ESI_Week_3_Documentation.docx            # Documentation for Week 3
│   └── ci-test.txt                              # CI pipeline trigger test file (Week 7)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── esi/
│   │   │           ├── EsiApplication.java                  # Main Application (Week 5)
│   │   │           ├── model/
│   │   │           │   ├── Skill.java                       # Skill Entity (Week 5)
│   │   │           │   ├── Employee.java                    # Employee Entity (Week 6)
│   │   │           │   └── EmployeeSkill.java               # Association Entity (Week 6)
│   │   │           ├── repo/
│   │   │           │   ├── SkillRepository.java             # Skill JPA Repository (Week 5)
│   │   │           │   ├── EmployeeRepository.java          # Employee JPA Repository (Week 6)
│   │   │           │   └── EmployeeSkillRepository.java     # Skill Association Repository (Week 6)
│   │   │           └── web/
│   │   │               ├── SkillController.java             # Skill REST Controller (Week 5)
│   │   │               ├── EmployeeController.java          # Employee REST Controller (Week 6, US-02)
│   │   │               ├── EmployeeSkillController.java     # Skill Mapping Controller (Week 6, US-02)
│   │   │               ├── SearchController.java            # Multi-parameter Search (Week 6, US-04)
│   │   │               └── AlertController.java             # Certification Alerts (Week 6, US-05)
│   │   └── resources/
│   │       ├── application.properties                       # App & DB Config (Weeks 5 & 6)
│   │       ├── application-dev.properties                   # Dev environment overrides (Week 8)
│   │       ├── application-qa.properties                    # QA environment overrides (Week 8)
│   │       └── static/                                       # Web dashboard (Week 8)
│   │           ├── index.html
│   │           ├── css/style.css
│   │           └── js/app.js
│   └── test/
│       └── java/
│           └── com/
│               └── esi/
│                   └── EsiApplicationTests.java             # Unit & Integration Tests (Week 5)
├── .gitignore                                   # Git ignore rules (Week 4)
├── Jenkinsfile                                  # Jenkins Declarative CI Pipeline (Week 7)
├── pom.xml                                      # Maven Build & Dependencies (Week 5)
└── README.md                                    # Project documentation (Week 4)
```

---

## 🛠️ Technology Stack

- **Language:** Java 17+
- **Framework:** Spring Boot 3.2.x (Spring Web, Spring Data JPA, Spring Validation)
- **Database:** In-memory H2 Database (`jdbc:h2:mem:esidb`)
- **Build Tool:** Apache Maven
- **Continuous Integration (CI):** GitHub Actions & Jenkins Declarative Pipeline

---

## 🚀 Getting Started

### Prerequisites

- **JDK 17** or higher installed
- **Apache Maven 3.8+** installed
- **Git** installed

### Running the Application Locally

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Dexter3110/DevOps.git
   cd DevOps/Project
   ```

2. **Build and test the application:**
   ```bash
   mvn clean test
   ```

3. **Run the Spring Boot application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application:**
   - **Web Dashboard:** `http://localhost:8085/` — a browser UI for everything below (employees, skill catalogue, skill assignments, search, exception alerts). It talks to the REST API on the same origin, so no extra setup is needed.
   - Base API URL: `http://localhost:8085/api`
   - H2 Database Web Console: `http://localhost:8085/h2-console`
     - **JDBC URL:** `jdbc:h2:mem:esidb`
     - **User Name:** `sa`
     - **Password:** *(leave blank)*

---

## 🖥️ Web Dashboard

`src/main/resources/static/` holds a small vanilla HTML/CSS/JS single-page dashboard. Spring Boot serves it automatically at `/` — there's no separate frontend build step or server.

| Screen | What it does |
|---|---|
| Dashboard | Employee/skill/assignment counts, category & proficiency breakdown, upcoming certification expiries |
| Employees | List, add, edit, delete employees |
| Skill Catalogue | List, add, edit, delete skills |
| Assignments | Assign a skill (with proficiency, years of experience, certification, expiry) to an employee; remove an assignment |
| Search | Search employees by department/skill/proficiency; search skills by keyword/category |
| Exception Alerts | Certifications expiring within a chosen number of days |

The badge in the top-right corner calls `GET /api/env` and shows which environment/port the running instance was started with — useful evidence that Jenkins really deployed with a different Spring profile (see Deployment below).

---

## 📡 REST API Documentation

### 1. Skills Management (`/api/skills`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/skills` | Retrieve all skills |
| `GET` | `/api/skills/{id}` | Retrieve skill by ID |
| `POST` | `/api/skills` | Create a new skill |
| `PUT` | `/api/skills/{id}` | Update an existing skill |
| `DELETE` | `/api/skills/{id}` | Delete a skill |

### 2. Employees Management (`/api/employees`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/employees` | Retrieve all employees |
| `GET` | `/api/employees/{id}` | Retrieve employee by ID |
| `POST` | `/api/employees` | Create a new employee |
| `PUT` | `/api/employees/{id}` | Update employee details |
| `DELETE` | `/api/employees/{id}` | Delete employee |

### 3. Employee Skills Association (`/api/employee-skills`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/employee-skills` | List all employee-skill mappings |
| `GET` | `/api/employee-skills/employee/{employeeId}` | Get all skills for a specific employee |
| `GET` | `/api/employee-skills/skill/{skillId}` | Get all employees with a specific skill |
| `POST` | `/api/employee-skills/assign?employeeId={id}&skillId={id}` | Assign or update skill proficiency for an employee |
| `DELETE` | `/api/employee-skills/{id}` | Remove a skill mapping |

### 4. Search (`/api/search`)
| Method | Endpoint | Description | Query Parameters |
|---|---|---|---|
| `GET` | `/api/search/skills` | Filter skills | `keyword`, `category` |
| `GET` | `/api/search/employees` | Search employees | `department`, `skillName`, `proficiency` |

### 5. Alerts & Notifications (`/api/alerts`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/alerts/expiring-certifications?days=30` | List certifications expiring in `N` days |
| `GET` | `/api/alerts/summary` | Get an alert summary for dashboards |

### 6. Environment (`/api/env`)
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/env` | Reports the active `esi.environment` and `server.port` for this running instance (Week 8 parameterization evidence) |

---

## 🔄 CI/CD Pipelines

### 1. GitHub Actions
- Workflow file: `.github/workflows/ci.yml`
- Triggers on push and pull requests to `main`, `master`, and `develop` branches.
- Automates compiling, running test suites, packaging JARs, and uploading build artifacts.

### 2. Jenkins Pipeline
- Pipeline definition: `Jenkinsfile`
- Multi-stage pipeline covering: `Checkout` ➔ `Build` ➔ `Test` ➔ `Package` ➔ `Archive Artifact` ➔ `Deploy` ➔ `Health Check`.
- **Parameterized build:** the job asks for `ENVIRONMENT` (`dev` or `qa`) on every run. That choice selects the Spring profile (`application-dev.properties` / `application-qa.properties`), the port (8085 / 8086) and the deploy folder (`C:\esi-deploy\dev` or `C:\esi-deploy\qa`).
- **Deploy stage:** stops whatever is currently listening on the target port, copies the freshly built JAR into the environment's deploy folder, and starts it with `--spring.profiles.active=%ENVIRONMENT%`.
- **Health Check stage:** calls `GET /api/skills` on the deployed instance and fails the build if it doesn't respond, so a broken deploy is caught immediately rather than silently left running the old version.

---

## 📜 License
This project is developed as part of the DevOps Course Curriculum.