# KWOK Infrastructure Documentation

## What is KWOK?
**KWOK** (Kubernetes WithOut Kubelet) is a toolkit for creating a Kubernetes cluster with thousands of nodes using minimal resources. Unlike traditional Kubernetes clusters where each node runs a kubelet and container runtime, KWOK simulates nodes, pods, and other resources without running actual containers.

### Why KWOK?
- **Fast iteration** — Spin up/destroy clusters in seconds.
- **Low resource usage** — thousands of nodes on a single machine; no Docker or container runtime needed.
- **Realistic API** — exposes the full Kubernetes API, so your automation (Python, kubectl, Helm) needs no changes.
- **CI/CD friendly** — perfect for testing platform automation, operators, and controllers without a cloud cluster.
- **Scalable** — test with 100, 1 000, or 10 000+ fake resources instantly.

## Architecture
┌──────────────────────────────────────────┐
│      KWOK Controller                     │
│  (manages fake nodes / pods / PVCs, etc.)│
└───────────┬──────────────────────────────┘
            │ talks to
┌───────────▼──────────────────────────────┐
│     kube-apiserver (kind / minikube)      │
└──────────────────────────────────────────┘
```
KWOK uses a real kube-apiserver (provisioned by `kind` or `k3d`) but every node and workload is a **fake object** managed by KWOK. No actual pods, containers, or images come into play.
---
## Installation
### Prerequisites
| Tool      | Version | Purpose                            |
|-----------|---------|------------------------------------|
| Go        | 1.22+   | Building KWOK                       |
| Docker    | latest  | Container runtime for kind / k3d   |
| kubectl   | latest  | Kubectl CLI                        |
| kind      | latest  | Creates kube-apiserver container   |

### Linux / macOS
```bash
# 1. Install or update KWOK
go install github.com/kubernetes-sigs/kwok/cmd/kwok@latest

# 2. Install kind  (if you don't have it)
go install sigs.k8s.io/kind@v0.26.0
# or via brew:
# brew install kind

# 3. Verify
kwok --version
kind version
```
### Windows (PowerShell)
```powershell
go install github.com/kubernetes-sigs/kwok/cmd/kwok@latest
go install sigs.k8s.io/kind@v0.26.0
```

> **Note:** Bash scripts in this repo target Linux/macOS. Windows users should convert them to PowerShell or run inside WSL / Git-Bash.
---
## Quick Start
### Step 1 — Create a KWOK Cluster
```bash
# Creates a kind cluster named "kwok-cluster" and starts KWOK controller
kwokctl create cluster kwok-cluster \
    --kubeconfig ~/.kube/config \
    --pod-name-format "pod-{index}" \
    --node-name-format   "node-{index}"
```

This produces:
- A **kube-apiserver** container (kind).
- A **KWOK controller** pod that reads fake-manifests and registers fake nodes/pods in the API server.
### Step 2 — Verify
```bash
kubectl get nodes          # shows "node-0", "node-1", ...
kubectl get pods -A        # initially empty, fills up as manifests are applied
```

### Step 3 — Create a Namespace
```bash
kubectl apply -f infra/namespace.yaml
# or inline:
kubectl create namespace kwok-test
```

---
## Deploying Fake Resources
### Fake Pod
See `infra/fake-pods.yaml`. Key KWOK fields:

| Field | Meaning |
|-------|---------|
| `status.conditions` | Tells KWOK the pod state so it appears **Running** |
| `status.phase`       | Must be `Running` |
| `status.podIP`       | Fake IP address |

```bash
kubectl apply -f infra/fake-pods.yaml -n kwok-test
kubectl get pods -n kwok-test
```

### Fake Deployment (1 000 Replicas)
See `infra/fake-deployment.yaml`.
KWOK converts a `Deployment` into fake ReplicaSets and Pods automatically — all it needs is `replicas` and container specs. No `image` pull occurs.
```bash
kubectl apply -f infra/fake-deployment.yaml -n kwok-test
kubectl get pods -n kwok-test --no-headers | wc -l   # → 1000
```

### ConfigMap for Testing Environment Variables
See `infra/configmap.yaml`. ConfigMaps are consumed by pods as `envFrom`, enabling simple environment variable injection tests.
```bash
kubectl apply -f infra/configmap.yaml -n kwok-test
kubectl get configmap -n kwok-test
---

🔥One-line summary
Idempotent → safe to run again without breaking anything
Ordering issue → wrong sequence breaks system
Race condition → timing causes random failures

1. Verify/install Docker
2. Verify/install kubectl
3. Verify/install Go
4. Install/verify kwokctl + kwok
5. Verify tools
6. Create KWOK cluster
7. Set kubeconfig
8. Verify cluster ready
9. Create namespace
10. Apply fake resources
11. Wait for readiness
12. Health check
13. Run tests
14. Cleanup


python/bash/make file are main automation tools used:-
1.make e2e
2./run_e2e.sh
3.one BDD step  with python test step(.py file)
    Feature: Full E2E System Test
    Scenario: Run full infrastructure test suite
        Given the full environment is set up
        When all tests are executed
        Then the system should be cleaned up
