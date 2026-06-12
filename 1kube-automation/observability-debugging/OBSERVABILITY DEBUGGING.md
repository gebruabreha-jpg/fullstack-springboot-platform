# Observability Debugging — Detailed Documentation

## Kubernetes Observability Hooks

### Log Collection

| Hook                        | Command / Method                       | Purpose                                  |
|-----------------------------|----------------------------------------|------------------------------------------|
| `kubectl logs <pod>`        | Current container logs                 | Application stdout/stderr                |
| `kubectl logs -p <pod>`     | Previous container logs (crash)        | Terminated container output              |
| `kubectl logs --tail=100`   | Last 100 lines                         | Quick tail                               |
| `kubectl logs -l app=<lbl>` | All pods with label                    | Aggregate logs from replicas             |

### Describe / Debug

| Hook                        | Command / Method                       | Purpose                                  |
|-----------------------------|----------------------------------------|------------------------------------------|
| `kubectl describe pod`      | Full pod spec + events + conditions    | Detailed pod state diagnosis             |
| `kubectl describe node`     | Node conditions, allocatable, images   | Node-level diagnostics                   |
| `kubectl describe svc`      | Service endpoints, selector            | Service routing verification             |
| `kubectl describe pvc`      | PVC phase, capacity, access mode       | Storage binding status                   |

### Metrics / Resource

| Hook                        | Command / Method                       | Purpose                                  |
|-----------------------------|----------------------------------------|------------------------------------------|
| `kubectl top pod`           | CPU/memory per pod                     | Resource consumption per workload        |
| `kubectl top node`          | CPU/memory per node                    | Node-level resource utilization          |
| `kubectl get events`        | Cluster events sorted by time          | Recent errors, warnings, normal events   |

### Event / Failure Tracking

| Hook                        | Method                       | Purpose                                  |
|-----------------------------|-------------------------------|------------------------------------------|
| `kubectlApi.fetchEvents(ns)`| Events for namespace              | Recent events in specific namespace      |
| `kubectlApi.getRecentFailures()` | Failed pods, node issues    | Aggregate failure information            |
| `kubectl logs -n kube-system` | Control plane logs           | API server, etcd, scheduler diagnostics  |

### Key Patterns

- **Structured log storage**: Use `FetchLogHelper` to store logs with timestamps
- **Describe collection**: Capture full describe output post-failure
- **Metrics correlation**: Pair `top pod` with `describe pod` for root cause
- **Event filtering**: Filter events by type (Warning, Normal) and reason
