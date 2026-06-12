# Security Access Failures — Detailed Documentation

## Kubernetes Security Failure Patterns

### Supported Failure Types

| Failure Type                | Mechanism                                    | Kubernetes Result                       |
|-----------------------------|----------------------------------------------|-----------------------------------------|
| Unauthorized API access     | RBAC denies request for user/service account | 403 Forbidden                           |
| RBAC misconfiguration       | Too permissive/missing RoleBindings          | Privilege escalation or access denied   |
| Secret leakage/missing      | Secret deleted or wrong value pushed         | Pod fail to start, config errors        |
| Pod running as root         | SecurityContext not set or wrong             | Security policy violation, audit alert  |
| Network policy blocking     | NetworkPolicy denies traffic                 | Connection refused, timeout             |
| Service account token failure | Token expired or invalid                   | 401 Unauthorized, pod can't call APIs   |

### RBAC Commands

```bash
kubectl auth can-i <verb> <resource> --as system:serviceaccount:<ns>:<sa>
kubectl get roles,rolebindings,clusterroles,clusterrolebindings -n <ns>
kubectl describe role <role> -n <ns>
kubectl describe rolebinding <rb> -n <ns>
```

### Secret Commands

```bash
kubectl get secrets -n <ns>
kubectl describe secret <secret> -n <ns>
kubectl get secret <secret> -n <ns> -o jsonpath='{.data.<key>}' | base64 -d
```

### Security Context Commands

```bash
kubectl get pod <pod> -o jsonpath='{.spec.securityContext}'
kubectl get pod <pod> -o jsonpath='{.spec.containers[*].securityContext.runAsUser}'
kubectl get pod <pod> -o jsonpath='{.spec.containers[*].securityContext.allowPrivilegeEscalation}'
```

### Network Policy Commands

```bash
kubectl get networkpolicy -n <ns>
kubectl describe networkpolicy <np> -n <ns>
kubectl get pod <pod> --show-labels
```

### Service Account Token Commands

```bash
kubectl create token <sa> -n <ns> --duration=<duration>
kubectl get secret <sa-token> -n <ns>
kubectl describe secret <sa-token> -n <ns>
```

### Key Patterns

- **Negative testing**: Verify access is DENIED when it should be
- **RBAC drift**: Apply misconfigured RBAC, verify impact, restore
- **Secret rotation**: Delete/update secret, verify pods reload (if configured)
- **Security context enforcement**: Verify pods run as non-root, no privilege escalation
