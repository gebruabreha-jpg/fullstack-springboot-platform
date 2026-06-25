  # PCC TF: Kill container by prefix
  When a container is killed:
    | Pod Prefix                 | Container  | Kill Signal | Number of pods |
    | eric-pc-sm-smf-pgw-session | smf        | sigkill     | 1              |

  # PCC TF: Kill container by resource
  When a container controlled by resource is killed:
    | Pod Resource       | Container  | Kill Signal | Number of pods |
    | eric-pc-sm-ds      | confd      | sigkill     | 1              |

  # PCC TF: Kill by pod name pattern
  When a container controlled by pod that matches name pattern is killed:
    | Pod name pattern                 | Container                    | Kill Signal | Number of pods |
    | eric-data-object-storage-mn-\\d+ | eric-data-object-storage-mn  | sigkill     | 1              |

  # Beets: Kill VPN container
  Then the container "eric-pc-network-forwarder-vp" controlled by home-pod of "ikev1-vpn-1" VPN is killed

  # Beets: CRE reboot methods
  Then cre pod reboot one by one use method kill container
  Then cre pod reboot one by one use method kubectl delete pod