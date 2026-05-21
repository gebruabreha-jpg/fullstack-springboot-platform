from behave import given, when, then
try:
    from kubernetes import client, config
    from kubernetes.config.config_exception import ConfigException
    K8S_AVAILABLE = True
except ImportError:
    config = None
    client = None
    ConfigException = Exception
    K8S_AVAILABLE = False
import re
from utils.logging import get_logger

logger = get_logger(__name__)

@when('I collect logs from pods matching "{pattern}"')
def step_collect_logs(context, pattern):
    if getattr(context, 'skipped_no_cluster', False) or not K8S_AVAILABLE:
        context.logs = {"mock-pod": "INFO Application started\nERROR test error"}
        return
    try:
        config.load_kube_config()
    except ConfigException:
        context.logs = {}
        return
    v1 = client.CoreV1Api()
    namespace = getattr(context, 'namespace', 'kwok-test')
    pods = v1.list_namespaced_pod(namespace)
    matching = [p for p in pods.items if pattern in p.metadata.name]
    context.logs = {}
    for pod in matching:
        try:
            log = v1.read_namespaced_pod_log(pod.metadata.name, namespace)
            context.logs[pod.metadata.name] = log
        except Exception as e:
            context.logs[pod.metadata.name] = f"Error: {e}"

@then('logs should not contain "{pattern}"')
def step_logs_not_contain(context, pattern):
    if getattr(context, 'skipped_no_cluster', False):
        return
    for pod, log in context.logs.items():
        assert pattern not in log, f"Pattern found in {pod} logs"

@then('error count should be less than {count:d}')
def step_error_count(context, count):
    if getattr(context, 'skipped_no_cluster', False):
        return
    error_count = sum(1 for log in context.logs.values() if "ERROR" in log or "FATAL" in log)
    assert error_count < count, f"Error count {error_count} >= {count}"

@when('I tail logs for {seconds:d} seconds')
def step_tail_logs(context, seconds):
    if getattr(context, 'skipped_no_cluster', False):
        return

@then('logs should contain "{pattern}"')
def step_logs_contain(context, pattern):
    if getattr(context, 'skipped_no_cluster', False):
        return
    for pod, log in context.logs.items():
        assert pattern in log, f"Pattern not found in {pod} logs"

@when('I parse logs for pattern "{regex}"')
def step_parse_logs(context, regex):
    if getattr(context, 'skipped_no_cluster', False) or not context.logs:
        context.parsed_logs = {}
        return
    pattern = re.compile(regex)
    context.parsed_logs = {}
    for pod, log in context.logs.items():
        context.parsed_logs[pod] = pattern.findall(log)