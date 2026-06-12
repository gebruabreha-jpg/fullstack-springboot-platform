import os
import sys
import subprocess
import logging
import json_log_formatter
import shutil
import json 

#loggging
#Structured logs (JSON) and Log collector (Fluent Bit / Vector / Filebeat)
# .sh - Shell scripts (Linux/WSL) and .ps1 - PowerShell (Windows native)
#every fearure file will use this 1.py + TC specific steps.
formatter = json_log_formatter.JSONFormatter()


#path to different folder and directorys

# Shell detection (robust)
def shell_detectiom():
    pass

# Setup dependencies
def setup_dependency():
    pass

# Setup tools
def setup_tools():
    pass
# Infra execution
def creat_infra():
    pass
#config_environemnt-infra
def config_infra():
    pass

#deploy pcg
def creat_pods():
    pass
    
#config_pcg
def conf_pods():
    pass

#health, cpu, metric check
def health_check():
    pass

# Main orchestration or clal methods:
# ----------------------------
def run(feature_file="features/health_check.feature"):
    log.info("=== START LOCAL ORCHESTRATION ===")

    try:
        install_dependencies()

        run_infra("setup")
        run_tests(feature_file)

        log.info("SUCCESS")

    finally:
        run_infra("teardown")

# CLI entrypoint
