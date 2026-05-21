import os
import sys
import subprocess
import logging
import shutil
import json

from utils.logging import setup_logging, get_logger

log = get_logger(__name__)

PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
INFRA_SCRIPT_DIR = os.path.join(PROJECT_ROOT, "infra", "window_script")


def install_dependencies():
    """Install Python dependencies from requirements.txt."""
    log.info("Installing dependencies...")
    req_file = os.path.join(PROJECT_ROOT, "requirements.txt")
    if os.path.exists(req_file):
        subprocess.run([sys.executable, "-m", "pip", "install", "-r", req_file], check=True)
    log.info("Dependencies installed")


def run_infra(action):
    """Run infrastructure setup or teardown scripts."""
    log.info(f"Running infra action: {action}")
    if action == "setup":
        script = os.path.join(INFRA_SCRIPT_DIR, "setup_kwok.ps1")
        if os.path.exists(script):
            subprocess.run(["powershell", "-File", script], cwd=INFRA_SCRIPT_DIR)
    elif action == "teardown":
        script = os.path.join(INFRA_SCRIPT_DIR, "teardown_kwok.ps1")
        if os.path.exists(script):
            subprocess.run(["powershell", "-File", script], cwd=INFRA_SCRIPT_DIR)
    log.info(f"Infra {action} complete")


def run_tests(feature_file="features/1.feature"):
    """Run behave tests for the specified feature file."""
    log.info(f"Running tests: {feature_file}")
    feature_path = os.path.join(PROJECT_ROOT, feature_file)
    if not os.path.exists(feature_path):
        feature_path = feature_file
    subprocess.run(["behave", feature_path], cwd=PROJECT_ROOT, check=False)


def setup_only():
    """Setup infrastructure only (for environment.py integration)."""
    setup_logging()
    log.info("=== SETUP ONLY ===")
    install_dependencies()
    run_infra("setup")


def run(feature_file="features/1.feature"):
    """Main orchestration with full lifecycle."""
    setup_logging()
    log.info("=== START LOCAL ORCHESTRATION ===")

    try:
        install_dependencies()
        run_infra("setup")
        run_tests(feature_file)
        log.info("SUCCESS")

    finally:
        run_infra("teardown")


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--setup-only":
        setup_only()
    else:
        run()