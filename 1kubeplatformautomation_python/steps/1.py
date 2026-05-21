import os
import sys
import subprocess
import logging
import shutil

#loggging

#path to different folder and directorys

# Setup dependencies

# Setup tools

# Shell detection (robust)

# Infra execution

#config_environemnt-infra

#deploy pcg

#config_pcg

# Test execution

#health, cpu, metric check

#others

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
