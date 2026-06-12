  # PCC TF: Cleanup / Recovery Operations
  Given node "worker-pool1-xxx" is uncordoned
  Given node "worker-pool1-xxx" drain is cancelled
  Given network interface "eth1" is restored to UP
  Given tc qdisc rules are removed from interface "eth1"

  Then chaos experiment files "experiment.yaml" are deleted
  Then chaos experiment files "rbac.yaml" are deleted
  Then chaos experiment files "chaosengine.yaml" are deleted
  Then litmus experiment "pod-network-loss-chaos" files are deleted

  # Verify cleanup
  Then node "worker-pool1-xxx" is ready
  Then all pods with prefix "eric-pc-sm" are ready
  Then service "eric-pc-sm-svc" is available
