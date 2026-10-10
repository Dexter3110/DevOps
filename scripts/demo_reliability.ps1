# ==============================================================================
# Employee Skill Inventory (ESI) - Week 14 Reliability & Rollback Demo
# PowerShell Wrapper to execute the reliability demonstration inside WSL2
# ==============================================================================

[CmdletBinding()]
param(
    [switch]$Auto
)

$ErrorActionPreference = "Stop"

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "  ESI Week 14: Reliability & Automated Rollback Demonstration (WSL2)   " -ForegroundColor Yellow
Write-Host "========================================================================" -ForegroundColor Cyan

$wslScriptPath = "/mnt/c/Sahil/sem7/DevOps/project8/Project/ansible/demo_reliability.sh"

# Ensure execute permissions on the script inside WSL
wsl -d Ubuntu -e chmod +x $wslScriptPath

if ($Auto) {
    Write-Host "[*] Executing demo in automated mode (--auto)..." -ForegroundColor Green
    wsl -d Ubuntu -e bash -c "cd /mnt/c/Sahil/sem7/DevOps/project8/Project/ansible && ./demo_reliability.sh --auto"
} else {
    Write-Host "[*] Executing demo in interactive mode (step-by-step)..." -ForegroundColor Green
    wsl -d Ubuntu -e bash -c "cd /mnt/c/Sahil/sem7/DevOps/project8/Project/ansible && ./demo_reliability.sh"
}
