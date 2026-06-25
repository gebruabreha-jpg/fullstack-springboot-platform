# Verification Health Checks — Detailed Documentation

## Kubernetes Health Verification Patterns

### Pod Health Verification

| Check                        | Method                       | Purpose                                  |
|------------------------------|------------------------------|------------------------------------------|
| `checkPodsReady`             | `kubectl get pods -l <label>`        | Verify pods are in Ready state           |
| `waitForPodsRunning`         | `kubectl wait --for=condition=Ready` | Block until all pods ready       |
| `getPodHealth`               | Describe + check conditions        | Detailed health per container            |
| `checkAllResourcesUp`        | Deployments/STS checks         | All controlled resources running         |

### Service Health Verification

| Check                        | Method                       | Purpose                                  |
|------------------------------|------------------------------|------------------------------------------|
| `checkServiceAvailability`   | `kubectl get endpoints <svc>`         | Service has endpoints                   |
| `verifyServiceEndpoints`     | Count endpoints, check pods     | Endpoints match healthy pods             |
| `verifyNexthopCount`         | `ip route show`                   | Routing table has expected nexthops      |
| `verifyLinkStatus`           | `ip link show <iface>`            | Interface UP/DOWN                        |

### Node Health Verification

| Check                        | Method                       | Purpose                                  |
|------------------------------|------------------------------|------------------------------------------|
| `checkNodeReady`             | `kubectl get node <name>`           | Node in Ready state                      |
| `checkNodeNotReady`          | Node NOT in Ready                  | Verify node failure detection            |
| `checkNodePressure`          | `kubectl describe node`            | MemoryPressure, DiskPressure, PIDPressure|
| `getClusterCapacity`         | `kubectl top nodes`                | CPU/memory utilization across cluster    |

### Chaos Result Verification

| Check                        | Method                       | Purpose                                  |
|------------------------------|------------------------------|------------------------------------------|
| `kubectl get chaosresults`   | List all Litmus results            | Overview of experiment results           |
| `kubectl describe chaosresult` | Detailed verdict                   | Inspect specific experiment outcome      |
| `litmus.getExperimentStatus` | Get status by ID                   | Programmatic result retrieval            |
| `litmus.getChaosReport()`    | Full chaos report                  | Comprehensive summary                    |

### Key Patterns

- **Poll-based verification**: `Poll.isActionCompleted(() -> condition, timeout, interval)`
- **Assert-based verification**: `JcatAssertApi.assertTrue(message, condition)`
- **Parallel verification**: Run checks across multiple nodes/pods simultaneously
- **Timeout handling**: Always set reasonable timeouts for async verification
