# Platform Engineer Interview Prep — DevOps & Infrastructure
> Helm, Terraform, Docker, Kubernetes, CI/CD, Scalability, HA and DR.
> Each section = one interview topic with key concepts, commands, and ready-to-use answers.
---
## Table of Contents
1. [Helm — Kubernetes Package Manager](#1-helm--kubernetes-package-manager)
2. [Terraform — Infrastructure as Code](#2-terraform--infrastructure-as-code)
3. [Docker — Containers & Image Optimization](#3-docker--containers--image-optimization)
4. [Kubernetes — Debugging & Troubleshooting](#4-kubernetes--debugging--troubleshooting)
5. [Kubernetes — Layered Debugging Approach](#5-kubernetes--layered-debugging-approach)
6. [Kubernetes — Workloads & Self-Healing](#6-kubernetes--workloads--self-healing)
7. [Kubernetes — Networking, Storage & Scheduling](#7-kubernetes--networking-storage--scheduling)
8. [Scalability & High Availability](#8-scalability--high-availability)
9. [CI/CD Pipeline](#9-cicd-pipeline)
10. [Disaster Recovery](#10-disaster-recovery)
11. [SLI / SLO / SLA & RPO / RTO](#11-sli--slo--sla--rpo--rto)
---



## 1. Helm — Kubernetes Package Manager
**What:** Reusable templates, versioned deployments, easy upgrades/rollbacks.
### Install
```
scoop install helm        # Windows (scoop)
winget install Helm.Helm  # Windows (winget)
choco install kubernetes-helm  # Windows (choco)
```
### Core Commands
```bash
helm version
helm lint .
helm template <release> . -f <values-file>
helm install <release> . -f <values-file> -n <namespace> --create-namespace
helm upgrade <release> . -f <values-file> --set image.tag=1.26.0 -n <namespace>
helm rollback <release> <revision> -n <namespace>
```
### Multi-Environment Strategy
```
values.yaml          → dev defaults
values-prod.yaml     → production overrides
```
```bash
helm template dev-release . -f values.yaml
helm template prod-release . -f values-prod.yaml
helm install prod-release . -f values-prod.yaml -n prod --create-namespace
```
### Verify Deployment
```bash
kubectl get all -n prod
kubectl get hpa -n prod       # HPA exists
kubectl get pdb -n prod       # PDB exists
kubectl get ingress -n prod   # Ingress exists
```
### Interview Answers
- **"How do you handle multi-environment deployments?"** → One chart, different `values-*.yaml` files per environment.
- **"How do you ensure zero-downtime deployments?"** → RollingUpdate with `maxUnavailable: 0`, `maxSurge: 1`.
---







## 2. Terraform — Infrastructure as Code
**What:** Declarative IaC. Idempotent (safe to run multiple times). State locking prevents race conditions.
### State (`terraform.tfstate`)
State maps `.tf` code to real infrastructure. Without state, Terraform doesn't know:
- Which resources already exist
- What has changed since last apply
- What to update, delete, or leave as-is
**Remote state:** S3 (storage) + DynamoDB (locking) for team collaboration.
### Workflow
```
terraform init   → configure backend (local or remote)
terraform plan   → compare .tf code with state → show changes
terraform apply  → create/update resources → update state
```

### Top 3 Concepts
| Concept | What | Why It Matters |
|---|---|---|
| **State** | Maps code to infra | Without it, Terraform can't track anything |
| **Dependency Graph** | Auto-builds resource ordering | Subnet before EC2, etc. |
| **Plan → Apply** | Preview before execute | Prevents production surprises |

### Drift
- **What:** Actual infra diverges from Terraform state (someone changed something manually).
- **Detect:** `terraform plan` shows mismatch.
- **Fix intentional changes:** Update `.tf` code to match.
- **Fix accidental changes:** Run `terraform apply` to revert to desired state.
### Safety
```hcl
lifecycle {
  prevent_destroy = true  # Terraform refuses to delete this resource
}
```
---







## 3. Docker — Containers & Image Optimization
**Key concept:** Containers virtualize OS/process isolation. VMs virtualize hardware. Containers share the host kernel — the base image provides user-space binaries, not a kernel.
### Dockerfile Best Practices (order matters)
```dockerfile
FROM python:3.12-slim          # 1. Slim base image
WORKDIR /app                   # 2. Working directory
COPY requirements.txt .        # 3. Dependencies first (layer caching)
RUN pip install --no-cache-dir -r requirements.txt
COPY . .                       # 4. Application code
RUN useradd -r appuser         # 5. Non-root user
USER appuser
EXPOSE 5000                    # 6. Port (informational)
CMD ["gunicorn", "--bind", "0.0.0.0:5000", "app:app"]  # 7. Startup
```

### CMD by Language
| Stack | CMD |
|---|---|
| Python (Flask/FastAPI) | `CMD ["python", "app.py"]` |
| Python (Gunicorn) | `CMD ["gunicorn", "--bind", "0.0.0.0:5000", "app:app"]` |
| Node.js | `CMD ["node", "server.js"]` |
| Java | `CMD ["java", "-jar", "app.jar"]` |
| Go / C binary | `CMD ["/mybinary"]` |

### Core Commands
```bash
docker build -t myapp:latest .
docker run -d -p 5000:5000 --name myapp_container myapp:latest
docker ps / docker ps -a
docker images | grep myapp
trivy image myapp:latest          # vulnerability scan
docker scout cves myapp:latest    # alternative scanner
```
### Interview Answers
- **"Why slim/distroless?"** → Smaller attack surface, fewer CVEs, faster pulls.
- **"Why multi-stage?"** → No build tools in production image, smaller size.
- **"Why COPY requirements first?"** → Layer caching — avoids reinstalling deps on every code change.
- **"Why non-root?"** → Principle of least privilege — limits damage if container is compromised.
---






## 4. Kubernetes — Debugging & Troubleshooting
### CrashLoopBackOff — 3-Step Debug
> CrashLoopBackOff is a **symptom**, not the cause.
```bash
kubectl describe pod <pod>        # 1. Check events (OOMKilled, config errors)
kubectl logs <pod> --previous     # 2. See why previous container died
# 3. Check resources, env vars, secrets, dependencies
```
### 502 vs 503
| | 502 Bad Gateway | 503 Service Unavailable |
|---|---|---|
| **Meaning** | Request reached backend, backend failed | Nothing available to serve |
| **Pod state** | Running but broken | Not running / not ready |
| **Service** | Usually OK | No endpoints |
| **Root cause** | App failure (crash, wrong port) | Availability issue |
| **Debug focus** | App / Pod layer | Service / Pod layer |
**Request flow:** `Browser → DNS → LB → Ingress → Service → Pod`
### Node Pressure & Eviction
- High CPU → pods get **throttled**, not killed immediately
- Eviction order by QoS: `BestEffort → Burstable → Guaranteed` (least critical first)
---






## 5. Kubernetes — Layered Debugging Approach
**Rule:** Isolate bottlenecks layer by layer — don't jump to conclusions.
**Path:** `LB → Ingress → Service → Pod → App → Dependencies`
### When to Use Layered Approach
✅ Latency / slow response issues
✅ Intermittent failures (random 200/500/502)
✅ Performance degradation under load
✅ Microservice communication issues
✅ End-to-end request failures ("it's not working")
### When NOT to Use (go directly to root cause)
❌ Clear error messages (OOMKilled, connection refused)
❌ Config issues (wrong env var, missing secret)
❌ Known bugs
### Layer-by-Layer Debug
**Layer 1 — Load Balancer**
```bash
curl http://<LB-IP>                    # Is LB reachable?
kubectl get svc                        # TYPE: LoadBalancer, EXTERNAL-IP assigned?
kubectl describe svc <service-name>    # Ports correct? TargetPort correct?
```
**Layer 2 — Ingress**
```bash
kubectl get ingress
kubectl describe ingress <ingress-name>   # Host/path rules, backend service, port mapping
kubectl logs <nginx-ingress-pod>          # 502/503 errors, upstream failures
```
**Layer 3 — Service**
```bash
kubectl get svc
kubectl describe svc <service-name>       # port vs targetPort, selector labels
kubectl get endpoints <service-name>      # Empty = no pods = 502/503
```
**Layer 4 — Pod**
```bash
kubectl get pods                          # Running? CrashLoopBackOff? Pending?
kubectl describe pod <pod-name>           # Events, OOMKilled, scheduling issues
```
**Layer 5 — Application (Container)**
```bash
kubectl exec -it <pod> -- curl localhost:5000   # App listening? Responds correctly?
```
**Layer 6 — Dependencies (DB / External)**
- DB connection errors, API timeouts, connection pool exhaustion
---








## 6. Kubernetes — Workloads & Self-Healing¡
### Workload Types
| Type | Use Case |
|---|---|
| **Deployment** | Stateless apps (web servers, APIs). Uses ReplicaSets. |
| **StatefulSet** | Stateful apps with stable identity + persistent storage (databases). |
| **DaemonSet** | One pod per node (monitoring agents, log collectors). |

### Self-Healing Mechanisms

| Failure | Response |
|---|---|
| Container crash | Kubelet restarts with exponential backoff (10s → 20s → 40s…) |
| Pod failure/deletion | ReplicaSet creates new Pod (new identity) |
| Node failure | Detected via heartbeats (~40s), pods rescheduled to healthy nodes |
---









## 7. Kubernetes — Networking, Storage & Scheduling
### DNS
Pods reach services via `<service>.<namespace>.svc.cluster.local` → CoreDNS resolves to ClusterIP → kube-proxy load-balances to pod endpoints.
### Storage
```
PersistentVolume (PV)      → actual storage resource
PersistentVolumeClaim (PVC) → request for storage by a Pod
```
### Node Drain
```bash
kubectl drain <node> --ignore-daemonsets --delete-emptydir-data
```
1. Node cordoned (marked unschedulable)
2. Pods gracefully evicted (SIGTERM → wait → SIGKILL)
3. Controller-managed pods → rescheduled. DaemonSet pods → ignored. Standalone pods → **lost permanently**.
### Affinity
- **Affinity** ("schedule me near X") → low latency between services
- **Anti-Affinity** ("schedule me away from X") → high availability
### PodDisruptionBudget (PDB)
Prevents too many pods going down during voluntary disruptions (drains, upgrades).
---






## 8. Scalability & High Availability
### Scalability Dimensions
Traffic (TPS), latency targets, data consistency needs, growth projections, partition tolerance (CAP).
| Type | How to Scale |
|---|---|
| **Stateless** | Add new instances (horizontal scaling) |
| **Stateful** | Read replicas, sharding, caching |
### Read Replica vs Global DB
- **Read Replica** = scale reads, single-region writes
- **Global DB** = multi-region access, distributed writes
### HA
- **RTO** driven: Active/Active or Active/Standby
- **RPO** driven: Sync replication or Async replication
| Pattern | Description | Trade-off |
|---|---|---|
| **Active/Active** | Both clusters handle traffic | Complex (data consistency) but more capacity |
| **Active/Standby** | One active, one synced idle | Simpler but wastes resources |
---






## 9. CI/CD Pipeline
- **CI (Continuous Integration):** Merge frequently → build → test → catch bugs early
- **CD (Continuous Delivery):** Auto-deploy to staging, manual approval for production

### Rollback vs Roll-Forward
- **Rollback** = revert to stable version → safe & fast
- **Roll-forward** = fix & continue → faster long-term velocity
---





## 10. Disaster Recovery
| Strategy | Speed | Cost |
|---|---|---|
| **Backup & Restore** | Slow | Cheap |
| **Active/Standby** | Medium | Medium |
| **Active/Active** | Fast | Expensive |
---




## 11. SLI / SLO / SLA & RPO / RTO
| Term | What | Example |
|---|---|---|
| **SLI** (Indicator) | The metric | "99.95% of requests succeed" |
| **SLO** (Objective) | The target | "We aim for 99.99% success rate" |
| **SLA** (Agreement) | The contract | "Below 99.9% → customer gets credit" |
| **RPO** (Recovery Point) | How much data can you lose? | "Last 1 minute of data" |
| **RTO** (Recovery Time) | How fast must you recover? | "Within 15 minutes" |
### Chaos Engineering
- **Litmus** (K8s native) — pod/node/network chaos
  - Kill pods (SIGTERM, SIGKILL)
  - Drain nodes
  - Network issues (latency, packet loss)
### Production Checklist
- ✅ Resilience → survives failures
- ✅ HA/DA → no single point of failure
- ✅ Stability → no unexpected restarts
- ✅ Scalability → handles load automatically
