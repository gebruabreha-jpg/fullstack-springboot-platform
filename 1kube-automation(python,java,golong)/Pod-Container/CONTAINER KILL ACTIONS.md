# Pod/Container Kill Actions — Reference

This section documents Pod/Container Kill Actions - fault injection mechanisms for testing pod resilience.

## PCC Framework Actions

- Kill containers by resource name or pod label pattern
- Kill specific processes inside pods with configurable signals (SIGTERM, SIGKILL, etc.)
- Restart pods ungracefully by killing their main container

## Beets Framework Actions

- Kill VPN-related containers on specific pods
- Reboot CRE pods using either container-kill or kubectl delete methods

Both frameworks support graceful vs force deletion and container-level precision targeting for chaos testing.

## Primary Implementation Files

### K8sContainerHelper.java (lines 64-228)

**KillProcessEntity() - kills process entities inside pods**
- Step 1: Verify table headers (pod header, container, kill signal, nr_of_pods, PE, nr_of_entities)
- Step 2: Iterate over each row in the DataTableWrapper
- Step 3: For each row - extract pod prefix, container name, kill signal (SIGTERM/SIGKILL), process entity
- Step 4: For each matching pod - get container ID, node name, K8sNodeApi; call `k8sNodeApi.killProcessEntities()`

**KillContainer() - kills containers by resource/prefix**
- Step 1: Verify table headers (pod header, container, kill signal, nr_of_pods, optional: role, namespace)
- Step 2: Build `nodeKillRequestsMap` via `createNodeKillRequestsMap()`
- Step 3: Execute kills in parallel via `ParallelExecutionUtil`

### K8sPodManagementSteps.java (lines 153-169)

**Pod Actions: runActionOnPodWithPrefix()**
- Uses `PodAction` enum (DELETED, FORCE_DELETED, RESTARTED)
- Calls `K8sPodPerformer.runActionOnPod()` which invokes `PodAction.execute()`

### KvdbSteps.java (lines 767-950)

**KVDB Fault Injection**
- `killContainerInSelectedKvdbPodsWithOutRoleCheck()` - kills containers in KVDB pods
- `sequentiallyKillPairedContainersInSelectedKvdbPodsWithOutRoleCheck()` - sequential kills with delay

## Key Execution Flow

```
Test Step → Helper Method → KillRequest Objects → K8sNodeApi.killContainers()/killProcessEntities() → Shell commands on worker node
```