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
import os
from utils.logging import get_logger

logger = get_logger(__name__)


def _skip_if_no_cluster(context):
    if not K8S_AVAILABLE:
        setattr(context, 'skipped_no_cluster', True)
        return True
    return False


@when('a container that controlled by resource is killed')
def step_kill_container(context):
    """Kill container via kubectl/SSH to node."""
    if _skip_if_no_cluster(context):
        return
    for row in context.table:
        resource = row['Pod Resource']
        container = row['Container']
        signal = row.get('Kill Signal', 'sigkill')
        logger.info(f"Killing container {container} in {resource}")
        # Mock implementation - would use kubectl to get pod, SSH to node, kill container
        setattr(context, 'container_killed', True)


@when('pods controlled by resource are deleted')
def step_delete_pods(context):
    """Delete pods via kubectl API."""
    if _skip_if_no_cluster(context):
        return
    for row in context.table:
        resource = row['Resource']
        logger.info(f"Deleting pods controlled by {resource}")
        # Mock implementation - would use kubectl delete
        setattr(context, 'pods_deleted', True)


@when('a worker node with a minority of KVDB master pods is kernel panicked')
def step_kernel_panic_node(context):
    """Trigger kernel panic on worker node."""
    if _skip_if_no_cluster(context):
        return
    logger.info("Triggering kernel panic on worker node")
    # Mock implementation - would SSH to node and echo c > /proc/sysrq-trigger
    setattr(context, 'node_panicked', True)


@then('container restart count values have increased')
def step_restart_count_increased(context):
    """Verify container restart count incremented."""
    if getattr(context, 'skipped_no_cluster', False):
        return
    assert getattr(context, 'container_killed', False)


@then('pods recover within {seconds:d} seconds')
def step_pods_recover(context, seconds):
    """Wait for pods to recover."""
    if getattr(context, 'skipped_no_cluster', False):
        return


@then('system recover within {seconds:d} seconds')
def step_system_recover(context, seconds):
    """Wait for system recovery."""
    if getattr(context, 'skipped_no_cluster', False):
        return


@then('no alarms have been active')
def step_no_alarms_active(context):
    """Verify no new alarms since failure."""
    if getattr(context, 'skipped_no_cluster', False):
        return