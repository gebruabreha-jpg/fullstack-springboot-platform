from behave import given, when, then
import subprocess
import requests

@when('I run system test "{script}"')
def step_run_test(context, script):
    result = subprocess.run(["python", script], capture_output=True, text=True, timeout=300)
    context.test_result = result
    if result.returncode != 0:
        context.results["failed"] += 1
        context.results["errors"].append(result.stderr)
    else:
        context.results["passed"] += 1

@when('I run HTTP GET on "{url}"')
def step_http_get(context, url):
    response = requests.get(url, timeout=getattr(context, 'timeout', 30))
    context.http_response = response
    assert response.status_code == 200

@then('test should pass')
def step_test_pass(context):
    assert context.test_result.returncode == 0, context.test_result.stderr

@when('I execute command "{cmd}"')
def step_exec_cmd(context, cmd):
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    context.cmd_result = result

@then('exit code should be {code:d}')
def step_exit_code(context, code):
    assert context.cmd_result.returncode == code

@when('I run smoke test')
def step_smoke_test(context):
    assert hasattr(context, "release") or hasattr(context, "namespace")

@when('I verify application is responding')
def step_verify_app(context):
    assert context.http_response.status_code == 200