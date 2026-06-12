  # PCC TF: Storage/Volume Operations
  When PVC "eric-pc-sm-data-pvc" is deleted
  Then PVC "eric-pc-sm-data-pvc" is bound and pod using it is running

  # Beets: Volume detach and reattach
  When volume is detached from pod "eric-pc-sm-smf-pgw-session-0"
  When slow disk is simulated on node for 200 milliseconds 50 milliseconds
  When I/O failure is simulated on mount path "/var/data"

  # Storage failure types: PVC, PV, slow disk, I/O
  # Verify pod can recover from volume detach/attach cycle
  Then pod "eric-pc-sm-smf-pgw-session-0" is healthy
