from behave import given, when, then
import os

@given('I load environment "{env}"')
def step_load_env(context, env):
    context.environment = env
    context.namespace = os.environ.get("NAMESPACE", "kwok-test")

@given('I set test data "{key}" to "{value}"')
def step_set_testdata(context, key, value):
    context.test_data = getattr(context, "test_data", {})
    context.test_data[key] = value

@given('I set timeout to {seconds:d} seconds')
def step_set_timeout(context, seconds):
    context.timeout = seconds

@given('I set namespace to "{ns}"')
def step_set_namespace(context, ns):
    context.namespace = ns

@when('I initialize test context')
def step_init_context(context):
    context.results = {"passed": 0, "failed": 0, "errors": []}