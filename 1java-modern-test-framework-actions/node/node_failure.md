# Node Failure Actions — Reference

K8sNode.java (base class) - Lines 875-921 handle KERNEL_PANICKED via triggerKernelPanic() which sends kernel panic command asynchronously, then reconnects after timeout.

## Platform-Specific Implementations

### K8sBaremetalNode.java
Action | Implementation
-------|---------------
SHUT_DOWN | IPMI: `ipmitool.shutdownNode(ipAddress)` or CAPI: `kubectl` with `POWER_OFF_ANNOTATION`
RESTARTED | IPMI: `ipmitool.restartNode(ipAddress)` or CAPI: power off → on cycle
FORCE_RESTARTED | IPMI: `ipmitool.restartNodeWithForce(ipAddress)`
KERNEL_PANICKED | Calls `performKernelPanic()` which triggers panic and reconnects

### K8sMesosNode.java
Action | Implementation
-------|---------------
SHUT_DOWN | `computeHost.shutdownNode(getHostname())`
RESTARTED | `computeHost.rebootNode(getHostname())`
FORCE_RESTARTED | `computeHost.rebootNodeWithForce(getHostname())`
DESTROYED | `computeHost.destroyNode(getHostname())`

### NexusNode.java
Action | Implementation
-------|---------------
RESTARTED/FORCE_RESTARTED | `azureHandler.restartNode(getHostname())`

### GdceNode.java
Action | Implementation
-------|---------------
RESTARTED | Pub/Sub `NodeRebootRequest` with `graceful=true`
FORCE_RESTARTED | Pub/Sub `NodeRebootRequest` with `graceful=false`

### K8sCeeNode.java
Action | Implementation
-------|---------------
SHUT_DOWN | `openstackHandler.shutdownServer(getHostname())`
RESTARTED | `openstackHandler.rebootServer(getHostname())`
FORCE_RESTARTED | `openstackHandler.rebootServerWithForce(getHostname())`

### K8sLpgNode.java
Action | Implementation
-------|---------------
SHUT_DOWN | `ipmitool.shutdownNode(ipAddress)`
RESTARTED | `ipmitool.restartNode(ipAddress)`
FORCE_RESTARTED | `ipmitool.restartNodeWithForce(ipAddress)`

## PCC Test Framework - All performAction implementations

| File | Lines | Cluster Type | Actions Supported |
|------|-------|--------------|-------------------|
| K8sBaremetalNode.java | 130-172 | Baremetal | All (RESTARTED, FORCE_RESTARTED, SHUT_DOWN, KERNEL_PANICKED, DRAINED, CORDONED, UNCORDONED) |
| K8sMesosNode.java | 84-126 | Mesos | SHUT_DOWN, RESTARTED, FORCE_RESTARTED, DESTROYED, KERNEL_PANICKED, DRAINED, CORDONED, UNCORDONED |
| K8sOpenshiftNode.java | 163-190 | Openshift | KERNEL_PANICKED only |
| NexusNode.java | 85-116 | Nexus/K8s | RESTARTED, FORCE_RESTARTED, CORDONED, UNCORDONED |
| GdceNode.java | 73-113 | GDCE | RESTARTED, FORCE_RESTARTED, CORDONED, UNCORDONED, DRAINED |
| K8sCeeNode.java | 141-192 | CEE/OpenStack | All except DESTROYED/POWER_STATUS |
| K8sLpgNode.java | 52-74 | LPG | STARTED, RESTARTED, FORCE_RESTARTED, SHUT_DOWN, KERNEL_PANICKED, POWER_STATUS |

## NodeAction Enum Workflow

```
Gherkin Step → K8sNodeSteps → NodeAction enum → K8sNodeApi.performAction() → Platform-specific implementation
```

## Key Patterns

### Kernel Panic Sequence
1. Add connection decorator with timeout
2. Send async command to `/proc/sysrq-trigger`
3. Wait for connection loss (verifies panic happened)
4. Remove decorator
5. Sleep until timeout
6. Reconnect and verify