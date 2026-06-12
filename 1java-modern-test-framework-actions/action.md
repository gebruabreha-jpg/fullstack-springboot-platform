# Fault Injection & Deployment Actions — Platform Engineering Notes

## Action Chaining Patterns

Shows how to chain multiple faults together for realistic failure scenarios:

### 1. KVDB Kill + CRE Restart
- Kill KVDB master container
- Wait for replica promotion
- Restart CRE pod
- Verify session continuity

### 2. Helm Upgrade + Pod Restart
- Start Helm chart upgrade
- Concurrently restart pods from specified prefixes
- Verify upgrade completes successfully
- Log restarted pods for analysis

### 3. Node Drain + Network Loss
- Drain worker node (evict pods)
- Bring down network interface on same node
- Verify pod rescheduling
- Restore interface and uncordon node

### 4. DCGW Shutdown + Interface Recovery
- Disable all interfaces towards SUT
- Verify traffic interruption
- Re-enable interfaces
- Verify link status and nexthop recovery

## Cleanup Patterns

How experiments clean up after fault injection:

### Node Cleanup
- `kubectlApi.uncordonNode(hostname)` — Make node schedulable again
- `kubectlApi.drainNode(hostname, flags)` — Evict pods before maintenance
- `computeHost.rebootNode(hostname)` — Restart compute host

### Network Cleanup
- `ip link set <iface> up` — Restore interface after bring-down
- `tc qdisc del dev <iface> root netem` — Remove packet loss rules
- `DcgwHelper.enableDcgwLinkInterface(...)` — Re-enable DCGW interfaces

### Litmus Cleanup
- Delete RBAC files
- Delete experiment YAML files
- Delete engine YAML files
- Validate ChaosResult verdict is 'Pass'

### Navigator Cleanup
- `dcgwNode.restoreToPreviousNavigator()` — Restore DCGW navigator state
- `navigator.getExtendedCli().removeInnerDecorator()` — Remove connection decorators

## Verification Patterns

How to verify system behavior after faults:

### Traffic Interruption Time
- Measure time between fault injection and traffic recovery
- Use timestamps from `TimeRegistry`
- Compare against SLA thresholds

### Pod Recovery Time
- Poll `kubectlApi.checkResourcesUp(prefix)` until pods are ready
- Verify pod count matches expected count
- Check container restart counts

### Session Continuity
- Verify active sessions survive fault injection
- Check no data loss during pod restart
- Validate traffic resumes after recovery

### Chaos Result Validation
- `kubectl get chaosresults.litmuschaos.io -o wide`
- Parse verdict field
- Assert verdict equals "Pass"

### Link Status Verification
- `verifyLinkStatusAndNexthopCountIsExpected(..., UP, ...)`
- `verifyLinkStatusAndNexthopCountIsExpected(..., DOWN, ...)`
- Check nexthop counts match expected values

## Safety Guards (Best Practices)

- Always cleanup in `finally` blocks
- Use timeouts for all polling operations
- Verify node/pod state before and after actions
- Log all actions for debugging

## Parallel Execution Patterns

- Use parallel streams for multi-node operations
- Synchronize access to shared Kubernetes APIs
- Coordinate start times for synchronized network loss (10s buffer in BashScheduleBuilder)

## Idempotency Patterns

- Actions should be safe to retry
- Delete existing releases before reinstall
- Handle already-cordoned/uncordoned nodes gracefully
- Check resource existence before deletion
