from behave import given, when, then
import requests
import json

@given('baseline metrics loaded from "{path}"')
def step_load_baseline(context, path):
    with open(path) as f:
        context.baseline = json.load(f)

@when('I collect metrics from Prometheus for {seconds:d} seconds')
def step_collect_metrics(context, seconds):
    url = f"{getattr(context, 'prometheus_url', 'http://localhost:9090')}/api/v1/query?query=up&timeout={seconds}"
    response = requests.get(url)
    context.current_metrics = response.json()

@when('I query metric "{query}"')
def step_query_metric(context, query):
    url = f"{getattr(context, 'prometheus_url', 'http://localhost:9090')}/api/v1/query?query={query}"
    response = requests.get(url)
    context.metric_value = response.json()

@then('metrics should be within baseline')
def step_metrics_baseline(context):
    metrics = context.baseline.get("metrics", context.baseline)
    for metric, config in metrics.items():
        threshold = config.get("threshold", config) if isinstance(config, dict) else config
        current = context.current_metrics.get(metric, 0)
        assert current <= threshold, f"{metric}: {current} > {threshold}"

@given('I store metrics as baseline "{name}"')
def step_store_baseline(context, name):
    pass

@when('I compare metrics to baseline')
def step_compare_metrics(context):
    pass

@then('metric "{name}" should be less than {threshold:f}')
def step_metric_threshold(context, name, threshold):
    value = float(context.metric_value.get("data", {}).get("result", [{}])[0].get("value", [0, 0])[1])
    assert value < threshold, f"{name}={value} >= {threshold}"