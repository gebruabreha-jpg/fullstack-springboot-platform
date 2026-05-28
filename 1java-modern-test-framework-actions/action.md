# Fault Injection & Deployment Actions Catalog
## PCC Test Framework + Beets Framework
## POD KILL / CONTAINER KILL ACTIONS
### PCC Test Framework
| Step Definition | File | Method |
|-----------------|------|--------|
| `a container (that )controlled by resource is killed` | K8sContainerSteps.java | Kills container by resource |
| `a container controlled by pod that matches name pattern is killed` | K8sContainerSteps.java | Kills container by pod pattern |
| `container is killed in database pod(s)` | KvdbSteps.java | Kills container in KVDB pod |
| `the process {string} is killed with signal {int} on a pod with label` | K8sPodManagementSteps.java:526 | Kills process on pod |
| `single Pod {string} is restarted ungracefully by main container` | K8sPodManagementSteps.java | Restarts pod by killing main container |

### Beets Framework
| Step Definition | File | Method |
|-----------------|------|--------|
| `the container {string} controlled by home-pod of {string} VPN is killed` | PcgStepDefinition.java:1686 | Kills VPN container |
| `cre pod reboot one by one use method kill container` | UpfCreIntegrationSteps.java:1930 | Kill container method |
| `cre pod reboot one by one use method kubectl delete pod` | UpfCreIntegrationSteps.java:1932 | Kubectl delete pod |

---

## NODE FAILURE ACTIONS

### PCC Test Framework
**NodeAction Enum Values:**
- `RESTARTED` - Graceful node restart
- `FORCE_RESTARTED` - Force restart via VM power commands
- `KERNEL_PANICKED` - Kernel panic injection (K8sNode.java:875-893)
- `DESTROYED` - Node destruction
- `SHUT_DOWN` - Node shutdown (K8sNode.java:905-921)
- `DRAINED` - Drain node of pods
- `CORDED` - Cordon node (prevent scheduling)
- `UNCORDED` - Uncordon node (enable scheduling)

**Step Definitions:**
| Step | File | Line |
|------|------|------|
| `a worker node with at least one of the following running pods is {node_action}` | K8sNodeSteps.java | 385 |
| `a node with name matching pattern {string} is {node_action}` | K8sNodeSteps.java | 604 |
| `a {node_type} node is {node_action}` | K8sNodeSteps.java | 467 |
| `a memorized worker node {string} is drained{time_limit_s} and restarted` | K8sNodeSteps.java | - |
| `a memorized worker node {string} is drained{time_limit_s}( and ){node_action}` | K8sNodeSteps.java | - |

### Beets Framework
| Step Definition | File | Line |
|-----------------|------|------|
| `a random DCGW is restarted within {int} {time_unit}` | DcgwStepDefinition.java | 569 |

---

## NETWORK LOSS ACTIONS

### PCC Test Framework - Litmus Experiments
| Enum Value | Description |
|------------|-------------|
| `POD_NETWORK_LOSS` | pod_network_loss - packet loss percentage |
| `POD_NETWORK_PARTITION` | pod_network_partition - network isolation |
| `POD_NETWORK_LATENCY` | pod_network_latency - network delay |
| `POD_NETWORK_CORRUPTION` | pod_network_corruption - packet corruption |
| `POD_IO_STRESS` | pod_io_stress - disk I/O stress |
| `POD_CPU_HOG` | pod_cpu_hog - CPU resource exhaustion |
| `POD_MEMORY_HOG` | pod_memory_hog - Memory resource exhaustion |
| `CONTAINER_KILL` | container_kill - Kill specific container |
| `KUBELET_SERVICE_KILL` | kubelet_service_kill - Kill kubelet service |
| `DOCKER_SERVICE_KILL` | docker_service_kill - Kill docker service |

### PCC Test Framework - Node Interface Control
| Step Definition | File | Execution |
|-----------------|------|-----------|
| `bring down network interface {string} in {int} random {node_type} node(s)` | K8sNodeSteps.java | tc commands (K8sNode.java:296-304) |
| `concurrently bring down network interface(s) on the following nodes` | K8sNodeSteps.java | Parallel interface control |
| `drop all packets on given interfaces for given duration on the following random nodes` | K8sNodeSteps.java | tc traffic control (K8sNode.java:332-343) |

### Beets Framework
| Step Definition | File |
|-----------------|------|
| `L3 interface(s) of random DCGW is/are disabled for {int} seconds in parallel` | - |
| `DCGW shutdown is simulated by disabling all interfaces towards SUT` | - |
| `DCGW start is simulated by enabling all interfaces towards SUT` | - |

---

## HELM CHART DEPLOYMENT ACTIONS

### PCC Test Framework (HelmApi.java)
| Method | Description |
|--------|-------------|
| `installHelmChart(chart, releaseName, namespace, values, version)` | Full install with values |
| `upgradeHelmChart(releaseName, chart, namespace, values, ...)` | Helm upgrade |
| `deleteChart(releaseName, namespace)` | Helm uninstall |
| `rollbackToProvidedRevision(releaseName, revision, namespace)` | Helm rollback |

### PCC Step Definitions
| Step | File |
|------|------|
| `helm chart is upgraded with additional set flags` | - |
| Helm install/upgrade with pod restart coordination | - |

### Beets Framework
| Step Definition | File | Line |
|-----------------|------|------|
| `the Helm chart is installed in namespace {string} with the following parameters` | DeploymentSteps.java | 1679 |
| `the Helm chart is removed and redeployed` | DeploymentSteps.java | 634 |
| `saved helm chart is reinstalled` | DeploymentSteps.java | 678 |
| `Helm chart is {helm_action} while {int} pods from the following prefixes are restarted` | - |

---

## POD RESTART CONTROL ACTIONS

### PCC Test Framework
| Step Definition | File | Line |
|-----------------|------|------|
| `pods with prefix are {pod_action}` | K8sPodManagementSteps.java | 154 |
| `a pod with prefix {string} is {pod_action}` | K8sPodManagementSteps.java | - |
| `pods controlled by resource are {pod_action}` | K8sPodManagementSteps.java | 321 |
| `Pod {string} is restarted ungracefully by main container {string}` | K8sPodManagementSteps.java | - |
| `for every KVDB pods pair, master kvdb is {pod_action} and one CRE pod is {pod_action}` | K8sPodManagementSteps.java | - |

### Beets Framework
| Step Definition | File |
|-----------------|------|
| `the following pods are {pod_action}{cluster_selector}` | Multiple step def files |

---

## CHAOS EXPERIMENTS (Litmus)

| Experiment | Purpose |
|------------|---------|
| `pod_cpu_hog` | CPU resource exhaustion |
| `pod_memory_hog` | Memory resource exhaustion |
| `container_kill` | Kill specific container |
| `pod_network_loss` | Packet loss injection |
| `pod_network_partition` | Network isolation |
| `pod_network_latency` | Network delay |
| `pod_network_corruption` | Packet corruption |
| `pod_io_stress` | Disk I/O stress |
| `kubelet_service_kill` | Kill kubelet |
| `docker_service_kill` | Kill docker daemon |

---

## ADDITIONAL FAULT INJECTION ACTIONS

### Traffic Generators
| Action | Description |
|--------|-------------|
| `TRex is started/stopped` | Traffic generation via TRex |
| `Xperf is stopped` | Traffic model control |
| `Gladiator injects traffic` | Traffic injection via Gladiator |

### Simulator Actions (Dallas)
| Action | Description |
|--------|-------------|
| `Dallas is stopped/stopped within {int} seconds` | Simulator control |
| `Dallas host is reinstalled` | Host reinstall |
| `Dallas upfsim is restarted` | UPF simulator restart |
| `Dallas SGW-C sims are restarted` | SGW simulator restart |

### Network Functions
| Action | Description |
|--------|-------------|
| `TOR {string} is restarted` | TOR restart |
| `a MSC_VLR is restarted` | MSC/VLR restart |
| `an HssUdm is restarted {graceful_or_ungraceful}` | HSS/UDM restart |
| `AMF sim is {binary_condition}` | AMF simulator control |
| `NRF is {binary_condition}` | NRF simulator control |
| `SMF is restarted {graceful_or_ungraceful}` | SMF restart |
| `SGW is restarted` | SGW restart |
| `SMSF is restarted {graceful_or_ungraceful}` | SMSF restart |

---

## ACTION COUNTS
- **PCC Framework:** 542 unique step definitions
- **Beets Framework:** 727 unique step definitions
- **Total:** 1,269+ actionable fault injection patterns

## Shared Capabilities
Both frameworks support:
- Pod kill/restart (kubectl delete, container kill, process kill)
- Node failure (restart, shutdown, kernel panic, destroy)
- Network chaos (Litmus experiments, tc traffic control)
- Helm deployments (install, upgrade, rollback, delete)
- Interface control (bring down/up, drop packets)