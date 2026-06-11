This section documents Pod/Container Kill Actions - fault injection mechanisms for testing pod resilience:-
PCC Framework actions execute:
Kill containers by resource name or pod label pattern
Kill specific processes inside pods with configurable signals (SIGTERM, SIGKILL, etc.)
Restart pods ungracefully by killing their main container

Beets Framework actions execute:
Kill VPN-related containers on specific pods
Reboot CRE pods using either container-kill or kubectl delete methods
Both frameworks support graceful vs force deletion and container-level precision targeting for chaos testing.


The pod/container kill actions are performed by:

Primary Implementation Files:
K8sContainerHelper.java (lines 64-228)
Pod/Container Kill Actions:

killProcessEntity() - Kills process entities inside pods using configurable signals (SIGTERM, SIGKILL, etc.)
killContainer() - Kills containers by resource name/prefix with specified signals
Uses KillRequest objects containing pod name, container name, container ID, and signal
Executes kills via K8sNodeApi.killProcessEntities() or K8sNodeApi.killContainers()
K8sPodManagementSteps.java (lines 153-169)
Pod Actions:

runActionOnPodWithPrefix() - Executes PodAction (deleted/restarted) on pods matching prefix
Uses PodAction enum (DELETED, FORCE_DELETED, RESTARTED) from PodAction.java
Calls K8sPodPerformer.runActionOnPod() which invokes PodAction.execute()
KvdbSteps.java (lines 767-950)
KVDB Fault Injection:

killContainerInSelectedKvdbPodsWithOutRoleCheck() - Kills containers in KVDB pods
sequentiallyKillPairedContainersInSelectedKvdbPodsWithOutRoleCheck() - Sequential kills with delay
Uses KillSignal enum for configurable signal handling
Key Execution Flow:
Test step (e.g., a container is killed) calls helper method
Helper creates KillRequest with pod/container ID and signal
K8sNodeApi executes killContainers() via shell commands on worker node
Signal passed to killProcessEntities() for process-level killing






Based on the code analysis, here's the step-by-step execution flow for Pod/Container Kill Actions:-
PCC Framework Actions
1. killProcessEntity() - K8sContainerHelper.java:64-111
Step 1: Verify table headers (pod header, container, kill signal, nr_of_pods, PE, nr_of_entities)
Step 2: Iterate over each row in the DataTableWrapper
Step 3: For each row:
  - Extract pod prefix, container name, kill signal (SIGTERM/SIGKILL), process entity
  - Get pod role if applicable (for ERIC_PC_SM_CONTROLLER)
  - Find pods matching filter using K8sPodRetriever.getPodsMatchingFilter()
Step 4: For each matching pod:
  - Get container ID via clusterKubectlApi.getContainerId(podName, container)
  - Get node name via clusterKubectlApi.getNodeName(podName)
  - Get K8sNodeApi for the node
  - Call k8sNodeApi.killProcessEntities(containerId, killSignal, processEntity, numberOfEntities)
2. killContainer() - K8sContainerHelper.java:129-203
Step 1: Verify table headers (pod header, container, kill signal, nr_of_pods, optional: role, namespace)
Step 2: Build nodeKillRequestsMap via createNodeKillRequestsMap():
  - For each row:
    - Extract namespace (optional)
    - Validate kill signal against KillSignal enum
    - Get pod list matching prefix
    - For each pod:
      - Get container ID
      - Get node name
      - Create KillRequest(podName, containerName, containerId, killSignal)
Step 3: Execute kills in parallel via ParallelExecutionUtil:
  - Each node gets list of KillRequest objects
  - k8sNodeApi.killContainers(killRequests) is called
3. Pod RESTARTED via PodAction - PodAction.java:65-92
Step 1: Synchronized block (KubeCtl.class):
  - Get all containers in pod
  - Create KillRequest for each container with SIGKILL signal
  - Get pod node name
Step 2: Call k8sNodeApi.killContainers(killRequests) - kills all containers triggering restart
Step 3: Poll until restart completes (container restart timeout)
Beets Framework Actions
4. killContainerInVPNHomePod() - PcgStepDefinition.java:3542-3548
Step 1: Get home pod name via CLI command: show ipsec vpns vpn [vpn-name] tunnel-distribution
Step 2: Call ActionsHelper.killContainerInPod(homePod, container, kubectlApi, k8sNodeApis)
5. ActionsHelper.killContainerInPod() - ActionsHelper.java:1286-1295
Step 1: Set signal to SIGKILL
Step 2: Get container ID via kubectlApi.getContainerId(podName, containerName)
Step 3: Get node name via kubectlApi.getNodeName(podName)
Step 4: Get K8sNodeApi for node
Step 5: Call k8sNodeApi.killContainers(containerIds, signal)
6. killContainerInCrePod() - UpfCreIntegrationSteps.java:87-107
Step 1: Get all CRE pods matching pattern
Step 2: For each pod:
  - Get first container
  - Call ActionsHelper.killContainerInPod(pod, container, kubectlApi, k8sNodeApis)
  - Wait for resources to come up (max 12 loops)
  - Sleep 60 seconds between iterations
7. KVDB Container Kill - KvdbSteps.java:1489-1512 / 767-788
Step 1: killContainerInSelectedKvdbPodsWithOutRoleCheck() (line 767-788):
  - Parse table for KVDB selection, container, signal, number of pods
  - Find KVDB servers via subStepFindKvdbServers()
  - Select servers via subStepSelectKvdbServers()
  - Call subStepKillContainerOnKvdbServers()
  - Wait KVDB_RESTART_WAIT_TIME_SECONDS
  - Verify replicas and pod count

Step 2: sequentiallyKillPairedContainersInSelectedKvdbPodsWithOutRoleCheck() (line 821-854):
  - Select master/replica pairs
  - Kill master containers first
  - Wait specified delay
  - Kill replica containers
  - Verify KVDB roles after action
Key Execution Flow Summary
Test Step → Helper Method → KillRequest Objects → K8sNodeApi.killContainers()/killProcessEntities() → Shell commands on worker node