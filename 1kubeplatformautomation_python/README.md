# 1kubeplatformautomation and test automation
A Python project for Kubernetes automation and robustness testing (KubePlatformAutomation).
## Goal
Build a **Kubernetes pod testing automation tool** with:
- **15 step classes** covering deployment, health, logs, metrics, and validation
- **~100 individual test steps** for comprehensive automation
- **BDD-style testing** using Behave (Python equivalent of Cucumber from Beets project)
## Quick Start
```bash
# Install dependencies
pip install -r requirements.txt
# Run tests (mock mode without cluster)
behave
# With Kubernetes cluster (requires Docker)
./infra/setup.sh
behave
```
## Architecture
See [ARCHITECTURE.md](ARCHITECTURE.md) for tech stack details and API mapping from Beets.
## Overview
This project implements a comprehensive robustness/chaos testing framework for Kubernetes-based telecom workloads, inspired by the Ericsson Beets/PCC test automation system. It provides automated failure injection and verification mechanisms to test system resilience.
## Core Capabilities
### 1. Container Kill (SIGKILL)
- **Implementation**: Uses Kubernetes API to get container ID and node name, then SSH to node to send SIGKILL
- **Step**: `When a container that controlled by resource is killed`
- **Low-level**: `k8sNodeApi.killContainers(containerIds, KillSignal.SIGKILL)`
### 2. Pod Delete (kubectl delete)
- **Implementation**: Uses Kubernetes API directly for reliable pod deletion
- **Step**: `When pods controlled by resource are deleted`
- **Low-level**: `kubectlApi.deleteResource(KubectlResourceType.PODS, podName)`
### 3. Worker Node Failure
- **Graceful Restart**: SSH reboot or OpenStack API (`openstack server reboot`)
- **Ungraceful (Kernel Panic)**: SSH to node → `echo c > /proc/sysrq-trigger`
-**Step**: `When a worker node with a minority of KVDB master pods is kernel panicked`
### 4. Control-Plane (Master) Node Failure
- **Graceful**: OpenStack API reboot
- **Ungraceful**: Same kernel panic mechanism via SSH
- **Step**: `When a control-plane node is kernel panicked`
### 5. Transport Disconnect (Network Loss between GeoRed nodes)
- **Implementation**: Change ICR peer port via ADP CM CLI (`epg node icr peer port 50099`)
- **Step**: `When transport between Geored nodes is disconnected by changing peer port to "50099"`
- **Reconnection**: Restore original peer port
### 6. Litmus Chaos Experiments
- **Framework**: Applies RBAC, experiment CRDs, and engine CRDs via kubectl
- **Experiments**: CONTAINER_KILL, POD_CPU_HOG, POD_NETWORK_PARTITION
- **Step**: `When the litmus experiment {} is run`
### 7. Packet Loss Verification
- **Verification**: Check Prometheus metrics for packet loss thresholds
- **Step**: `Given the packet loss is less than: 5 packets per million loss`
## Test Flow Pattern
Every robustness scenario follows this end-to-end pattern:
### Phase 1: PRE-CONDITION (Setup/Background)
- Traffic started (Dallas simulator generates signaling + user plane traffic)
- Given Dallas is started
- And traffic model on all MS groups has been issued with percentage 50.0
- Then the Dallas busy-hour is reached with parameters:
    | Tolerance | MaxFailed | Timeout | Poll Interval | Critical |
    | 3500      | 10000     | 3600    | 60            | true     |
### Phase 2: SAVE BASELINE (Start of each scenario)
- When the container restart count values are saved as 'container_restart_counters'
- And memorized event "failure_action_started" is saved
- And active session data is saved in checkpoint 'sessions_pre_failure'

### Phase 3: FAILURE ACTION (The actual kill/disruption)
- **Container Kill**:
  - When a container that controlled by resource is killed:
    | Pod Resource          | Container  | Kill Signal | Number of pods | Role   |
    | eric-pc-sm-controller | controller | sigkill     | 1              | active |
- **Pod Delete**:
  - When pods controlled by 3 resource are deleted randomly:
    | Resource                   | Role    | Max random resource |
    | eric-pc-sm-controller      | active  | 1                   |
    | eric-pc-sm-smf-pgw-session |         | 7                   |
- **Worker Node Failure**:
  - Graceful restart: When a memorized worker node "memorized-worker" is restarted
  - When a worker node with a minority of KVDB master pods is restarted
  - Ungraceful (kernel panic): When a worker node with a minority of KVDB master pods is kernel panicked
- **Control-Plane Failure**:
  - Graceful: When a control-plane node is restarted
  - Ungraceful: When a control-plane node is kernel panicked
- **Transport Disconnect**:
  - When transport between Geored nodes is disconnected by changing peer port to "50099"
  - # ... wait ...
  - When transport between Geored nodes is reconnected by restoring peer port number
### Phase 4: WAIT FOR RECOVERY
- And 5 minutes have passed
- # or
- And 10 minutes have passed
### Phase 5: VERIFICATION (Post-failure checks)
1. **Signaling success rate**:
   - Then total signaling success rate is above 99.999% for the past 6 minutes
2. **No unexpected container restarts**:
   - And the container restart count values have not increased since they were saved as "container_restart_counters" except:
     | Pod Prefix            | Container  | Expected Restarts |
     | eric-pc-sm-controller | controller | 1                 |
3. **No unexpected alarms**:
   - Then no alarms have been active since memorized event "failure_action_started" except:
     | verdict | alarm              | details | severity |
     | ignore  | SM DB Unavailable  | ...     | N/A      |
4. **Correct ISP (In-Service Performance) events occurred**:
   - Then the following ISP events have occurred exactly 1 time since memorized event "failure_action_started":
     | event_type.keyword | regexp:message.keyword                                    |
     | container-failed   | The container controller in pod eric-pc-sm-controller.* failed... |
     | container-started  | The container controller in pod eric-pc-sm-controller.* started.  |
5. **Recovery time check**:
   - And verify memorized date-times satisfy the following requirements:
     | label regexp                 | corresponding label          | time difference      | requirement |
     | sm_controller_container_kill | sm_controller_pod_is_serving | less than 20 seconds | REQ18835    |
6. **Packet loss check (PCG)**:
   - Given the packet loss is less than:
     | Max packet per million loss | UL/DL/ULDL | From memorized event | To memorized event/now |
     | 5                           | UL         | After_Failure_Event1 | now                    |

7. **No unexpected ISP events after recovery**:
   - Then the following ISP events have occurred exactly 0 times:
     | range_gt:timestamp |
     | node_recovered     |
## Technical Implementation
### Core Libraries Used
- **Kubernetes**: fabric8 kubernetes-client (Java) / kubernetes (Python)
- **SSH**: JSch / Apache MINA SSHD (Java) / Paramiko (Python)
- **Prometheus**: HTTP API client for metrics queries
- **Elasticsearch/OpenSearch**: For log/event/alarm queries (ISP events)
- **NETCONF**: Custom client for configuration management
- **Helm**: Java client or shell CLI for install/upgrade/rollback
- **Chaos Engineering**: LitmusChaos CRDs applied via kubectl
- **Traffic Simulation**: Custom Dallas traffic simulator client
- **Certificate Management**: Bouncy Castle / EJBCA for PKI operations
- **Cloud Providers**: OpenStack4j / boto3 for VM operations
- **Test Framework**: Cucumber + TestNG + Guice (Java) or pytest (Python)

### Action Execution Flow
**Container Kill**:
1. Resolve pod name from resource (StatefulSet/Deployment) using KubectlApi
2. Get container ID: `kubectlApi.getContainerId(podName, containerName)`
3. Get node hosting pod: `kubectlApi.getNodeName(podName)`
4. SSH to node and kill: `k8sNodeApi.killContainers(containerIds, KillSignal.SIGKILL)`

**Pod Delete**:
1. `kubectlApi.deleteResource(KubectlResourceType.PODS, podName)` (equivalent to `kubectl delete pod`)
**Node Kernel Panic**:
1. SSH to node → execute `echo c > /proc/sysrq-trigger`
**Transport Disconnect**:
1. Execute CLI: `epg node icr peer port 50099` via ADP CM yang provider
2. To reconnect: restore original peer port
**Litmus Chaos**:
1. Apply RBAC (service account)
2. Create experiment CRD
3. Start chaos engine

### Java Step Definitions (from pcc-framework library)

The actual step implementations live in the pcc-framework dependency (com.ericsson.pcc:pcc-framework). Here's how each action works:

**1. Container Kill — K8sContainerSteps.java**
- Step annotations:
  - `@When("a container (that )controlled by resource is killed{cluster_selector}:")`
  - `@When("a container is killed{cluster_selector}:")`
  - `@When("container(s) controlled by {int} resource(s) is/are killed randomly{cluster_selector}:")`
- Flow:
  1. Reads the data table (Pod Resource, Container, Kill Signal, Number of pods, Role)
  2. Resolves the pod name from the resource (StatefulSet/Deployment) using KubectlApi
  3. Gets the container ID: `kubectlApi.getContainerId(podName, containerName)`
  4. Gets the node hosting the pod: `kubectlApi.getNodeName(podName)`
  5. SSHs into the node and kills the container process: `k8sNodeApi.killContainers(containerIds, KillSignal.SIGKILL)`

**2. Pod Delete — K8sPodManagementSteps.java**
- Step annotations:
  - `@When("a pod with prefix {string} is {pod_action}{cluster_selector}")`
  - `@When("pods controlled by resource are {pod_action}{cluster_selector}:")`
  - `@When("pod(s) controlled by {int} resource(s) is/are deleted randomly{cluster_selector}:")`
- PodAction enum values:
  - DELETED — calls `kubectlApi.deleteResource(KubectlResourceType.PODS, podName)` (equivalent to `kubectl delete pod`)
  - FORCE_DELETED — force delete with grace period 0
  - RESTARTED — calls k8sNodeApis to restart the pod

**3. Node Restart/Kill — K8sNodeSteps.java**
- Step annotations:
  - `@When("a worker node with/hosting the following running pods is {node_action}:")`
  - `@When("a worker node with at least one of the following running pods is {node_action}{cluster_selector}:")`
  - `@When("a worker node with a minority of KVDB master pods is {node_action}{cluster_selector}")`
  - `@When("a {node_type} node is {node_action}")`
  - `@When("a memorized worker node {string} is {node_action}{cluster_selector}")`
- NodeAction enum values and what they do:
  - RESTARTED — Graceful reboot via OpenStack API (`openstack server reboot`) or SSH reboot
  - FORCE_RESTARTED — Hard reboot via OpenStack (`openstack server reboot --hard`)
  - KERNEL_PANICKED — SSH into node → `echo c > /proc/sysrq-trigger`
  - KERNEL_PANICKED_WITHOUT_RECONNECT — Same but doesn't wait for reconnection
  - SHUT_DOWN — Shuts down the VM/node
  - DESTROYED — Destroys the VM
  - DRAINED — `kubectl drain <node>`
  - CORDONED — `kubectl cordon <node>`
  - UNCORDONED — `kubectl uncordon <node>`
- All executed via `k8sNodeApi.performAction(NodeAction, args...)` which SSHs into the node or uses the cloud API.

**4. Network Interface Manipulation — K8sNodeApi interface**
- Bring down a network interface on a node: `k8sNodeApi.bringDownInterface(String interfaceName)`
- Bring up a network interface on a node: `k8sNodeApi.bringUpInterface(String interfaceName)`
- Bring down and up interface: `k8sNodeApi.bringDownAndUpInterface(String interfaceName, int durationSeconds)`
- Drop all packets on interfaces for a duration (creates packet loss): `k8sNodeApi.dropAllPacketsOnInterfaces(Set<String> interfaces, Duration duration, long delayMs)`

**5. Transport Disconnect — GeoRedStepDefinition.java (in beets)**
```java
@Then("transport between Geored nodes is disconnected by changing peer port to {string}")
public void transportBetweenGeoredNodesIsDisconnectedByChangingPeerPortNumber(String portNumber) {
    String activeCluster = clusterRegistry.getActiveCluster();
    String currentIcrPeerPort = mappedAdpCmControllers.get(activeCluster).get()
            .getIcrPeerPort(activeCluster);
    // Save current port for later restoration
    scenarioRegistryClusterMap.get(activeCluster).put(ICR_PEER_PORT_ACTIVE_CLUSTER, currentIcrPeerPort);
    // Set wrong port to break transport
    mappedAdpCmControllers.get(activeCluster).get().setIcrPeerPort(portNumber, activeCluster);
}
```
This executes CLI command: `epg node icr peer port 50099` via the ADP CM yang provider.

**6. Litmus Chaos — LitmusExperimentSteps.java (in beets)**
```java
@When("the litmus experiment {} is run{cluster_selector}")
public void runLitmusExperiments(LitmusExperiments experiment, ClustersToExecute clustersToExecute) {
    String cluster = clustersToExecute.getCurrentCluster();
    LitmusExperimentsActions litmusActions = mappedLitmusExperimentsActions.get(cluster);
    // 1. Apply RBAC (service account)
    litmusActions.applyRbac(rbacFile, experiment.getExperimentName());
    // 2. Create experiment CRD
    litmusActions.createExperiment(experimentFile, experiment.getExperimentName());
    // 3. Start chaos engine
    litmusActions.startEngine(engineFile, experiment.getExperimentName());
}
```
Experiments include: CONTAINER_KILL, POD_CPU_HOG, POD_NETWORK_PARTITION, kubelet-service-kill-chaos, docker-service-kill-chaos.

## Usage

This framework can be used to test:
- **Robustness**: System behavior under failure conditions
- **Stability**: Long-running system behavior
- **Performance**: Under load and stress conditions
- **Upgrade/Rollback**: Helm chart lifecycle operations
- **Security**: Vulnerability scanning and compliance
- **Capacity**: Scaling limits and resource utilization

## Extending the Framework

To add new failure scenarios:
1. Implement new action in appropriate action module (pod_actions.py, node_actions.py, etc.)
2. Add verification checks in checks module
3. Define scenario in YAML format under config/scenarios/
4. Register new action/check in plugin system
5. Run via pytest: `python -m pytest tests/test_robustness.py`

## Example Scenario (YAML)

```yaml
scenarios:
  - name: "Container Kill - SMF Pod"
    tags: [robustness, pod-kill]
    pre_checks:
      - type: all_pods_ready
      - type: save_restart_counts
        save_as: "baseline"
    action:
      type: kill_container
      params:
        resource: "eric-pc-sm-smf-pgw-session"
        container: "smf"
        signal: SIGKILL
        count: 1
    wait: 300  # seconds
    post_checks:
      - type: pods_ready
        timeout: 300
      - type: restart_count_increased
        baseline: "baseline"
        expected:
          eric-pc-sm-smf-pgw-session: 1
      - type: prometheus_query
        query: "rate(signaling_success_total[5m])"
        condition: "> 0.9999"
      - type: no_alarms_since
        except: ["Diagnostic Data Collection Output Failed"]
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
Install Dependencies:
cd "C:\Users\ewolgeb\gebru\office-weekend\1kubeplatformautomation_python"
pip install -r requirements.txt


Install Required Tools (using the provided script):
.\infra\script\install_tools.ps1
This will install:-
kubectl
Go
Docker (manual installation required)
kind (Kubernetes IN Docker)
KWOK (Kubernetes Without Kubelet)
Restart PowerShell after installation to refresh PATH for kwokctl

deploy the app:
Deploy/Create Environment .\infra\script\setup_kwok.sh


Running the Tests:-
Option 1: Mock Mode (No Actual Cluster Required)
cd "C:\Users\ewolgeb\gebru\office-weekend\1kubeplatformautomation_python"
behave
behave features/1.feature
behave --tags=robustness


Why line-by-line, not feature-by-feature:
The step files (steps/) depend on utils/ and infra conventions — review them together before indenting principles

1.py is the orchestrator (_pip_install, _find_gobin, _detect_shell, _run_infra_script, _run_behave); bugs there cascade everywhere

infra scripts and configs (infra/script/, infra/config/) are the foundation everything else builds on

Feature files describe tests that call step definitions — they can only be meaningfully reviewed after all step files are understood