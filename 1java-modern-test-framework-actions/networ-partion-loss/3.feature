  Feature File Examples (Gherkin)

  # --- PCC TF: Interface-level control ---
  # Bring down interfaces on random workers
  Then concurrently bring down network interfaces on the following nodes:
    | Node type     | Number of nodes | Interfaces | Duration | Time unit |
    | worker        | 2               | eth1, eth2 | 5        | seconds   |
    | control-plane | 1               | eth1       | 10       | seconds   |

  # Drop all packets via tc netem (100% packet loss)
  Then drop all packets on given interfaces for given duration on the following random nodes:
    | Node type | Number of nodes | Interfaces | Duration | Time unit |
    | worker    | 2               | eth1       | 30       | seconds   |

  # --- Beets: Litmus chaos experiments ---
  When litmus is installed
  When the litmus operator is installed

  When the litmus engine POD_NETWORK_LOSS is configured:
    | variable name                  | value                  |
    | experiment_name                | pod-network-loss-chaos |
    | applabel                       | app=eric-pc-sm         |
    | chaos_duration                 | 60                     |
    | network_packet_loss_percentage | 100                    |
    | affected_pods                  | 1                      |
  When the litmus experiment POD_NETWORK_LOSS is run

  When the litmus engine POD_NETWORK_PARTITION is configured:
    | variable name    | value                         |
    | experiment_name  | pod-network-partition-chaos   |
    | applabel         | app=eric-pc-sm                |
    | chaos_duration   | 120                           |
    | policy_types     | all                           |
  When the litmus experiment POD_NETWORK_PARTITION is run

  # --- Beets: DCGW interface simulation ---
  When DCGW shutdown is simulated by disabling all interfaces towards SUT
  # ... traffic failure verification ...
  When DCGW start is simulated by enabling all interfaces towards SUT

  # DCGW L3 interface break
  Given "sgi1" L3 interfaces of all DCGWs are broken for 10 seconds

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Summary: Two Approaches to Network Chaos

  ┌──────────────────────────────┬───────────┬────────────────────────────────────┬────────────────────────────────────────────────┐
  │ Approach                     │ Framework │ Mechanism                          │ Use Case                                       │
  ├──────────────────────────────┼───────────┼────────────────────────────────────┼────────────────────────────────────────────────┤
  │ ip link set down/up          │ PCC TF    │ Complete L2 interface disable      │ Total link failure simulation                  │
  ├──────────────────────────────┼───────────┼────────────────────────────────────┼────────────────────────────────────────────────┤
  │ tc qdisc netem loss 100%     │ PCC TF    │ L3 packet drop via traffic control │ Packet loss without link down (more realistic) │
  ├──────────────────────────────┼───────────┼────────────────────────────────────┼────────────────────────────────────────────────┤
  │ Litmus POD_NETWORK_LOSS      │ Beets     │ Litmus ChaosEngine injects netem   │ Pod-targeted network loss                      │
  ├──────────────────────────────┼───────────┼────────────────────────────────────┼────────────────────────────────────────────────┤
  │ Litmus POD_NETWORK_PARTITION │ Beets     │ Litmus network isolation           │ Full network isolation                         │
  ├──────────────────────────────┼───────────┼────────────────────────────────────┼────────────────────────────────────────────────┤