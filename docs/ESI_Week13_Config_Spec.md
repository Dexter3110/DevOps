# ESI Week 13: Configuration Management & Service Deployment with Ansible

## 1. Overview and Architecture

This specification document outlines the automated configuration management, service provisioning, and deployment workflow for the **Employee Skill Inventory (ESI)** Spring Boot application using **Ansible** on **WSL2 Ubuntu 24.04 LTS**.

### 1.1 Architecture Highlights
- **Declarative Infrastructure as Code (IaC):** All server states, packages, user permissions, directories, and service configurations are defined declaratively without ad-hoc shell commands.
- **Strict Idempotency:** The playbook ensures that subsequent executions against an unchanged environment result in zero state changes (`changed=0`), eliminating configuration drift.
- **Release Directory & Symlink Strategy:** Deployments are isolated in `/opt/esi/releases/<release_version>/`, with active traffic routed through an `/opt/esi/current` symlink. This mirrors production enterprise standards and enables instantaneous atomic rollbacks.
- **Principle of Least Privilege:** The application runs under a dedicated system user (`esi`) and group (`esi`) configured with a `/usr/sbin/nologin` shell, preventing interactive terminal access.
- **Native Systemd Supervision:** The Spring Boot application is managed as a standard systemd service (`esi.service`) configured with automatic recovery (`Restart=on-failure`), managed restart handlers, and centralized logging to `/var/log/esi/app.log`.
- **Automated Health Verification:** The playbook includes post-deployment verification using `ansible.builtin.wait_for` (port polling) and `ansible.builtin.uri` (REST health check on `/api/env` with retries).

```
+---------------------------------------------------------------------------------+
|                               WSL2 Ubuntu 24.04                                 |
|                                                                                 |
|  /mnt/c/Sahil/.../Project/target/esi-project-0.0.1-SNAPSHOT.jar (Source)        |
|                                        │ (ansible.builtin.copy)                 |
|                                        ▼                                        |
|  /opt/esi/releases/v1/esi-project.jar <──────────+                             |
|  /opt/esi/releases/v2/esi-project.jar            │                              |
|                                                  │ (symlink)                    |
|  /opt/esi/current ───────────────────────────────┘                              |
|         │                                                                       |
|         ▼ (systemd: esi.service)                                                |
|  java -jar /opt/esi/current/esi-project.jar --server.port=8090                  |
|         │                                                                       |
|         ├── Logs: /var/log/esi/app.log                                          |
|         └── Health Check: http://127.0.0.1:8090/api/env                        |
+---------------------------------------------------------------------------------+
```

---

## 2. Server Prerequisites Specification

The table below details all server components, files, permissions, and network ports configured and enforced by the Ansible playbook:

| Category | Component / Resource | Target Value / Path | Owner : Group | Permissions / Mode | Functional Purpose |
|---|---|---|---|---|---|
| **Package** | `openjdk-21-jre-headless` | APT Package | `root:root` | System Default | Runtime environment for Spring Boot (Java 21) |
| **Package** | `curl` | APT Package | `root:root` | System Default | Command-line utility for HTTP verification and troubleshooting |
| **User & Group** | `esi` (Group) | GID (System) | `root:root` | N/A | Dedicated security group for application isolation |
| **User & Group** | `esi` (User) | UID (System), Shell: `/usr/sbin/nologin` | `root:root` | N/A | Dedicated non-privileged system user without login shell |
| **Directory** | Application Base | `/opt/esi` | `esi:esi` | `0755` (`rwxr-xr-x`) | Root directory for application deployment artifacts |
| **Directory** | Releases Store | `/opt/esi/releases` | `esi:esi` | `0755` (`rwxr-xr-x`) | Directory housing versioned release subfolders |
| **Directory** | Active Release Folder | `/opt/esi/releases/{{ release_version }}` | `esi:esi` | `0755` (`rwxr-xr-x`) | Dedicated release folder for current version (default: `v1`) |
| **Directory** | Log Directory | `/var/log/esi` | `esi:esi` | `0755` (`rwxr-xr-x`) | Application log directory writable by `esi` user |
| **File** | Application Binary | `/opt/esi/releases/{{ release_version }}/esi-project.jar` | `esi:esi` | `0644` (`rw-r--r--`) | Standalone Spring Boot executable JAR |
| **Symlink** | Active Symlink | `/opt/esi/current` -> `/opt/esi/releases/{{ release_version }}` | `esi:esi` | Link | Symbolic link pointing to the currently active release directory |
| **Template / Unit**| Systemd Service Unit | `/etc/systemd/system/esi.service` | `root:root` | `0644` (`rw-r--r--`) | Systemd unit configuration managing application lifecycle |
| **Service** | `esi.service` | State: `started`, Enabled: `yes` | `root:root` | N/A | Background daemon supervised by systemd |
| **Port** | Application Port | `8090` (Configurable via `app_port`) | N/A | TCP Listen | HTTP port exposed for application traffic and health checks |
| **Endpoint** | REST Environment API | `http://127.0.0.1:8090/api/env` | N/A | HTTP 200 OK | Health check endpoint returning active profile and runtime port |

---

## 3. Playbook Configuration Structure

The playbook resides in the `ansible/` directory within the project repository:

```
ansible/
├── ansible.cfg              # Ansible configuration (inventory path, privilege escalation, stdout formatting)
├── inventory.ini            # Target inventory definition (localhost connection=local)
├── site.yml                 # Main idempotent provisioning and deployment playbook
└── templates/
    └── esi.service.j2       # Jinja2 template for systemd unit file
```

### 3.1 Playbook Variables & Defaults

| Variable Name | Default Value | Description |
|---|---|---|
| `app_port` | `8090` | HTTP port on which the Spring Boot application listens |
| `app_profile` | `dev` | Active Spring profile passed via `--spring.profiles.active` |
| `release_version` | `v1` | Release identifier used for versioned directory isolation |
| `jar_src` | `/mnt/c/Sahil/sem7/DevOps/project8/Project/target/esi-project-0.0.1-SNAPSHOT.jar` | Source path of compiled JAR on WSL host mount |

---

## 4. Step-by-Step WSL2 Setup & Execution Guide

### Step 1: Enable Systemd in WSL2 Ubuntu 24.04
Ubuntu 24.04 in WSL2 requires systemd to be explicitly enabled in `/etc/wsl.conf`.

Inside your WSL terminal, run:
```bash
sudo bash -c 'cat <<EOF >> /etc/wsl.conf
[boot]
systemd=true
EOF'
```

### Step 2: Restart WSL2 from Windows PowerShell
To apply the systemd boot configuration, shut down WSL completely from a Windows PowerShell terminal:
```powershell
wsl --shutdown
```
Re-open your WSL terminal. Verify that systemd is active:
```bash
systemctl is-system-running
# Expected output: running or degraded (both indicate systemd is functional)
```

### Step 3: Install Ansible in Ubuntu 24.04
Inside your WSL terminal, update package repositories and install Ansible:
```bash
sudo apt update
sudo apt install -y ansible
ansible --version
```

### Step 4: Build the ESI Application JAR on Windows
In your Windows PowerShell terminal (inside `C:\Sahil\sem7\DevOps\project8\Project`):
```powershell
mvn clean package -DskipTests
```
This generates the packaged artifact at:
`C:\Sahil\sem7\DevOps\project8\Project\target\esi-project-0.0.1-SNAPSHOT.jar`

### Step 5: WSL Path Translation Reference
Windows filesystem drives are mounted in WSL2 under `/mnt/`. The mapping for this project is:
- **Windows Path:** `C:\Sahil\sem7\DevOps\project8\Project\target\esi-project-0.0.1-SNAPSHOT.jar`
- **WSL2 Path:** `/mnt/c/Sahil/sem7/DevOps/project8/Project/target/esi-project-0.0.1-SNAPSHOT.jar`

### Step 6: Execute the Ansible Playbook
Navigate to the `ansible/` directory within WSL and execute the playbook:
```bash
cd /mnt/c/Sahil/sem7/DevOps/project8/Project/ansible
ansible-playbook -i inventory.ini site.yml
```

To run with custom parameters (e.g. deploying version `v2` or changing the port to `8091`):
```bash
ansible-playbook -i inventory.ini site.yml \
  -e "release_version=v2" \
  -e "app_port=8091" \
  -e "app_profile=prod" \
  -e "jar_src=/mnt/c/Sahil/sem7/DevOps/project8/Project/target/esi-project-0.0.1-SNAPSHOT.jar"
```

### Step 7: Verify Playbook Idempotency
Run the playbook a second time without changing parameters:
```bash
ansible-playbook -i inventory.ini site.yml
```
Verify that the `PLAY RECAP` reports `changed=0`.

---

## 5. First-Run Execution Log Placeholder

Below is the recording placeholder for the initial Ansible execution and the subsequent idempotency test run.

### 5.1 Initial Run Log (Run 1: Provisioning & Deployment)
```text
PLAY [Deploy Employee Skill Inventory (ESI) Spring Boot Service] ***************

TASK [Gathering Facts] *********************************************************
ok: [localhost]

TASK [Install OpenJDK 21 JRE headless and curl] ********************************
changed: [localhost]

TASK [Ensure esi system group exists] ******************************************
changed: [localhost]

TASK [Ensure esi system user exists] *******************************************
changed: [localhost]

TASK [Ensure base and release directories exist] *******************************
changed: [localhost] => (item=/opt/esi)
changed: [localhost] => (item=/opt/esi/releases)
changed: [localhost] => (item=/opt/esi/releases/v1)
changed: [localhost] => (item=/var/log/esi)

TASK [Copy application JAR artifact to release directory] **********************
changed: [localhost]

TASK [Update current release symlink] ******************************************
changed: [localhost]

TASK [Deploy systemd unit file for esi service] ********************************
changed: [localhost]

TASK [Reload systemd daemon if unit file changed] ******************************
ok: [localhost]

TASK [Ensure esi service is enabled and started] *******************************
changed: [localhost]

TASK [Flush handlers to restart service if release or configuration changed] ***

RUNNING HANDLER [Restart esi service] ******************************************
changed: [localhost]

TASK [Wait for application port to become ready] *******************************
ok: [localhost]

TASK [Verify application health via /api/env endpoint] *************************
ok: [localhost]

TASK [Output deployment verification details] **********************************
ok: [localhost] => 
  msg:
  - ESI service deployed successfully!
  - 'Active Release: v1'
  - 'Port: 8090'
  - 'Profile: dev'
  - 'Response: {''environment'': ''dev'', ''port'': ''8090''}'

PLAY RECAP *********************************************************************
localhost                  : ok=14   changed=9    unreachable=0    failed=0    skipped=0    rescued=0    ignored=0
```

### 5.2 Idempotency Verification Log (Run 2: Zero Changes Expected)
```text
PLAY [Deploy Employee Skill Inventory (ESI) Spring Boot Service] ***************

TASK [Gathering Facts] *********************************************************
ok: [localhost]

TASK [Install OpenJDK 21 JRE headless and curl] ********************************
ok: [localhost]

TASK [Ensure esi system group exists] ******************************************
ok: [localhost]

TASK [Ensure esi system user exists] *******************************************
ok: [localhost]

TASK [Ensure base and release directories exist] *******************************
ok: [localhost] => (item=/opt/esi)
ok: [localhost] => (item=/opt/esi/releases)
ok: [localhost] => (item=/opt/esi/releases/v1)
ok: [localhost] => (item=/var/log/esi)

TASK [Copy application JAR artifact to release directory] **********************
ok: [localhost]

TASK [Update current release symlink] ******************************************
ok: [localhost]

TASK [Deploy systemd unit file for esi service] ********************************
ok: [localhost]

TASK [Reload systemd daemon if unit file changed] ******************************
skipping: [localhost]

TASK [Ensure esi service is enabled and started] *******************************
ok: [localhost]

TASK [Flush handlers to restart service if release or configuration changed] ***

TASK [Wait for application port to become ready] *******************************
ok: [localhost]

TASK [Verify application health via /api/env endpoint] *************************
ok: [localhost]
TASK [Output deployment verification details] **********************************
ok: [localhost] => 
  msg:
  - ESI service deployed successfully!
  - 'Active Release: v1'
  - 'Port: 8090'
  - 'Profile: dev'
  - 'Response: {''environment'': ''dev'', ''port'': ''8090''}'

PLAY RECAP *********************************************************************
localhost                  : ok=12   changed=0    unreachable=0    failed=0    skipped=1    rescued=0    ignored=0
```

---

## 6. Verification and Troubleshooting Reference

### 6.1 Checking Service Status
```bash
sudo systemctl status esi.service
```

### 6.2 Monitoring Application Logs
```bash
# Direct log file inspection
sudo tail -f /var/log/esi/app.log

# Systemd journal logs
sudo journalctl -u esi.service -f
```

### 6.3 Querying Health Endpoint Manually
```bash
curl -i http://127.0.0.1:8090/api/env
```
Expected HTTP 200 JSON Response:
```json
{
  "environment": "local",
  "port": "8090"
}
```

### 6.4 Service Management Commands
```bash
sudo systemctl restart esi.service
sudo systemctl stop esi.service
sudo systemctl start esi.service
```
