# Storage Volume Operations — Detailed Documentation

## Volume Lifecycle in Kubernetes

A Kubernetes volume lifecycle involves multiple controllers and components working together:

1. **PVC Creation** — User creates PersistentVolumeClaim
2. **Provisioner** — Dynamically provisions PersistentVolume (if StorageClass exists)
3. **Binding** — PV is bound to PVC
4. **Pod Scheduling** — Scheduler finds node with available PV access
5. **Attachment** — CSI driver attaches volume to node
6. **Mount** — kubelet mounts volume into pod container

### Supported Failure Types

| Failure Type           | Mechanism                              | Kubernetes Result                        |
|------------------------|----------------------------------------|------------------------------------------|
| PVC deletion           | `kubectl delete pvc <name>`            | PV retained (or reclaimed per policy)    |
| Volume detach          | `kubectl delete pod` + detach timeout  | Pod rescheduled on different node        |
| Volume attach failure  | CSI simulate attach failure            | Pod stays in ContainerCreating state     |
| Slow disk I/O          | `tc qdisc` on block device             | Application timeouts, retries            |
| PV corruption          | Corrupt underlying disk/storage        | Read errors, application crashes          |

### Key Patterns

- **Graceful deletion**: `kubectl delete pvc --grace-period=30` — allows data flush
- **Force deletion**: `kubectl delete pvc --grace-period=0 --force` — immediate removal
- **Volume detach**: Delete pod, wait for detach, verify volume is available
- **Slow disk**: Use `tc qdisc` to simulate latency on block device interface
- **I/O failure**: Use CSI driver's `simulateIOFailure()` for controlled failures

### Integration Points

- `KubectlApi.deletePVC(pvcName)` — delete PVC
- `KubectlApi.getPVCStatus(pvcName)` — check PVC phase
- `KubectlApi.getPodsUsingPVC(pvcName)` — find pods bound to PVC
- `K8sNodeApi.simulateSlowDisk(interface, delay, variance)` — tc-based disk latency
- `K8sNodeApi.simulateIOFailure(mountPath)` — CSI-level I/O failure
