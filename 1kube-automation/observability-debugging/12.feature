  # PCC TF: Observability / Debugging Hooks
  When logs of pod "eric-pc-sm-smf-pgw-session-0" are collected
  When describe output of pod "eric-pc-sm-smf-pgw-session-0" is collected
  When resource metrics of pod "eric-pc-sm-smf-pgw-session-0" are collected
  When resource metrics of node "worker-pool1-xxx" are collected
  When events in namespace "pcc-namespace" are collected
  When recent failures are fetched

  # Verify logs contain expected text
  Then logs of pod "eric-pc-sm-smf-pgw-session-0" contain "Started successfully"

  # Beets: Log collection after experiments
  When pod logs and describe outputs for experiment "pod_network_loss" are collected

  # Observability for: container crash, pod restart, node failure
  # Used to diagnose: OOMKilled, CrashLoopBackOff, etc.
