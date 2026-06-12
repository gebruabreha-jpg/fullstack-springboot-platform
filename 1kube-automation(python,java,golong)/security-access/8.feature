  # PCC TF: Security / Access Failures
  Then user "test-user" does not have "delete" access to "pods"
  Then secret "db-password" is missing in namespace "default"
  Then pod "eric-pc-sm-smf-pgw-session-0" is not running as root
  Then service account "test-sa" token is invalid in namespace "default"

  # Beets: RBAC misconfiguration
  When RBAC is misconfigured with role "bad-role" in namespace "default"

  # Network policy blocking
  When network policy from file "block-all-policy.yaml" is applied in namespace "default"

  # Security failure types:
  # Unauthorized API access, RBAC misconfiguration, secret leakage,
  # Pod running as root, network policy blocking, SA token failure
