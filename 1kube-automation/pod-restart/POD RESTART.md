# Pod Restart Actions — Reference

## Key Classes and Methods

### PodAction Enum
- `PodAction.DELETED` - Graceful pod deletion via `kubectl delete --grace-period`
- `PodAction.FORCE_DELETED` - Force deletion via `kubectl delete --force --grace-period=0`
- `PodAction.RESTARTED` - Kill all containers in pod via `killContainers()`

### Pod Restart Methods

**ConfigMapHelper.restartPodsWithPrefix(kubectlApi, podPrefix, timeout)**
- Used in `BeetsSuiteFixture.java`
- Finds pods by prefix, deletes them, waits for recovery

**PodAction.RESTARTED.execute(k8sNodeApis, kubectlApi, properties, podName)**
- Used in `UpgradeSteps.java` for restarting pods during Helm upgrade
- Synchronized block to get container IDs
- Calls `k8sNodeApi.killContainers(killRequests)` to kill all containers
- Polls until pod recovery completes

### Pod Kill Methods

**ActionsHelper.killContainerInPod(podName, containerName, kubectlApi, k8sNodeApis)**
- Kills specific container in pod
- Gets container ID and node name via kubectl
- Executes kill via K8sNodeApi

**KubectlApi.deletePod(podName)**
- Deletes pod directly
- New pod created by ReplicaSet if configured

**kubectlApi.killContainerInsidePod(podName, containerName)**
- Alternative container kill method (may exist)

## Execution Flow

```
PodAction.RESTARTED
  → Get all containers in pod (synchronized)
  → Build KillRequest for each container
  → Get node hosting pod
  → Kill all containers via K8sNodeApi
  → Poll until pod recovery

Pod Deletion
  → kubectlApi.deletePod(podName)
  → Wait for pod to be recreated
  → Verify pod is running
```

## Use Cases

- **KVDB restart** - Sequential master/replica kills
- **Helm upgrade coordination** - Restart pods during upgrade
- **CRE pod reboot** - Kill one pod at a time, verify recovery
- **Random pod selection** - Robustness testing with random selection