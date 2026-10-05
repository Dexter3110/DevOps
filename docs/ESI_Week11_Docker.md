# ESI Week 11 Docker Containerization Guide & Lifecycle Documentation

## 1. Overview and Architecture
This document details the containerization of the **Employee Skill Inventory (ESI)** Spring Boot application using a production-grade, multi-stage Docker build.

### Multi-Stage Build Architecture
- **Stage 1: Build Environment (`maven:3.9-eclipse-temurin-21`)**
  - Contains complete JDK 21 and Maven 3.9 tooling.
  - Resolves dependencies and packages the executable JAR file (`esi-project-0.0.1-SNAPSHOT.jar`).
  - Discarded after build completion, preventing build tools and source code from leaking into production.
- **Stage 2: Runtime Environment (`eclipse-temurin:21-jre`)**
  - Minimal JRE 21 footprint.
  - Runs as an unprivileged, non-root user (`appuser`, UID `1001`) in `appgroup`.
  - Includes `curl` for container health monitoring.
  - Exposes port `8085` and natively supports dynamic configuration via `SPRING_PROFILES_ACTIVE`.

---

## 2. Docker Image Details and Layer Breakdown

| Attribute | Specification |
|---|---|
| **Image Name** | `esi-app` |
| **Tag(s)** | `1.0`, `latest` |
| **Build Base** | `maven:3.9-eclipse-temurin-21` (~800 MB - discarded) |
| **Runtime Base** | `eclipse-temurin:21-jre` (~260 MB) |
| **Final Image Size** | ~330 MB (Includes JRE 21, curl, and Spring Boot fat JAR) |
| **User / UID** | `appuser` / `1001` (Non-root) |
| **Exposed Ports** | `8085` |
| **Healthcheck** | `CMD curl -f http://localhost:8085/api/env \|\| exit 1` |

### Runtime Layer Composition
1. `eclipse-temurin:21-jre` base OS and OpenJDK runtime layers.
2. Package manager layer installing `curl` utility and clearing `/var/lib/apt/lists/*`.
3. System user and group provisioning (`groupadd appgroup`, `useradd appuser`).
4. Application artifact layer (`COPY --from=builder app.jar`).
5. Security context declaration (`USER appuser`).
6. Metadata instructions (`EXPOSE`, `ENV`, `HEALTHCHECK`, `ENTRYPOINT`).

---

## 3. Complete Docker Container Lifecycle

### Step 1: Build the Docker Image
```bash
docker build -t esi-app:1.0 .
```
> **Explanation:**  
> Executes the multi-stage `Dockerfile`. Maven resolves all project dependencies and builds the application inside the builder stage. The final JAR is copied into the minimal JRE 21 runtime container, tagged as `esi-app:1.0`.

> [!NOTE]  
> **[ Screenshot Placeholder 1: `docker build` command and successful build output ]**  
> *(Insert screenshot showing build steps 1/12 through completion)*

---

### Step 2: List Local Images
```bash
docker images
```
> **Explanation:**  
> Displays all Docker images currently stored in the local registry, showing `REPOSITORY`, `TAG`, `IMAGE ID`, `CREATED`, and `SIZE` (~330 MB).

> [!NOTE]  
> **[ Screenshot Placeholder 2: `docker images` output showing `esi-app:1.0` ]**  
> *(Insert screenshot showing the image name, tag, and size)*

---

### Step 3: Tag the Image
```bash
docker tag esi-app:1.0 esi-app:latest
```
```bash
# Optional: Tag for Docker Hub registry
docker tag esi-app:1.0 dexter3110/esi-app:1.0
```
> **Explanation:**  
> Creates an alias or reference pointing to the existing image ID. In production and CI pipelines, this allows version tagging (`1.0`) alongside floating tags (`latest`) and remote repository namespaces.

> [!NOTE]  
> **[ Screenshot Placeholder 3: `docker tag` execution and updated `docker images` list ]**  
> *(Insert screenshot showing both `1.0` and `latest` pointing to the same Image ID)*

---

### Step 4: Run the Container in Detached Mode
```bash
docker run -d --name esi-dev -p 8085:8085 -e SPRING_PROFILES_ACTIVE=dev esi-app:1.0
```
> **Explanation:**  
> Starts the container in the background (`-d`), assigns the container name `esi-dev`, maps host port `8085` to container port `8085` (`-p 8085:8085`), sets the environment variable `SPRING_PROFILES_ACTIVE=dev`, and uses image `esi-app:1.0`.

> [!NOTE]  
> **[ Screenshot Placeholder 4: `docker run` execution returning container ID ]**  
> *(Insert screenshot of container launch)*

---

### Step 5: Check Running Containers and Health Status
```bash
docker ps
```
```bash
# List all containers (including stopped)
docker ps -a
```
> **Explanation:**  
> Lists running containers, verifying container name (`esi-dev`), status (`Up`, `healthy` once healthcheck passes), port bindings (`0.0.0.0:8085->8085/tcp`), and runtime command.

> [!NOTE]  
> **[ Screenshot Placeholder 5: `docker ps` showing status `Up (healthy)` ]**  
> *(Insert screenshot showing container running with healthy status)*

---

### Step 6: Follow Container Logs
```bash
docker logs -f esi-dev
```
> **Explanation:**  
> Streams (`-f`) standard output and error from the Spring Boot application running inside the container. Verifies Spring profile activation (`dev`), database initialization, `DataSeeder` logs, and Tomcat startup on port `8085`. Press `Ctrl+C` to exit streaming.

> [!NOTE]  
> **[ Screenshot Placeholder 6: `docker logs` showing Spring Boot startup banner and DataSeeder ]**  
> *(Insert screenshot showing Spring Boot logs)*

---

### Step 7: Execute Commands Inside the Running Container
```bash
# Verify non-root user execution
docker exec -it esi-dev id
```
```bash
# Verify internal health endpoint response
docker exec -it esi-dev curl -s http://localhost:8085/api/env
```
```bash
# Interactive shell access (if needed for troubleshooting)
docker exec -it esi-dev /bin/bash
```
> **Explanation:**  
> Runs a command directly within the namespace of the active container. `id` confirms the process executes as UID `1001(appuser)` rather than `root`, complying with container security best practices.

> [!NOTE]  
> **[ Screenshot Placeholder 7: `docker exec` output showing `uid=1001(appuser)` and `/api/env` output ]**  
> *(Insert screenshot of non-root user verification)*

---

### Step 8: Inspect Container Metadata and Health
```bash
docker inspect esi-dev
```
```bash
# Filter specifically for container Healthcheck status
docker inspect --format='{{json .State.Health.Status}}' esi-dev
```
```bash
# Filter for container IP and Environment configuration
docker inspect --format='{{range .Config.Env}}{{println .}}{{end}}' esi-dev
```
> **Explanation:**  
> Returns low-level JSON configuration and state information for the container, including network bindings, health check logs, mounts, and active environment variables.

> [!NOTE]  
> **[ Screenshot Placeholder 8: `docker inspect` showing Health state and environment variables ]**  
> *(Insert screenshot of docker inspect health check log)*

---

### Step 9: Stop, Start, and Restart Container Lifecycle
```bash
# Gracefully stop the container
docker stop esi-dev

# Start the stopped container
docker start esi-dev

# Restart the container
docker restart esi-dev
```
> **Explanation:**  
> - `docker stop`: Sends `SIGTERM` followed by `SIGKILL` after a grace period, safely shutting down Spring Boot.
> - `docker start`: Resumes the existing stopped container while preserving its state.
> - `docker restart`: Issues a stop followed by a start sequence.

> [!NOTE]  
> **[ Screenshot Placeholder 9: `docker stop`, `docker start`, and `docker restart` commands ]**  
> *(Insert screenshot of lifecycle commands)*

---

### Step 10: Remove the Container
```bash
docker stop esi-dev
docker rm esi-dev
```
*(Or force remove in one step: `docker rm -f esi-dev`)*
> **Explanation:**  
> Deletes the container instance and its writable container layer from the host system.

> [!NOTE]  
> **[ Screenshot Placeholder 10: `docker rm` confirmation ]**  
> *(Insert screenshot confirming container removal)*

---

### Step 11: Remove the Docker Image
```bash
docker rmi esi-app:latest
docker rmi esi-app:1.0
```
> **Explanation:**  
> Removes the specified tags and image layers from the local image store, freeing host disk storage.

> [!NOTE]  
> **[ Screenshot Placeholder 11: `docker rmi` untagged/deleted layers output ]**  
> *(Insert screenshot confirming image deletion)*

---

## 4. Application Verification in Browser
While the container `esi-dev` is active on port `8085`:
- **Web Dashboard:** [http://localhost:8085](http://localhost:8085)
- **Environment API:** [http://localhost:8085/api/env](http://localhost:8085/api/env)
- **Skill Catalogue API:** [http://localhost:8085/api/skills](http://localhost:8085/api/skills)
- **Exception Alerts API:** [http://localhost:8085/api/alerts/summary](http://localhost:8085/api/alerts/summary)

> [!NOTE]  
> **[ Screenshot Placeholder 12: Browser showing running ESI Dashboard at `http://localhost:8085` ]**  
> *(Insert screenshot of browser dashboard loaded from Docker container)*
