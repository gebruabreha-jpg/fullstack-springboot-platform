import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.logging import setup_logging, get_logger
from pathlib import Path

log = get_logger(__name__)

def before_all(context):
    """Run infrastructure setup before all tests."""
    import subprocess
    setup_logging()
    log.info("Starting infrastructure setup...")
    
    # Run 1.py setup only
    result = subprocess.run(
        [sys.executable, "steps/1.py", "--setup-only"], 
        cwd=Path(__file__).parent.parent,
        capture_output=True, text=True
    )
    if result.returncode != 0:
        log.error(f"Setup failed: {result.stderr}")
    log.info("Infrastructure setup complete")


def after_all(context):
    """Run infrastructure teardown after all tests."""
    import subprocess
    log.info("Starting infrastructure teardown...")
    
    teardown_script = Path(__file__).parent.parent / "infra" / "window_script" / "teardown_kwok.ps1"
    if teardown_script.exists():
        subprocess.run(["powershell", "-File", str(teardown_script)])
    log.info("Infrastructure teardown complete")