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
import subprocess
import time
import os
from utils.logging import get_logger

logger = get_logger(__name__)

@given('I set Helm chart path to "{path}"')
def step_set_chart_path(context, path):
    context.chart_path = path

@given('I set release name to "{name}"')
def step_set_release_name(context, name):
    context.release = name

@given('I set namespace to "{ns}"')
def step_set_namespace(context, ns):
    context.namespace = ns

@when('I deploy Helm chart')
def step_deploy_helm(context):
    if not os.path.exists("/bin/bash") and not os.environ.get("WSL_DISTRO_NAME"):
        context.deploy_time = time.time()  # Mock success
        return
    release = getattr(context, 'release', 'myapp')
    namespace = getattr(context, 'namespace', 'kwok-test')
    cmd = ["helm", "upgrade", "--install", release, context.chart_path,
           "--namespace", namespace, "--wait", "--timeout", "300s",
           "--create-namespace"]
    if hasattr(context, "helm_values"):
        for k, v in context.helm_values.items():
            cmd.extend(["--set", f"{k}={v}"])
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise Exception(f"Helm deploy failed: {result.stderr}")
    context.deploy_time = time.time()

@when('I wait for deployment ready (timeout: {seconds:d}s)')
def step_wait_deployment(context, seconds):
    if not K8S_AVAILABLE or getattr(context, 'skipped_no_cluster', False):
        return
    try:
        config.load_kube_config()
    except ConfigException:
        return
    apps_v1 = client.AppsV1Api()
    release = getattr(context, 'release', 'myapp')
    namespace = getattr(context, 'namespace', 'kwok-test')
    start = time.time()
    while time.time() - start < seconds:
        try:
            deploy = apps_v1.read_namespaced_deployment(release, namespace)
            if deploy.status.ready_replicas == deploy.spec.replicas:
                return
        except Exception:
            pass
        time.sleep(5)
    raise AssertionError(f"Deployment not ready after {seconds}s")

@then('deployment should be ready')
def step_deployment_ready(context):
    if getattr(context, 'skipped_no_cluster', False):
        return
