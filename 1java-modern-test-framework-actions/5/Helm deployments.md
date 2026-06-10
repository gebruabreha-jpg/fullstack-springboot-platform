PCC Test Framework (HelmApi.java methods):-
installHelmChart(chart, releaseName, namespace, values, version) - Full install with values

upgradeHelmChart(releaseName, chart, namespace, values, ...) - Helm upgrade

deleteChart(releaseName, namespace) - Helm uninstall

rollbackToProvidedRevision(releaseName, revision, namespace) - Helm rollback

PCC Step Definitions:-
helm chart is upgraded with additional set flags
Helm install/upgrade with pod restart coordination

Beets Framework (DeploymentSteps.java):-
the Helm chart is installed in namespace {string} with the following parameters
the Helm chart is removed and redeployed 
saved helm chart is reinstalled 
Helm chart is {helm_action} while {int} pods from the following prefixes are restarted