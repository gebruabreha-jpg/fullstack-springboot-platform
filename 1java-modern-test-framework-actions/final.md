# Kubernetes + Helm Automation & Chaos Engineering Summary

## Kubernetes Automation Patterns

### Workload Lifecycle Automation
- **Deployments/ReplicaSets**: Auto-create and maintain desired Pod count
- **Self-healing**: Failed Pods automatically restarted or replaced
- **Auto-scaling**: HPA scales based on CPU/memory/custom metrics
- **Rolling updates**: Zero-downtime updates via gradual Pod replacement

### Node-Level Automation
- **Node cordon**: Prevents new workloads from scheduling
- **Node drain**: Safely evicts Pods before maintenance
- **Cluster autoscaler**: Adds/removes nodes based on demand

### Resilience Automation
- **Liveness probes**: Restart unhealthy containers
- **Readiness probes**: Remove unhealthy Pods from service
- **ReplicaSets**: Ensure desired replicas always exist

## Helm Automation Patterns

### Key Concepts
- **Chart**: Application template
- **Values.yaml**: Configurable parameters
- **Release**: Deployed instance of a chart
- **Revision history**: Versioned deployment state

### Common Flows
```
Install:    helm install app ./chart -f values.yaml
Upgrade:    helm upgrade app ./chart -f values.yaml
Rollback:   helm rollback app 2
Uninstall:  helm uninstall app
```

### CI/CD Integration
- Git commit → pipeline triggers Helm upgrade
- GitOps tools (Argo CD / Flux) continuously reconcile cluster state

## Chaos Engineering Patterns

### Failure Injection Levels

| Level | Actions | Validates |
|-------|---------|-----------|
| Container | SIGKILL container, hard-stop processes | Restart policies, liveness probes, stateless recovery |
| Pod | Delete Pod, Evict Pod, Kill app container | ReplicaSet self-healing, service discovery, load balancing |
| Node | Restart/shutdown, kernel panic, drain/cordon | Rescheduling, cluster capacity, stateful workloads |
| Network | tc netem (latency/packet loss), interface down | Retries, circuit breakers, service mesh resilience, timeouts |

### Implementation Notes

This reference project demonstrates how to structure test automation for Kubernetes chaos testing:

- **Modular design**: Each `.java` file is self-contained with nested interfaces
- **Action patterns**: `KillRequest`, `PodAction`, `NodeAction` enums for typed operations
- **Step definitions**: Gherkin scenarios map to Java methods
- **Platform abstraction**: Same actions work across different infrastructure types (Baremetal/IPMI, OpenStack, Azure, Mesos, GDCE)

### Key Takeaways for Platform Engineers

1. **Fault injection is code** — Actions are version-controlled test steps
2. **Orchestration matters** — Parallel execution + synchronization for multi-node chaos
3. **Cleanup is critical** — Always restore state in `finally` blocks
4. ** Abstraction layers** — Same interface, different implementations per platform
5. **Verification patterns** — Poll until condition, assert on recovery state