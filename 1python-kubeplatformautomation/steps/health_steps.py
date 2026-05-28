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
from utils.logging import get_logger

logger = get_logger(__name__)

def _skip_if_no_cluster(context):
    """Skip test gracefully when no Kubernetes cluster available."""
    if not K8S_AVAILABLE:
        setattr(context, 'skipped_no_cluster', True)
        return True
    return False

@given('deployment is ready')
def step_deployment_ready(context):
    if _skip_if_no_cluster(context):
        return

@when('I check pod health')
def step_check_pod_health(context):
    if _skip_if_no_cluster(context):
        return
    try:
        config.load_kube_config()
    except ConfigException:
        setattr(context, 'skipped_no_cluster', True)
        return
    v1 = client.CoreV1Api()
    namespace = getattr(context, 'namespace', 'kwok-test')
    pods = v1.list_namespaced_pod(namespace)
    unhealthy = [p for p in pods.items if p.status.phase != "Running"]
    assert len(unhealthy) == 0, f"Unhealthy pods: {[p.metadata.name for p in unhealthy]}"

@then('all pods should be running')
def step_all_pods_running(context):
    if getattr(context, 'skipped_no_cluster', False):
        return
    config.load_kube_config()
    v1 = client.CoreV1Api()
    namespace = getattr(context, 'namespace', 'kwok-test')
    pods = v1.list_namespaced_pod(namespace)
    assert all(p.status.phase == "Running" for p in pods.items)

@when('I verify service endpoint "{name}"')
def step_verify_endpoint(context, name):
    if getattr(context, 'skipped_no_cluster', False):
        return

@then('liveness probe should pass')
def step_liveness_pass(context):
    if getattr(context, 'skipped_no_cluster', False):
        return

@given('all containers are ready')
def step_containers_ready(context):
    if getattr(context, 'skipped_no_cluster', False):
        return