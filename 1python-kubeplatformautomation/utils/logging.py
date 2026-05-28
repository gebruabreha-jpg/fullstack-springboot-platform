"""Centralized logging configuration for KubePlatformAutomation."""
import logging
import os
from datetime import datetime

def setup_logging(log_dir="reports", log_level=logging.INFO):
    """Configure centralized logging for all test modules."""
    os.makedirs(log_dir, exist_ok=True)
    
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_file = os.path.join(log_dir, f"test_{timestamp}.log")
    
    logging.basicConfig(
        level=log_level,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.FileHandler(log_file),
            logging.StreamHandler()
        ]
    )
    
    return logging.getLogger(__name__)

def get_logger(name):
    """Get a logger instance for a specific module."""
    return logging.getLogger(name)