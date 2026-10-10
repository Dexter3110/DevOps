#!/usr/bin/env bash
# ==============================================================================
# Employee Skill Inventory (ESI) - Week 14 Reliability & Rollback Demo
# Demonstrates node wiping, initial provisioning, idempotency, multi-version
# release deployment, deliberate failure handling, automated rollback, and
# check/diff mode verification.
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

export ANSIBLE_CONFIG="${SCRIPT_DIR}/ansible.cfg"
LOG_FILE="${SCRIPT_DIR}/demo_reliability.log"

AUTO_MODE=false
if [[ "${1:-}" == "--auto" || "${1:-}" == "-y" ]]; then
    AUTO_MODE=true
fi

# Colors for terminal output
BOLD="\033[1m"
GREEN="\033[0;32m"
CYAN="\033[0;36m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
RESET="\033[0m"

log_step() {
    local title="$1"
    echo -e "\n${BOLD}${CYAN}========================================================================${RESET}"
    echo -e "${BOLD}${YELLOW}${title}${RESET}"
    echo -e "${BOLD}${CYAN}========================================================================${RESET}\n"
}

pause_prompt() {
    if [ "$AUTO_MODE" = false ]; then
        echo -e "\n${BOLD}${GREEN}Press [Enter] to continue to the next step...${RESET}"
        read -r
    fi
}

# Start logging
exec > >(tee "$LOG_FILE") 2>&1

echo -e "${BOLD}${GREEN}Starting ESI Week 14 Reliability & Automated Rollback Demonstration${RESET}"
echo -e "Target Directory: ${SCRIPT_DIR}"
echo -e "Logging Output to: ${LOG_FILE}\n"

# ------------------------------------------------------------------------------
# STEP A: Wipe the node to pristine state
# ------------------------------------------------------------------------------
log_step "STEP A: Wipe Node (Stop service, remove unit, delete dirs & esi user)"
echo "[*] Executing wipe playbook to return system to clean state..."
ansible-playbook -i inventory.ini wipe.yml

echo -e "\n[*] Verifying node clean state:"
echo -n "Service status: "
systemctl is-active esi.service 2>/dev/null || echo "inactive / not found"
echo -n "Application dir: "
ls -d /opt/esi 2>/dev/null || echo "/opt/esi removed"
echo -n "ESI User: "
id esi 2>/dev/null || echo "esi user removed"

pause_prompt

# ------------------------------------------------------------------------------
# STEP B: First provisioning run (Expect changed=N)
# ------------------------------------------------------------------------------
log_step "STEP B: First Provisioning Run (Expect changed=N)"
echo "[*] Provisioning OpenJDK 21, creating user/dirs, deploying v1 JAR, starting service..."
ansible-playbook -i inventory.ini site.yml -e "release_version=v1"

pause_prompt

# ------------------------------------------------------------------------------
# STEP C: Second run with no changes (Idempotency proof, expect changed=0)
# ------------------------------------------------------------------------------
log_step "STEP C: Second Run (Idempotency Proof, Expect changed=0)"
echo "[*] Re-running playbook against active environment to verify idempotence..."
ansible-playbook -i inventory.ini site.yml -e "release_version=v1"

pause_prompt

# ------------------------------------------------------------------------------
# STEP D: Curl health check on port 8090
# ------------------------------------------------------------------------------
log_step "STEP D: Query REST Health Endpoint (/api/env on Port 8090)"
echo "[*] Performing HTTP GET request to http://127.0.0.1:8090/api/env..."
curl -i http://127.0.0.1:8090/api/env
echo ""

pause_prompt

# ------------------------------------------------------------------------------
# STEP E: Deploy release v2
# ------------------------------------------------------------------------------
log_step "STEP E: Deploy Release v2 (-e release_version=v2)"
echo "[*] Deploying version v2 to isolated directory /opt/esi/releases/v2..."
ansible-playbook -i inventory.ini site.yml -e "release_version=v2"

echo -e "\n[*] Verifying symlink points to release v2:"
ls -ld /opt/esi/current
echo -n "Live API env: "
curl -s http://127.0.0.1:8090/api/env
echo ""

pause_prompt

# ------------------------------------------------------------------------------
# STEP F: Deploy deliberately broken release v3 (Expect Loud Failure)
# ------------------------------------------------------------------------------
log_step "STEP F: Deploy Broken Release v3 (Text File as JAR, Expect Loud Play Failure)"
echo "[*] Creating corrupt dummy JAR artifact at /tmp/corrupt-esi.jar..."
echo "CORRUPT NON-JAVA FILE - INTENTIONAL FAILURE DEMONSTRATION" > /tmp/corrupt-esi.jar

echo "[*] Executing deployment of release v3 (Expect task failure and error diagnostics)..."
set +e
ansible-playbook -i inventory.ini site.yml \
    -e "release_version=v3" \
    -e "jar_src=/tmp/corrupt-esi.jar"
PLAY_EXIT_CODE=$?
set -e

if [ $PLAY_EXIT_CODE -ne 0 ]; then
    echo -e "\n${BOLD}${RED}[PASS] Playbook failed loudly as expected with exit code ${PLAY_EXIT_CODE}.${RESET}"
else
    echo -e "\n${BOLD}${RED}[FAIL] Expected playbook to fail, but it succeeded!${RESET}"
    exit 1
fi

pause_prompt

# ------------------------------------------------------------------------------
# STEP G: Run rollback.yml -e rollback_to=v2 & Verify
# ------------------------------------------------------------------------------
log_step "STEP G: Rollback to Release v2 (ansible-playbook rollback.yml -e rollback_to=v2)"
echo "[*] Executing automated rollback to release v2..."
ansible-playbook -i inventory.ini rollback.yml -e "rollback_to=v2"

echo -e "\n[*] Verifying active symlink repointed to v2:"
ls -ld /opt/esi/current
echo -e "\n[*] Releases in /opt/esi/releases:"
ls -l /opt/esi/releases

echo -e "\n[*] Querying health check on rolled-back service:"
curl -i http://127.0.0.1:8090/api/env
echo ""

pause_prompt

# ------------------------------------------------------------------------------
# STEP H: Run site.yml --check --diff
# ------------------------------------------------------------------------------
log_step "STEP H: Dry-Run Verification with --check --diff"
echo "[*] Running site.yml in check mode with diffs against release v2..."
ansible-playbook -i inventory.ini site.yml -e "release_version=v2" --check --diff

echo -e "\n${BOLD}${GREEN}========================================================================${RESET}"
echo -e "${BOLD}${GREEN}DEMO SEQUENCE COMPLETED SUCCESSFULLY!${RESET}"
echo -e "${BOLD}${GREEN}Log saved to: ${LOG_FILE}${RESET}"
echo -e "${BOLD}${GREEN}========================================================================${RESET}\n"
