 Feature File Examples (Gherkin)

  # === PCC TF: Direct Helm operations ===
  When helm chart is installed with additional set flags:
    | key                         | value       |
    | global.pullSecret           | regcred     |
    | persistence.storageClass    | network-ssd |

  When helm chart is upgraded with additional set flags:
    | key                         | value |
    | eric-pc-sm.resources.cpu    | 4     |

  # === Beets: Table-driven install ===
  When the helm chart is installed in namespace "pcg2" with the following parameters:
    | Release Name | Chart Name       | Values                                         | Values Files     | Package Files       | Arguments   |
    | pcg2         | eric-pc-gateway  | mongo.persistence.storageClass="network-block" | helm_values.yaml | values.minimal.yaml | timeout=200 |

  # === Beets: Remove and redeploy (full cycle) ===
  Given the Helm chart is removed and redeployed

  # === Beets: Reinstall from saved values ===
  When saved helm chart is reinstalled

  # === Beets: Helm upgrade with concurrent pod restarts ===
  When Helm chart is upgraded while 10 pods from the following prefixes are restarted with 1 seconds interval:
    | Pod prefixes           |
    | eric-pc-routing-engine |
    | eric-sec               |
    | eric-pc-up             |

  When Helm chart is upgraded keeping object storage images while 5 pods from the following prefixes are restarted with 2 seconds interval:
    | Pod prefixes           |
    | eric-pc-routing-engine |
    | eric-data-coordinator  |

  # === Rollback ===
  When helm release "eric-pc-sm" is rolled back to revision 3
  Then helm release "eric-pc-sm" status is verified
  # PCC TF: helmApi.rollbackToProvidedRevision("pcc", 3, "pcc-namespace")

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Summary: Helm Operations Architecture

  PCC Test Framework:
    Step Definition → HelmApi (interface)
      → installHelmChart(chart, release, namespace, values, version)
      → upgradeHelmChart(release, chart, namespace, values, files, params, switches)
      → deleteChart(release, namespace)
      → rollbackToProvidedRevision(release, revision, namespace)

  Beets Framework:
    Step Definition → DeploymentStepsHelper → HelmApi
      installHelmChart:
        1. DeploymentStepsHelper.deleteExistingReleases()
        2. DeploymentStepsHelper.handleNamespaceDeletion()
        3. DeploymentStepsHelper.buildValuesFiles(entry, productChart, supportFiles)
        4. DeploymentStepsHelper.installChart(helmApi, entry, namespace, version, chart, files, values)

      upgradeWithPodRestart:
        1. CompletableFuture: helmApi.upgradeHelmChart(...)
        2. CompletableFuture: restartRandomPods(prefixes, count, interval)
        3. Wait for both to complete
        4. K8sPodPerformer.waitAllPodsComeUp(...)

  ┌───────────┬─────────────────────────────────────────┬─────────────────────────────────────────┐
  │ Operation │ PCC TF Command                          │ Beets Pattern                           │
  ├───────────┼─────────────────────────────────────────┼─────────────────────────────────────────┤
  │ Install   │ helmApi.installHelmChart(...)           │ Table-driven with DeploymentStepsHelper │
  ├───────────┼─────────────────────────────────────────┼─────────────────────────────────────────┤
  │ Upgrade   │ helmApi.upgradeHelmChart(...)           │ Concurrent with pod restarts            │
  ├───────────┼─────────────────────────────────────────┼─────────────────────────────────────────┤
  │ Delete    │ helmApi.deleteChart(release, ns)        │ + namespace cleanup                     │
  ├───────────┼─────────────────────────────────────────┼─────────────────────────────────────────┤
  │ Rollback  │ helmApi.rollbackToProvidedRevision(...) │ Direct via HelmApi                      │
  ├───────────┼─────────────────────────────────────────┼─────────────────────────────────────────┤
  │ Redeploy  │ N/A                                     │ Delete → recreate namespace → install   │
  └───────────┴─────────────────────────────────────────┴─────────────────────────────────────────┘