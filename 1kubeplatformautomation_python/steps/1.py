import os
import sys
import subprocess
import logging
import shutil

#loggging

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
# Test execution
def run_traffic():
    pass

#health, cpu, metric check
def health_check():
    pass

#others
def other():
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
