  Feature File Examples (Gherkin)

  # --- PCC TF: Worker node actions ---
  # Restart worker node hosting specific pods
  When a worker node with at least one of the following running pods is restarted:
    | Pod Prefix       |
    | eric-pc-sm       |
    | eric-pc-kvdb-rd  |

  # Kernel panic on worker node
  When a worker node with at least one of the following running pods is kernel panicked:
    | Pod Prefix                 |
    | eric-pc-sm-smf-pgw-session |

  # Shutdown specific node by pattern
  When a node with name matching pattern ".*worker-pool1.*" is shut down

  # Force restart control plane
  When a control-plane node is force restarted

  # Drain + Restart (maintenance workflow)
  When a memorized worker node "memorized-worker" is drained within 1200 seconds and restarted

  # Unique node actions (no repeat in same test)
  When a control-plane node not used before is restarted
  When a control-plane node not used before is kernel panicked

  # --- Beets: DCGW restart ---
  Given a random DCGW is restarted within 5 minutes

  # --- PCC TF: Node status and describe ---
  Then node "worker-pool1-xxx" status is retrieved
  When node "worker-pool1-xxx" is described

  # --- Kubelet failure injection ---
  When kubelet is restarted on node "worker-pool1-xxx"
  When kubelet process is killed on node "worker-pool1-xxx"

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Summary: Execution Flow for Node Actions

  Step Definition (Gherkin)
    → K8sNodeSteps (selects target node via kubectl queries)
      → NodeAction.execute(k8sNodeApi)
        → k8sNodeApi.performAction(nodeAction, flags)
          → K8sCeeNode / K8sBaremetalNode / K8sMesosNode (platform-specific switch)
            → KERNEL_PANICKED: triggerKernelPanic("echo c > /proc/sysrq-trigger", timeout)
            → RESTARTED: openstackHandler.rebootServer() or ipmitool.restartNode()
            → SHUT_DOWN: openstackHandler.shutdownServer() or ipmitool.shutdownNode()
            → DRAINED: kubectlApi.drainNode(hostname, flags)
            → CORDONED: kubectlApi.cordonNode(hostname)
            → UNCORDONED: kubectlApi.uncordonNode(hostname)