Kubernetes:-
🔐 Kubernetes Security (Cloud Security / DevSecOps)
Container Security
Running containers as root (Page 2)
Identity & Access Management (IAM)
Service accounts & RBAC (Page 3–4)
Access Control & Permissions
Cluster roles and bindings
API & Network Security
Exposing dashboards/public APIs (Page 5)

Kubrneets object:-
Pod
The smallest deployable unit in Kubernetes.
A Pod contains one or more containers that share:
•	Network (same IP/port space) 
•	Storage (volumes) 
👉 Use when:
•	You need tightly coupled containers (e.g., sidecar pattern) 
•	Basic execution unit (rarely managed directly in production) 
________________________________________
Deployment
Manages stateless applications using ReplicaSets. Ensures the desired number of identical Pods are running.
👉 Use when:
•	Applications are stateless 
•	Pods can be replaced freely (no identity required) 
•	Examples: web servers, APIs, microservices (e.g., PFCP endpoint) 
✔ Features:
•	Rolling updates & rollbacks 
•	Self-healing (recreates failed Pods) 
________________________________________
StatefulSet
Manages stateful applications with stable identity and persistent storage.
👉 Use when:
•	Each Pod needs a unique identity (e.g., pod-0, pod-1) 
•	Data must persist across restarts 
•	Ordered startup/shutdown is required 
•	Examples: databases (e.g., kvdb), Kafka, ZooKeeper 
✔ Features:
•	Stable network identity 
•	Dedicated Persistent Volume per Pod 
•	Predictable scaling order 
DaemonSet
Ensures one Pod runs on every node (or selected nodes).
👉 Use when:
•	You need node-level services 
•	Examples: logging agents, monitoring agents, CNI plugins 
✔ Features:
•	Automatically runs on new nodes 
•	One instance per node


Kubernetes Failure Handling & Storage
Pod Failures & Restarts
Kubernetes uses self-healing mechanisms to keep applications running.
Restart Policy (Pod-level)
•	Always (default for Deployments) → always restart containers 
•	OnFailure → restart only if the container exits with error 
•	Never → do not restart 
________________________________________
What happens on failures?
👉 Container crash (inside a Pod)
•	The kubelet restarts the container 
•	Uses exponential backoff (e.g., 10s → 20s → 40s…) 

👉 Pod failure/deletion
•	The ReplicaSet (via Deployment) creates a new Pod 
•	New Pod gets a new identity (stateless behavior) 

👉 Node failure
•	Kubernetes detects it via node heartbeats (~40s timeout) 
•	The controller manager reschedules Pods to healthy nodes







Persistent Storage (PV, PVC, StorageClass)
Kubernetes separates storage request, provisioning, and usage:
StorageClass → defines HOW storage is created
     ↓
PersistentVolume (PV) → actual storage resource
     ↓
PersistentVolumeClaim (PVC) → request for storage by a Pod
________________________________________
StorageClass
Defines how storage is provisioned:
•	Type: SSD, HDD, NFS, etc. 
•	Supports dynamic provisioning 
👉 Use when:
•	You want automatic, on-demand storage creation 
________________________________________
PersistentVolume (PV)
Represents the actual storage resource in the cluster:
•	Example: 10GB disk, network storage, etc. 
•	Can be static or dynamically created 
________________________________________
PersistentVolumeClaim (PVC)
A request for storage by a Pod:
•	Example: “I need 50GB of fast storage” 
•	Kubernetes binds it to a matching PV 
________________________________________
Example (KVDB / database use case)
•	KVDB needs persistent storage for session/state data 
•	A PVC requests 50GB 
•	StorageClass provisions an SSD-backed PV 
•	Pod mounts it (e.g., /data)





What happens when you kubectl drain a node?
Draining prepares a node for maintenance or removal.
👉 Step-by-step behavior:
•	Node is cordoned → marked unschedulable (no new Pods assigned) 
•	Existing Pods are gracefully evicted: 
o	SIGTERM sent to containers 
o	Waits terminationGracePeriodSeconds 
o	Forces SIGKILL if not stopped 
________________________________________
👉 Pod handling:
•	Pods managed by controllers (Deployment, StatefulSet) → rescheduled on other nodes 
•	DaemonSet Pods → ignored (remain running unless manually removed) 
•	Standalone Pods (no controller) → lost permanently 
________________________________________
👉 Operational insight (refined)
•	Draining tests high availability 
•	If traffic continues → system tolerates node loss (good resiliency)

How does HPA (Horizontal Pod Autoscaler) work?
HPA automatically scales Pods based on metrics.
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
👉 How it works:
•	Monitors metrics (CPU, memory, or custom metrics) 
•	Compares current usage vs target 
•	Adjusts replica count accordingly 

👉 Scaling logic:
•	CPU > 70% → scale up 
•	CPU < 70% → scale down 
•	Checks every ~15 seconds (default) 
👉 Key idea:
•	Works only for stateless workloads (usually Deployments)


Liveness vs Readiness Probes
Liveness Probe → “Is the container alive?”
•	If it fails → container is restarted 
•	Use for: deadlocks, stuck processes 
________________________________________
Readiness Probe → “Can it serve traffic?”
•	If it fails → Pod is removed from Service endpoints 
•	Use for: startup delays, dependency readiness 
________________________________________
👉 Example:
livenessProbe:
  httpGet:
    path: /healthz
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5






How to debug CrashLoopBackOff
👉 Step-by-step:
# 1. Describe pod (events + errors)
kubectl describe pod <pod-name>

# 2. Current logs
kubectl logs <pod-name>

# 3. Previous crash logs
kubectl logs <pod-name> --previous

# 4. Cluster events
kubectl get events --sort-by=.metadata.creationTimestamp
________________________________________
👉 Common causes:
•	Missing config / secret 
•	Port conflicts 
•	OOMKilled (memory limits too low) 
•	Application crash on startup 
👉 Key idea:
•	CrashLoopBackOff = container keeps failing + restart backoff

Graceful vs Ungraceful Termination
Graceful (SIGTERM)
•	Kubernetes sends SIGTERM 
•	App has time to: 
o	Close connections 
o	Flush data 
•	Controlled by terminationGracePeriodSeconds (default: 30s) 
•	Then SIGKILL if still running 
________________________________________
Ungraceful (SIGKILL)
•	Immediate termination 
•	No cleanup → risk of data loss or corruption 
________________________________________
👉 Operational insight (refined)
•	SIGKILL tests = worst-case failure scenarios 
•	Recovery time measures system resilience 
________________________________________
Pod Affinity & Anti-Affinity
Affinity → “Schedule me near X”
•	Same node / zone 
•	Use for: low latency between services 
________________________________________
Anti-Affinity → “Schedule me away from X”
•	Avoid same node 
•	Use for: high availability (HA) 
________________________________________
👉 Example:
affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchLabels:
          app: kvdb-server
      topologyKey: kubernetes.io/hostname
✔ Ensures replicas run on different nodes
✔ Protects against node failure
________________________________________
Kubernetes Service Discovery
Kubernetes provides built-in networking + DNS.
________________________________________
ClusterIP (default)
•	Internal-only access 
•	Used for service-to-service communication 
________________________________________
NodePort
•	Exposes service on each node (port range: 30000–32767) 
•	Used for basic external access 
________________________________________
LoadBalancer
•	Creates external load balancer (cloud environments) 
•	Used for production external traffic 
________________________________________
DNS
•	Automatic service discovery 
•	Format:
<service>.<namespace>.svc.cluster.local
Example:
eric-pc-up-pfcp-endpoint.namespace.svc.cluster.local



Designing & Testing Real-World Kubernetes Systems
1. Resilience (failure handling)
👉 How to design it
•	Use Deployments / StatefulSets (never standalone Pods) 
•	Configure: 
o	livenessProbe → detect broken containers 
o	readinessProbe → avoid sending traffic too early 
•	Set proper: 
o	resources.requests and limits (avoid OOM) 
•	Use multiple replicas (≥2) 
________________________________________
👉 How to test it
•	Kill containers:
kubectl delete pod <pod>
•	Simulate crash:
kubectl exec <pod> -- kill 1
•	Force ungraceful failure:
kubectl delete pod <pod> --grace-period=0 --force
________________________________________
👉 Good signals
•	Pods restart automatically 
•	No user-visible downtime 
•	No data loss (for stateful apps) 
________________________________________
2. High Availability (HA)
👉 How to design it
•	Run replicas across nodes: 
o	Use pod anti-affinity 
•	Use: 
o	PodDisruptionBudget (PDB) → prevent too many pods going down 
•	Multi-zone setup (if possible) 
________________________________________
👉 How to test it
•	Drain a node:
kubectl drain <node> --ignore-daemonsets
•	Simulate node failure: 
o	Stop kubelet / shut down node 
________________________________________
👉 Good signals
•	Traffic continues during node drain 
•	Pods rescheduled quickly 
•	No single point of failure 
________________________________________
3. Stability (no unexpected behavior)
👉 How to design it
•	Proper: 
o	resource limits 
o	health probes 
•	Avoid: 
o	tight CPU/memory limits 
•	Use monitoring: 
o	restart count 
o	error rates 
________________________________________
👉 How to test it
•	Long-running tests: 
o	Run traffic for hours/days 
•	Check:
kubectl get pods
kubectl describe pod <pod>
________________________________________
👉 Good signals
•	Zero or minimal restarts 
•	No CrashLoopBackOff 
•	Consistent latency 
✔ Your note refined:
“No restarts across clusters” = strong stability indicator
________________________________________
4. Scalability
👉 How to design it
•	Use HPA (Horizontal Pod Autoscaler) 
•	Ensure app is: 
o	stateless 
o	horizontally scalable 
•	Externalize state (DB, cache) 
________________________________________
👉 How to test it
•	Generate load: 
o	tools like hey, wrk, k6 
•	Increase traffic gradually 
________________________________________
👉 Good signals
•	Pods scale up/down automatically 
•	CPU/memory stays near target (e.g., ~70%) 
•	No performance degradation under load 
________________________________________
5. Graceful Degradation (often missed but critical)
👉 How to design it
•	Implement: 
o	timeouts 
o	retries 
o	circuit breakers 
•	Use readiness probes to remove unhealthy pods 
________________________________________
👉 How to test it
•	Kill dependencies (e.g., DB) 
•	Introduce latency 
________________________________________
👉 Good signals
•	System slows down but does not crash 
•	Partial functionality still works 
________________________________________
6. Chaos / Robustness Testing (advanced level)
This is what your “beets tests” are doing.
👉 What to test
•	Kill pods (SIGTERM, SIGKILL) 
•	Drain nodes 
•	Network issues (latency, packet loss) 
________________________________________
👉 Example scenarios
•	Kill 1 pod → should recover instantly 
•	Kill all pods → system should recover within X seconds 
•	Node drain → zero traffic loss 
________________________________________
👉 Good signals
•	Measured recovery time (e.g., 77s max) 
•	No cascading failures 
•	System self-heals 
________________________________________
🔑 Final Mental Model (Interview Gold)
A production-ready Kubernetes system should:
•	Resilience → survives failures 
•	HA → no single point of failure 
•	Stability → no unexpected restarts 
•	Scalability → handles load automatically 
________________________________________
🔥 One-Line Summary (use this in interviews)
“We design for failure using replicas, probes, and anti-affinity; then validate it with chaos testing like pod kills and node drains, measuring recovery time, zero downtime, and stable performance under load.
2. Helm Charts
What is Helm? Chart, Release, Repository?
•	Helm: Package manager for K8s (like apt for Linux)
•	Chart: Package of K8s manifests + templates + values. Like 
eric-pc-up-user-plane-cm-publisher/
•	Release: A running instance of a chart. 
helm install my-release ./chart
•	Repository: Where charts are stored (like Docker Hub for images)
________________________________________
Explain helm install vs upgrade vs rollback
# First time deploy
helm install pcg-release ./pcg-chart -f values-prod.yaml

# Update (new version or config change)
helm upgrade pcg-release ./pcg-chart -f values-prod-v2.yaml

# Something went wrong — go back
helm rollback pcg-release 1    # rollback to revision 1
Copybash
From beets: 
PcgUpf_Maintainability_GeoRed_HelmUpgrade_ISSU_FromX2
 — tests upgrading PCG via Helm while traffic is running.
________________________________________
What is values.yaml? How do you override?
Default config for a chart. Override with:
# File override
helm install release chart -f custom-values.yaml

# Inline override
helm install release chart --set replicaCount=3

# Priority: --set > -f custom > default values.yaml
Copybash
From beets: 
helm_values_cee_geored.yaml
 overrides values for the CEE GeoRed environment.
________________________________________
What are Helm hooks?
Jobs that run at specific lifecycle points:
annotations:
  "helm.sh/hook": pre-upgrade
  "helm.sh/hook-weight": "1"
  "helm.sh/hook-delete-policy": hook-succeeded
Copyyaml
•	pre-install
: Before any resources created (DB migration)
•	post-install
: After all resources created (run smoke test)
•	pre-upgrade
: Before upgrade (backup data)
•	post-upgrade
: After upgrade (verify health)
•	pre-delete
: Before deletion (cleanup)
________________________________________
How do you do rolling update with zero downtime?
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0      # never have less than desired
      maxSurge: 1            # create 1 extra pod at a time
  template:
    spec:
      containers:
      - readinessProbe: ...  # critical — don't send traffic until ready
Copyyaml
Process: Create new pod → wait until ready → remove old pod → repeat. At no point is capacity reduced.
From beets ISSU tests: 
packet loss during failure event is at most 0.5 percent
 — verifies near-zero loss during upgrade.
________________________________________
How do you manage secrets in Helm?
Options:
1.	K8s Secrets (base64, not encrypted at rest by default)
2.	Helm secrets plugin (encrypts values files with SOPS/PGP)
3.	External secrets (AWS Secrets Manager, HashiCorp Vault)
4.	Sealed Secrets (encrypted in Git, decrypted in cluster)
Best practice: Never put secrets in values.yaml in Git. Use external secret management.



Helm Basics
Helm → Package manager for Kubernetes (like apt)
•	Chart → Template + manifests + values.yaml 
•	Release → Running instance of a chart 
•	Repository → Where charts are stored 
________________________________________
install vs upgrade vs rollback
helm install pcg-release ./chart -f values.yaml
helm upgrade pcg-release ./chart -f values-v2.yaml
helm rollback pcg-release 1
•	install → first deploy 
•	upgrade → update config/version 
•	rollback → revert to previous working state 
👉 Key: safe updates with ability to recover fast
________________________________________
values.yaml (Overrides)
Default config for chart
helm install release chart -f custom.yaml
helm install release chart --set replicaCount=3
Priority:
--set > -f > values.yaml
________________________________________
Helm Hooks
Run actions at lifecycle events:
•	pre-install → setup (e.g., DB init) 
•	pre-upgrade → backup 
•	post-upgrade → health checks 
•	pre-delete → cleanup 
👉 Used for safe upgrades
________________________________________
Zero-Downtime Rolling Update
maxUnavailable: 0
maxSurge: 1
•	New Pod → becomes ready → old Pod removed 
•	Requires readinessProbe 
👉 Result: no traffic loss during upgrade
________________________________________
Secrets Management
Options:
•	K8s Secrets 
•	Helm secrets (SOPS) 
•	External (Vault, AWS) 
•	Sealed Secrets 
👉 Best practice:
❌ Don’t store secrets in Git
✅ Use encrypted/external solutions
“Helm packages Kubernetes apps and enables configurable, versioned deployments with safe upgrades, rollbacks, and zero-downtime releases.”

