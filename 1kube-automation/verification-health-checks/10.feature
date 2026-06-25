  # PCC TF: Verification / Health Checks
  Then all pods with prefix "eric-pc-sm" are ready
  Then all pods with prefix "eric-pc-sm" are running
  Then pod "eric-pc-sm-smf-pgw-session-0" is healthy
  Then service "eric-pc-sm-svc" has available endpoints
  Then service "eric-pc-sm-svc" is available

  # Beets: Node verification
  Then node "worker-pool1-xxx" is ready
  Then node "worker-pool1-xxx" is not ready
  Then node "worker-pool1-xxx" has no pressure conditions
  Then cluster capacity is sufficient

  # Network verification
  Then link status of interface "eth1" is "UP"
  Then nexthop count on node is 2

  # Chaos result verification
  # Litmus experiment result validation
  Then chaos result for "pod-network-loss-chaos" is Pass
