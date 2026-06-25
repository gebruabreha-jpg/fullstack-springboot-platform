# Resource Exhaustion Failures — Detailed Documentation

## Kubernetes Resource Limit Testing

### Supported Failure Types

| Failure Type                | Mechanism                                    | Kubernetes Result                       |
|-----------------------------|----------------------------------------------|-----------------------------------------|
| CPU saturation              | LimitReached → throttling / stress-ng       | Slow response, OOMKilled avoided        |
| Memory exhaustion           | LimitReached → OOMKilled                     | Pod restarted, CrashLoopBackOff         |
| Disk full (root/volume)    | Fill filesystem with data                    | Write failures, app crashes             |
| Ephemeral storage full      | emptyDir full                                | App write failures, crash               |
| File handle exhaustion      | Open max files → EMFILE                     | "Too many open files" errors            |
| Thread pool exhaustion      | Max threads reached                          | Request queuing, timeouts               |
| Connection pool exhaustion  | DB/HTTP connection pool full                 | Connection refused, timeout             |

### CPU Saturation Commands

```bash
# Inside pod
kubectl exec -it <pod> -- stress-ng --cpu 4 --timeout 60
kubectl exec -it <pod> -- dd if=/dev/zero bs=1M count=1024 | gzip | gzip -d | dd of=/dev/null
```

### Memory Exhaustion Commands

```bash
# Inside pod
kubectl exec -it <pod> -- stress-ng --vm 1 --vm-bytes 512M --timeout 60
kubectl exec -it <pod> -- python3 -c "a = ' ' * 1024 * 1024 * 512; input()"
```

### Disk Fill Commands

```bash
# Inside pod
kubectl exec -it <pod> -- dd if=/dev/zero of=/tmp/fill bs=1M count=1024
kubectl exec -it <pod> -- fallocate -l 1G /data/fillfile
```

### File Handle Exhaustion

```bash
# Inside pod (requires bash)
kubectl exec -it <pod> -- bash -c 'for i in $(seq 1 65535); do : > /tmp/f$i; done'
```

### Resource Limit Verification

```bash
kubectl describe pod <pod> | grep -E "Limits|Requests"
kubectl top pod <pod>
kubectl top node
kubectl get pod <pod> -o jsonpath='{.status.containerStatuses[*].lastState}'
kubectl get pod <pod> -o jsonpath='{.status.containerStatuses[*].restartCount}'
```

### Key Patterns

- **Resource limit testing**: Apply stress within limits → verify throttling (not OOMKill)
- **Exceed limits**: Apply stress beyond limits → expect OOMKill or CPU throttle
- **Ephemeral storage**: Test emptyDir with size limits
- **Connection pools**: Simulate connection exhaustion for DB/service dependencies
- **Recovery verification**: Verify pod recovers after stressor is removed
