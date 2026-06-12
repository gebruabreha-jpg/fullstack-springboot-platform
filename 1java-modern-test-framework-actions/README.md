# Modern Test Framework Actions — Platform Engineering Guide
1java-modern-test-framework-actions/
├── Pod-Container/
│   ├── CONTAINER KILL ACTIONS.java    # Standalone Java reference for container/pod kill actions
│   ├── CONTAINER KILL ACTIONS.md      # Detailed documentation
│   ├── 1.java                         # Full implementation walkthrough
│   └── 1.feature                      # Gherkin scenarios
├── node/
│   ├── node_failure.java              # Standalone Java reference for node failure actions
│   ├── node_failure.md                # Detailed documentation
│   ├── 2.java                         # Full implementation walkthrough
│   └── 2.feature                      # Gherkin scenarios
├── network-partition-loss/
│   ├── NETWORK LOSS.java              # Standalone Java reference for network loss actions
│   ├── NETWORK LOSS.md                # Detailed documentation
│   ├── 3.java                         # Full implementation walkthrough
│   └── 3.feature                      # Gherkin scenarios
├── pod-restart/
│   ├── POD RESTART.java               # Standalone Java reference for pod restart actions
│   ├── POD RESTART.md                 # Detailed documentation
│   ├── 1.java                         # Full implementation walkthrough
│   └── 2.feature                      # Gherkin scenarios
├── helm/
│   ├── Helm deployments.java          # Standalone Java reference for Helm deployment actions
│   ├── Helm deployments.md            # Detailed documentation
│   └── 1.java                         # Full implementation walkthrough
├── Litmus/
│   ├── 1.java                         # Litmus chaos experiment implementation
│   └── 1.feature                      # Gherkin scenarios
├── action.md                          # Action chaining and verification notes
└── README.md                          # This file
```

## Action Categories

### 1. Container/Pod Kill Actions
- **Purpose**: Kill containers or restart pods to test resilience
- **Mechanisms**:
  - Kill specific container by pod prefix/resource name
  - Kill process inside container with configurable signal (SIGTERM, SIGKILL)
  - Restart pod by killing all containers
  - Sequential paired kills for KVDB master/replica testing
- **Key Files**: `Pod-Container/CONTAINER KILL ACTIONS.java`, `Pod-Container/1.java`

### 2. Node Failure Actions
- **Purpose**: Simulate node-level failures (restart, shutdown, kernel panic, drain, cordon)
- **Supported Node Types**:
  - Baremetal (IPMI/CAPI)
  - Mesos (computeHost)
  - Nexus/Azure
  - GDCE (Pub/Sub)
  - CEE/OpenStack
  - LPG (IPMI)
- **Key Files**: `node/node_failure.java`, `node/2.java`

### 3. Network Loss/Partition Actions
- **Purpose**: Inject network impairments (interface down, packet loss, Litmus chaos)
- **Mechanisms**:
  - `ip link set` for interface control
  - `tc qdisc netem` for packet loss
  - Litmus experiments for pod-level network chaos
  - DCGW interface simulation
- **Key Files**: `network-partition-loss/NETWORK LOSS.java`, `network-partition-loss/3.java`, `Litmus/1.java`

### 4. Pod Restart Actions
- **Purpose**: Restart pods via deletion or container kill
- **Mechanisms**:
  - Delete pod and wait for recreation
  - Kill container to trigger restart
  - Coordinated restart during Helm upgrade
  - Random pod selection for robustness testing
- **Key Files**: `pod-restart/POD RESTART.java`, `pod-restart/1.java`

### 5. Helm Deployment Actions
- **Purpose**: Manage Helm chart lifecycle (install, upgrade, rollback, delete)
- **Mechanisms**:
  - Install with values/files/version
  - Upgrade with command-line parameters
  - Rollback to specific revision
  - Coordinated upgrade with pod restart
- **Key Files**: `helm/Helm deployments.java`, `helm/1.java`

### 6. Litmus Chaos Experiments
- **Purpose**: Run chaos experiments via Litmus operator
- **Supported Experiments**:
  - `pod_cpu_hog`
  - `pod_memory_hog`
  - `container_kill`
  - `pod_network_loss`
  - `pod_network_partition`
  - `pod_network_latency`
  - `pod_network_corruption`
  - `pod_io_stress`
  - `kubelet_service_kill`
  - `docker_service_kill`
- **Key Files**: `Litmus/1.java`, `networ-partion-loss/NETWORK LOSS.java`

## Platform Engineering Notes

### Action Chaining
Shows how to chain multiple faults together:
- KVDB kill + CRE restart
- Helm upgrade + pod restart
- Node drain + network loss
- DCGW shutdown + interface recovery

### Cleanup Patterns
How experiments clean up:
- Uncordon nodes after drain
- Restore network interfaces after bring-down
- Delete Litmus CRDs after experiments
- Restore navigator state after DCGW operations

### Verification Patterns
How to verify system behavior after faults:
- Traffic interruption time
- Pod recovery time
- Session continuity after faults
- Chaos result validation (Pass/Fail)

## Java File Design

Each standalone Java file follows this pattern:
1. **Nested interfaces** - Define contracts for external dependencies
2. **Nested enums** - Define action types and configurations
3. **Static helper classes** - Implement the core action logic
4. **Default implementations** - Provide no-op or simple implementations for interfaces

This allows each file to be read independently without needing the full project build context.

## Usage

These files are reference documentation, not production code. They show:
- How actions are structured in the real framework
- What dependencies each action needs
- How step definitions map to implementation methods
- How cleanup and verification are handled

Container/Pod Kill — kill containers, restart pods, signal injection
Node Failure — simulate baremetal/Mesos/Nexus node failures
Network Loss/Partition — tc netem, interface down, Litmus network chaos
Pod Restart — delete/kill pods, coordinated Helm-upgrade restarts
Helm Deployments — install, upgrade, rollback, delete
Litmus Chaos — CPU/memory hog, network loss/latency/corruption, IO stress, service kills
