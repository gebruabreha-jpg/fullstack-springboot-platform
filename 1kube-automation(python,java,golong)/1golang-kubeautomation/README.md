# Modern Test Framework Actions — Platform Engineering Guide
```
1java-modern-test-framework-actions/
├── Pod-Container/
│   ├── CONTAINER KILL ACTIONS.java    # Standalone Java reference for container/
│   ├── CONTAINER KILL ACTIONS.md      # Detailed documentation
│   ├── 1.java                         # Full implementation walkthrough
│   └── 1.feature                      # Gherkin scenarios
├── node/
│   ├── node_failure.java               # Standalone Java reference for node
│   ├── node_failure.md                # Detailed documentation
│   ├── 2.java                         # Full implementation walkthrough
│   └── 2.feature                      # Gherkin scenarios
├── network-partition-loss/
│   ├── NETWORK LOSS.java              # Standalone Java reference for network
│   ├── NETWORK LOSS.md                # Detailed documentation
│   ├── 3.java                         # Full implementation walkthrough
│   └── 3.feature                      # Gherkin scenarios
├── pod-restart/
│   ├── POD RESTART.java               # Standalone Java reference for pod
│   ├── POD RESTART.md                 # Detailed documentation
│   ├── 1.java                         # Full implementation walkthrough
│   └── 2.feature                      # Gherkin scenarios
├── helm/
│   ├── Helm deployments.java          # Standalone Java reference for Helm
│   ├── Helm deployments.md            # Detailed documentation
│   ├── 1.java                         # Full implementation walkthrough
│   └── 1.feature                      # Gherkin scenarios
├── storage-volume/
│   ├── STORAGE VOLUME OPERATIONS.md   # Detailed documentation
│   ├── 4.java                         # Full implementation walkthrough
│   └── 4.feature                      # Gherkin scenarios
├── control-plane/
│   ├── CONTROL PLANE OPERATIONS.md    # Detailed documentation
│   ├── 5.java                         # Full implementation walkthrough
│   └── 5.feature                      # Gherkin scenarios
├── verification-health-checks/
│   ├── VERIFICATION HEALTH CHECKS.md  # Detailed documentation
│   ├── 10.java                        # Full implementation walkthrough
│   └── 10.feature                     # Gherkin scenarios
├── observability-debugging/
│   ├── OBSERVABILITY DEBUGGING.md     # Detailed documentation
│   ├── 12.java                        # Full implementation walkthrough
│   └── 12.feature                     # Gherkin scenarios
├── security-access/
│   ├── SECURITY ACCESS.md             # Detailed documentation
│   ├── 8.java                         # Full implementation walkthrough
│   └── 8.feature                      # Gherkin scenarios
├── resource-exhaustion/
│   ├── RESOURCE EXHAUSTION.md         # Detailed documentation
│   ├── 9.java                         # Full implementation walkthrough
│   └── 9.feature                      # Gherkin scenarios
├── Litmus/
│   ├── 1.java                         # Litmus chaos experiment implementation
│   └── 1.feature                      # Gherkin scenarios
├── action-chaining/
│   ├── 8.md                          # Action chaining documentation
│   ├── 8.java                        # Full implementation walkthrough
│   └── 8.feature                     # Gherkin scenarios
├── cleanup-recovery/
│   ├── 9.md                          # Cleanup/recovery documentation
│   ├── 9.java                        # Full implementation walkthrough
│   └── 9.feature                     # Gherkin scenarios
├── README.md                          # This file
```
## Kubernetes Automation Patterns
### Workload Lifecycle Automation
- **Deployments/ReplicaSets**: Auto-create and maintain desired Pod count
- **Self-healing**: Failed Pods automatically restarted or replaced
- **Auto-scaling**: HPA scales based on CPU/memory/custom metrics
- **Rolling updates/deployment automation**: Zero-downtime updates via gradual Pod replacement

Container → restart
Pod → replace
Node → reschedule
Network → retry

Container   → kubelet
Pod         → ReplicaSet / Deployment
Node        → Node Controller + Scheduler
Network     → CoreDNS + kube-proxy + app/service mesh
Storage     → kubelet + PV controller + CSI drivers
Control plane → no higher healer (degraded cluster state)                              Traffic changes → Endpoints Controller + kube-proxy
Scaling needed  → HPA / Cluster Autoscaler

🧱 1. Node Lifecycle Operations (Kubernetes + Infra)
kubectlApi.cordonNode(node)
kubectlApi.uncordonNode(node)
kubectlApi.drainNode(node)
kubectlApi.getNodeStatus(node)
kubectlApi.describeNode(node)
Infra-level equivalents:
ipmi.powerOff(node)
ipmi.powerOn(node)
ipmi.reboot(node)
systemctl restart kubelet

📦 2. Pod / Container Fault Injection
kubectl delete pod <pod>
kubectlApi.killPod(prefix)
kubectlApi.killContainer(pod, container)
kubectlApi.restartPod(pod)
kubectlApi.getPodStatus(pod)
kubectlApi.execInContainer(pod, cmd)
Signal-based injection:
kill -9 <pid>
kill -15 <pid>

🌐 3. Network Chaos Injection
tc qdisc add dev <iface> root netem loss 10%
tc qdisc add dev <iface> root netem delay 200ms
tc qdisc add dev <iface> root netem corrupt 5%
tc qdisc add dev <iface> root netem duplicate 5%
tc qdisc change dev <iface> root netem delay 100ms 20ms
tc qdisc del dev <iface> root netem
Interface control:
ip link set <iface> down
ip link set <iface> up
ip route del default
ip route add default via <gw>
DNS-level:
kubectl delete pod -n kube-system coredns
kubectl rollout restart deployment coredns -n kube-system

💾 4. Storage / Volume Operations
kubectl delete pvc <pvc>
kubectlApi.detachVolume(pod)
kubectlApi.attachVolume(pod)
kubectlApi.checkPVCStatus(pvc)
kubectlApi.getPVStatus(pv)
CSI-level actions:
csi.rescanVolume()
csi.forceDetachVolume()
csi.simulateIOFailure()

⚙️ 5. Control Plane / Cluster Operations
kubectlApi.getAPIServerStatus()
kubectlApi.checkEtcdHealth()
kubectlApi.restartScheduler()
kubectlApi.restartControllerManager()
API failure simulation:
systemctl stop kube-apiserver
systemctl stop etcd
systemctl restart kube-apiserver

🚀 6. Deployment / Helm Operations
helm install <release>
helm upgrade <release>
helm rollback <release>
helm uninstall <release>
Platform wrappers:
helmApi.upgradeWithRestart()
helmApi.rollbackToRevision()
helmApi.validateReleaseHealth()

🧪 7. Litmus Chaos Operations
litmus.runExperiment("pod_delete")
litmus.runExperiment("container_kill")
litmus.runExperiment("pod_cpu_hog")
litmus.runExperiment("pod_memory_hog")
litmus.runExperiment("pod_network_loss")
litmus.runExperiment("pod_network_latency")
litmus.runExperiment("pod_io_stress")
litmus.runExperiment("kubelet_service_kill")
Result tracking:
kubectl get chaosresults
litmus.getExperimentStatus(id)
litmus.getChaosReport()

🔁 8. Action Chaining (Composite Faults)
chaos.chain("kvdb_kill + cre_restart")
chaos.chain("helm_upgrade + pod_restart")
chaos.chain("node_drain + network_loss")
chaos.chain("dcgw_shutdown + iface_recovery")

🧹 9. Cleanup / Recovery Operations (VERY IMPORTANT)
kubectlApi.uncordonNode(node)
kubectlApi.cancelDrain(node)
ip link set <iface> up
tc qdisc del dev <iface> root netem
kubectl delete -f experiment.yaml
kubectl delete -f rbac.yaml
kubectl delete -f chaosengine.yaml
litmus.cleanupExperiment(id)

🔍 10. Verification / Health Checks
kubectlApi.checkPodsReady(prefix)
kubectlApi.waitForPodsRunning(prefix)
kubectlApi.getPodHealth(pod)

kubectl get chaosresults
kubectl describe chaosresult <name>

verifyLinkStatus(iface)
verifyNexthopCount(expected)
verifyServiceEndpoints(service)

kubectlApi.checkServiceAvailability(service)


🖥️ 11. Node & System Verification
kubectlApi.checkNodeReady(node)
kubectlApi.checkNodeNotReady(node)
kubectlApi.checkNodePressure(node)
kubectlApi.getClusterCapacity()



📊 12. Observability / Debugging Hooks
kubectl logs <pod>
kubectl describe pod <pod>
kubectl top pod
kubectl top node

kubectlApi.fetchEvents(namespace)
kubectlApi.getRecentFailures()








Kubernetes + cloud-native testing (chaos engineering / resilience testing). This is the standard “full coverage” set used in real systems:-
🔥 1. Container-Level Failures
These test the smallest execution unit inside a Pod
Container process crash (exit code failure)
OOMKilled (memory limit exceeded)
CPU starvation / throttling
SIGKILL / SIGTERM injection
Infinite loop / hung process
File descriptor exhaustion
Application panic / exception crash
Image pull failure (CrashLoopBackOff at start)
Read-only filesystem violation
📦 2. Pod-Level Failures

These test Kubernetes scheduling + ReplicaSet self-healing

Pod deletion (kubectl delete pod)
Pod eviction (node pressure)
Pod crash loop (CrashLoopBackOff)
Liveness probe failure
Readiness probe failure (traffic removal)
Init container failure
ConfigMap/Secret missing or corrupted
Volume mount failure
DNS resolution failure inside pod
Sidecar container failure (service mesh disruption)
🖥️ 3. Node-Level Failures

These test cluster scheduling + high availability

Node shutdown / restart
Node crash (kernel panic)
Kubelet process failure
Node NotReady state
Disk pressure / full disk
Memory pressure
CPU pressure / overload
Network interface down on node
Node cordon (drain traffic)
Node eviction of all pods
Node replacement in autoscaling group
🌐 4. Network-Level Failures

These test distributed system resilience

Packet loss (e.g., tc netem)
High latency injection
Network partition (split brain)
DNS failure / latency
Service-to-service unreachable
Intermittent connectivity
TCP connection reset
Load balancer failure
API gateway failure
Cross-zone / cross-region communication failure
💾 5. Storage / Data Layer Failures

These test stateful systems

Volume detach/attach failure
PersistentVolume (PV) corruption
PVC binding failure
Slow disk I/O
Database crash (MySQL/Postgres/NoSQL)
Replication lag failure
Data corruption / inconsistent writes
Backup/restore failure
Snapshot failure
⚙️ 6. Control Plane Failures

These test Kubernetes itself

API server downtime
etcd failure or latency
Scheduler failure
Controller manager crash
RBAC misconfiguration
Admission controller blocking requests
Cluster autoscaler failure
Webhook failure (mutating/validating)
🚀 7. Deployment / Release Failures

These test delivery pipelines

Helm upgrade failure
Rolling update stuck
Canary deployment failure
Blue-green switch failure
Version rollback failure
Image tag mismatch
Config drift between environments
🔐 8. Security / Access Failures

These test permission + isolation

Unauthorized API access attempt
RBAC misconfiguration
Secret leakage or missing secrets
Pod running as root violation
Network policy blocking legitimate traffic
Service account token failure
📊 9. Resource Exhaustion Failures

These test scaling limits

CPU saturation
Memory exhaustion
Disk full
Ephemeral storage full
File handle exhaustion
Thread pool exhaustion
Connection pool exhaustion
🧪 10. Chaos Tool / Injected Failures (Litmus / custom scripts)

These are combinations of the above

Container kill (random / targeted)
Pod network loss
Pod CPU hog
Pod memory hog
Node shutdown simulation
Kubelet kill
Docker service kill
Random pod deletion (game day testing)