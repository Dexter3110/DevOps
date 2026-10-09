# ESI Week 12: Continuous Deployment with Jenkins and Docker

## 1. Overview and Architecture
This document details the automated Docker build, registry publishing, and containerized deployment workflow implemented for the **Employee Skill Inventory (ESI)** Spring Boot application in Jenkins.

### Key Highlights
- **Quality Gate Integration:** Full test suite execution—including unit/integration tests and automated **Selenium WebDriver UI tests** against a temporary instance on port `8099`—runs **BEFORE** any Docker stage executes. A test failure immediately aborts the pipeline, preventing defective containers from ever being built or deployed.
- **Dual Deployment Modes:** The pipeline is parameterized with `DEPLOY_MODE` (`docker` and `jar`), allowing seamless selection between containerized microservice deployments and traditional JAR process deployments.
- **Local Private Registry:** Built images are pushed to a local Docker registry running at `localhost:5000` to mirror enterprise artifact repository workflows (such as Nexus, Harbor, or Docker Hub).
- **Automated Container Lifecycle:** Previous environment containers are stopped and cleanly replaced with new containers mapped to dedicated host ports (`8087` for `dev`, `8088` for `qa`).

---

## 2. Pipeline Parameterization & Port Mapping

### 2.1 Pipeline Parameters
The Jenkins pipeline (`esi-build`) provides two execution parameters:
- `ENVIRONMENT`: Choice between `dev` and `qa`.
- `DEPLOY_MODE`: Choice between `docker` (default) and `jar`.

### 2.2 Port Allocation Reference Table

| Environment / Service | Mode | Host Port | Target / Container Port | Purpose |
|---|---|---|---|---|
| **Selenium Quality Gate** | Transient JAR | `8099` | `8099` | Temporary instance during Selenium UI testing |
| **Dev Environment** | `docker` | `8087` | `8085` | ESI application container (`esi-dev`) |
| **QA Environment** | `docker` | `8088` | `8085` | ESI application container (`esi-qa`) |
| **Dev Environment** | `jar` | `8085` | `8085` | Direct JAR deployment on host (`C:\esi-deploy\dev`) |
| **QA Environment** | `jar` | `8086` | `8086` | Direct JAR deployment on host (`C:\esi-deploy\qa`) |
| **Docker Registry** | Container | `5000` | `5000` | Local private distribution registry (`registry:2`) |

---

## 3. Pipeline Stages Breakdown

The Jenkins pipeline (`Jenkinsfile`) executes the following sequential stages:

```
[ Checkout ]
     ↓
[ Build ] (mvn clean compile)
     ↓
[ Test ] (mvn test + JUnit XML reports)
     ↓
[ Package ] (mvn package -DskipTests)
     ↓
[ Archive Artifact ] (target/*.jar)
     ↓
[ Selenium UI Tests ] (port 8099 quality gate, headless Chrome)
     ↓
+-----------------------------------+-----------------------------------+
|     when (DEPLOY_MODE == 'docker') |     when (DEPLOY_MODE == 'jar')    |
+-----------------------------------+-----------------------------------+
| [ Docker Build ]                  | [ Deploy ]                        |
| [ Push to Registry ]              | [ Health Check ]                  |
| [ Deploy Container ]              |                                   |
+-----------------------------------+-----------------------------------+
```

### Stage 1: Checkout
Retrieves the latest commit from `origin/develop` using Jenkins SCM checkout.

### Stage 2: Build
Compiles the application source code:
```bat
mvn clean compile
```

### Stage 3: Test
Executes all unit and repository integration tests, capturing surefire reports:
```bat
mvn test
```
The JUnit plugin archives test reports (`target/surefire-reports/*.xml`).

### Stage 4: Package
Generates the executable Spring Boot JAR file without re-running unit tests:
```bat
mvn package -DskipTests
```

### Stage 5: Archive Artifact
Archives `target/*.jar` in Jenkins for artifact traceability and retention.

### Stage 6: Selenium UI Tests (Quality Gate)
- Spawns the packaged JAR detached on temporary port `8099`.
- Polls `http://localhost:8099/api/env` until the application is ready.
- Runs the Selenium WebDriver end-to-end tests (`mvn test -Dtest=*UiTest -Dbase.url=http://localhost:8099`).
- Cleans up the background application process upon completion.
- Publishes test reports and failure screenshots (`target/screenshots/*.png`).
- **Critical Quality Gate:** If any UI journey fails, the pipeline aborts immediately. Docker images are **never built** from failing code.

### Stage 7: Docker Build *(when DEPLOY_MODE == 'docker')*
Executes the multi-stage `Dockerfile`, building the application image and assigning dual registry tags:
```bat
docker build -t esi-app:%BUILD_NUMBER% -t localhost:5000/esi-app:%BUILD_NUMBER% -t localhost:5000/esi-app:latest . || exit /b 1
```

### Stage 8: Push to Registry *(when DEPLOY_MODE == 'docker')*
Pushes both the immutable build number tag and the mutable latest tag to the private registry, then queries the tag catalog:
```bat
docker push localhost:5000/esi-app:%BUILD_NUMBER% || exit /b 1
docker push localhost:5000/esi-app:latest || exit /b 1
curl -f http://localhost:5000/v2/esi-app/tags/list || exit /b 1
```

### Stage 9: Deploy Container *(when DEPLOY_MODE == 'docker')*
Gracefully cleans up previous container instances, starts the new container with environment-specific profiles and port mappings, waits for initialization, and verifies health:
```bat
REM Stop and remove existing container if running (ignoring errors if absent)
docker stop %CONTAINER_NAME% >nul 2>&1 || echo Container %CONTAINER_NAME% was not running
docker rm %CONTAINER_NAME% >nul 2>&1 || echo Container %CONTAINER_NAME% did not exist

REM Run new container mapped to host port
docker run -d --name %CONTAINER_NAME% -p %DOCKER_PORT%:8085 -e SPRING_PROFILES_ACTIVE=%ENVIRONMENT% localhost:5000/esi-app:%BUILD_NUMBER% || exit /b 1

REM Wait about 30 seconds for Spring Boot container startup
ping -n 31 127.0.0.1 > nul

REM Health check: verify /api/env returns HTTP 200
curl -f http://localhost:%DOCKER_PORT%/api/env || exit /b 1
```

### Stages 10 & 11: Deploy & Health Check *(when DEPLOY_MODE == 'jar')*
Guarded by `when { expression { params.DEPLOY_MODE == 'jar' } }` to ensure full backward compatibility with the Week 8 native Windows service JAR deployment.

---

## 4. Docker Image Tagging Scheme

Every pipeline run produces two distinct tags in the registry:
1. **Immutable Build Tag (`localhost:5000/esi-app:%BUILD_NUMBER%`):**
   - Strictly tied to the Jenkins execution (e.g., `localhost:5000/esi-app:18`).
   - Ensures end-to-end traceability from a running container back to the exact Jenkins build, Git commit, and test reports.
   - Prevents cache collisions and accidental overwrite.
2. **Rolling Latest Tag (`localhost:5000/esi-app:latest`):**
   - Always points to the most recently verified and deployed image.
   - Simplifies manual pulling, debugging, and local deployment workflows.

---

## 5. Local Docker Registry & Verification Commands

### 5.1 Starting the Local Registry
Run the official Docker distribution registry on port `5000`:
```powershell
docker run -d -p 5000:5000 --restart=always --name registry registry:2
```

### 5.2 Verifying Registry Catalog
Verify that the registry is accessible and list registered repositories:
```powershell
curl http://localhost:5000/v2/_catalog
```
*Expected Output:*
```json
{"repositories":["esi-app"]}
```

### 5.3 Listing Image Tags
Query all available tags pushed for `esi-app`:
```powershell
curl http://localhost:5000/v2/esi-app/tags/list
```
*Expected Output:*
```json
{"name":"esi-app","tags":["18","latest"]}
```

---

## 6. Windows Host and Jenkins Service Setup

Because Jenkins runs as a Windows Service, the following prerequisites must be fulfilled on Windows 11:

### 1. Ensure Docker Desktop is Running
- Docker Desktop must be running in Linux container mode (`desktop-linux` context).
- Under Docker Desktop Settings > General: Ensure **"Start Docker Desktop when you log in"** and **"Expose daemon on tcp://localhost:2375 without TLS"** (optional) are configured if needed.

### 2. Add Windows User to `docker-users` Group
The account executing Docker commands must belong to the `docker-users` local group:
1. Open PowerShell as Administrator:
   ```powershell
   Add-LocalGroupMember -Group "docker-users" -Member "$env:USERNAME"
   ```
2. Log off and log back in (or restart) to apply group token changes.

### 3. Configure Jenkins Windows Service Logon Credentials
By default, the Jenkins Windows Service runs under `NT AUTHORITY\SYSTEM` (Local System), which lacks access to the Docker Desktop named pipe (`\\.\pipe\docker_engine`):
1. Press `Win + R`, type `services.msc`, and press Enter.
2. Locate the **Jenkins** service.
3. Right-click > **Properties** > **Log On** tab.
4. Select **This account** and enter your Windows username and password.
5. Click **Apply** and **OK**.
6. Restart the Jenkins service:
   ```powershell
   Restart-Service -Name "jenkins"
   ```

---

## 7. Evidence & Screenshot Placeholders

> [!NOTE]
> **[ Screenshot Placeholder 1: Local Docker Registry Running & Catalog Response ]**
> *(Command prompt showing `docker ps` with the `registry` container active and `curl http://localhost:5000/v2/_catalog` returning `{"repositories":["esi-app"]}`)*

---

> [!NOTE]
> **[ Screenshot Placeholder 2: Jenkins Job Configuration with Parameters ]**
> *(Jenkins job configuration or Build with Parameters page showing `ENVIRONMENT` (`dev`/`qa`) and `DEPLOY_MODE` (`docker`/`jar` with default `docker`))*

---

> [!NOTE]
> **[ Screenshot Placeholder 3: Jenkins Pipeline Execution View with Docker Stages ]**
> *(Stage View showing green stages: Checkout → Build → Test → Package → Archive → Selenium UI Tests → Docker Build → Push to Registry → Deploy Container, with Deploy (JAR) and Health Check skipped)*

---

> [!NOTE]
> **[ Screenshot Placeholder 4: Docker Build & Tagging Console Output ]**
> *(Console output for the 'Docker Build' stage showing `docker build -t esi-app:<BUILD_NUMBER> -t localhost:5000/esi-app:<BUILD_NUMBER> -t localhost:5000/esi-app:latest .`)*

---

> [!NOTE]
> **[ Screenshot Placeholder 5: Push to Registry & Tag Verification Console Output ]**
> *(Console output for 'Push to Registry' showing successful push to `localhost:5000` and the curl response from `http://localhost:5000/v2/esi-app/tags/list` displaying the build number tag and latest)*

---

> [!NOTE]
> **[ Screenshot Placeholder 6: Container Deployment & Healthcheck Console Output ]**
> *(Console output for 'Deploy Container' showing container cleanup, `docker run -d --name esi-dev -p 8087:8085 ...`, ping delay, and successful curl to `/api/env` returning HTTP 200)*

---

> [!NOTE]
> **[ Screenshot Placeholder 7: Browser Verification of Dockerized Application ]**
> *(Browser showing the ESI dashboard loaded at `http://localhost:8087/` with the ENV badge reading `ENV: DEV · PORT 8085`)*

---

> [!NOTE]
> **[ Screenshot Placeholder 8: Host Docker Status (`docker ps`) ]**
> *(Terminal showing `docker ps` with `esi-dev` listening on `0.0.0.0:8087->8085/tcp` alongside the `registry` container on port `5000`)*
