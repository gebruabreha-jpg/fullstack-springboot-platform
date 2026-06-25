from behave import given, when, then
import json
from datetime import datetime
import os
from utils.logging import get_logger

logger = get_logger(__name__)

@then('I generate test report')
def step_generate_report(context):
    report = {
        "timestamp": datetime.now().isoformat(),
        "environment": getattr(context, "environment", "unknown"),
        "namespace": getattr(context, "namespace", "kwok-test"),
        "results": context.results,
        "passed": context.results.get("passed", 0),
        "failed": context.results.get("failed", 0)
    }
    context.report = report

@then('I write report to "{path}"')
def step_write_report(context, path):
    with open(path, "w") as f:
        json.dump(context.report, f, indent=2)

@then('I archive results')
def step_archive_results(context):
    namespace = getattr(context, 'namespace', 'kwok-test')
    os.makedirs(f"reports/history/{namespace}", exist_ok=True)

@then('test status should be "{status}"')
def step_test_status(context, status):
    assert status in ("PASS", "FAIL")
    assert status == "PASS" if context.results.get("failed", 0) == 0 else status == "FAIL"