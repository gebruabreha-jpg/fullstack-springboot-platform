"""Unit tests for log parsing utilities."""
import pytest

def count_errors_in_log(log_text: str) -> int:
    """Count ERROR and FATAL occurrences in log text."""
    return sum(1 for line in log_text.split('\n') if 'ERROR' in line or 'FATAL' in line)

def parse_log_pattern(log_text: str, pattern: str) -> list:
    """Extract matches from log using regex pattern."""
    import re
    return re.findall(pattern, log_text)

def test_count_errors_empty():
    assert count_errors_in_log("") == 0

def test_count_errors_single():
    assert count_errors_in_log("INFO start\nERROR fail\nINFO end") == 1

def test_count_errors_multiple():
    assert count_errors_in_log("ERROR: a\nFATAL: b\nERROR: c") == 3

def test_parse_pattern_match():
    log = "2024-01-01 ERROR in pod-xyz at 10:00"
    assert parse_log_pattern(log, r"pod-[\w]+") == ["pod-xyz"]

def test_parse_pattern_no_match():
    assert parse_log_pattern("no matches", r"ERROR") == []