 Feature File Examples (Gherkin)

  # --- PCC TF: Single pod actions ---
  When a pod with prefix "eric-pc-sm-smf-pgw-session" is deleted
  When a pod with prefix "eric-pc-sm-smf-pgw-session" is restarted
  When a pod with prefix "eric-pc-sm-controller" is force deleted

  # --- PCC TF: Multiple pods by prefix ---
  When pods with prefix are deleted:
    | Number of pods | Prefix                     |
    | 2              | eric-pc-sm-smf-pgw-session |
    | 1              | eric-pc-kvdb-rd-data       |
    | all            | eric-pc-sm-notification    |

  When pods with prefix are restarted:
    | Number of pods | Prefix                | Role   |
    | 1              | eric-pc-sm-controller | active |

  # --- PCC TF: By pod name pattern (regex) ---
  When pods with name matching specified pattern are deleted:
    | Number of pods | Pod name pattern             |
    | 1              | eric-pc-sm-controller.*      |
    | 2              | eric-data-object-storage-mn-\\d+ |

  # --- PCC TF: By resource (Deployment/StatefulSet) ---
  When pods controlled by resource are restarted:
    | Number of pods | Resource               |
    | 1              | eric-pc-sm-smf-pgw-session |
    | all            | eric-pc-kvdb-rd-data   |

  # --- PCC TF: Process kill on pod ---
  Then a pod with label 'eric-pc-networking-cm-agent' is restarted by killing process '/usr/local/bin/agentd' with signal 9
  When the process '/usr/local/bin/agentd' is killed with signal 9 on a pod with label 'eric-pc-networking-cm-agent'

  # --- Beets: Routing-engine pod actions ---
  When primary routing-engine pod is deleted
  When secondary routing-engine pod is restarted
  When primary routing-engine pod is force deleted on active cluster

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Summary: PodAction Execution Flow

  Step Definition (Gherkin)
    → K8sPodManagementSteps (selects pods via K8sPodRetriever)
      → K8sPodPerformer.runActionOnPods(foundPods, podAction, cluster, ...)
        → PodAction.execute(k8sNodeApis, kubectlApi, kubectlProperties, podName)
          → DELETED:       kubectlApi.deletePodGrace(podName)
          → FORCE_DELETED: kubectlApi.deletePodForce(podName)
          → RESTARTED:     kill ALL containers → wait for K8s to notice restart

            │ Action        │ What it does                                       │ Kubernetes result                                     │
  ├───────────────┼────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ DELETED       │ kubectl delete pod <name>                          │ Pod gracefully terminates, controller creates new one │
  ├───────────────┼────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ FORCE_DELETED │ kubectl delete pod <name> --force --grace-period=0 │ Immediate removal from API, controller recreates      │
  ├───────────────┼────────────────────────────────────────────────────┼───────────────────────────────────────────────────────┤
  │ RESTARTED     │ SIGKILL all containers in the pod                  │ Pod restarts in-place (same pod name for StatefulSet) │