  # PCC TF: Resource Exhaustion Failures
  When CPU is saturated on pod "eric-pc-sm-smf-pgw-session-0" with 4 cores
  When memory is exhausted on pod "eric-pc-sm-smf-pgw-session-0" with 512 MB
  When disk is filled on pod "eric-pc-sm-smf-pgw-session-0" at mount "/var/data" with 1024 MB

  # Beets: Ephemeral storage and connection pool
  When ephemeral storage is filled on pod "eric-pc-sm-smf-pgw-session-0" with 512 MB
  When connection pool is exhausted for service "eric-pc-sm-svc" with 100 connections

  # Verify pods handle resource exhaustion
  Then pod "eric-pc-sm-smf-pgw-session-0" restarts due to resource exhaustion
  Then pod "eric-pc-sm-smf-pgw-session-0" is not OOMKilled

  # Resource exhaustion failure types:
  # CPU saturation, memory exhaustion, disk full, ephemeral storage full,
  # File handle exhaustion, connection pool exhaustion
