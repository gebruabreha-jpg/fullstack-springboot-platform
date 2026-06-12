  # PCC TF: Control Plane Operations
  When API server is restarted
  Then API server is healthy

  When etcd is restarted
  Then etcd health is OK

  When scheduler is restarted
  When controller-manager is restarted
  Then control plane components are running

  # Beets: RBAC misconfiguration
  When RBAC is misconfigured with role "misconfigured-role" in namespace "default"

  # Control plane failure types: API server, etcd, scheduler, controller-manager
  # Admission webhook blocking, RBAC misconfiguration
