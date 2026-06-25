  # PCC TF: Action Chaining
  When chaos chain "kvdb_kill + cre_restart" is executed with interval 5 seconds
  When composite fault "helm_upgrade + pod_restart" is triggered
  When composite fault "node_drain + network_loss" is triggered
  When composite fault "dcgw_shutdown + iface_recovery" is triggered
