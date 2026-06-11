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

HELM CHART DEPLOYMENT ACTIONS SUMMARY
Category	Action/Method	Location
PCC Test Framework	installHelmChart(chart, releaseName, namespace, values, version)	Helm.java:287-290
installHelmChart(chart, releaseName, namespace, values, files, arguments, version)	Helm.java:293-336
upgradeHelmChart(releaseName, chart, namespace, values, files, params, switches)	Helm.java:339-354
upgradeHelmChartNoDebug(...)	Helm.java:357-366
upgradeHelmChartAtomic(...)	Helm.java:381-408
upgradeHelmChartNoResult(...)	Helm.java:411-421
deleteChart(releaseName, namespace)	Helm.java:130-137
deleteChartWithNoHooks(releaseName)	Helm.java:151-158
rollbackToProvidedRevision(releaseName, revisionNumber, namespace)	Helm.java:424-434
Beets Step Defs	theHelmChartWithGivenNamespaceIsInstalled("ns", cluster, table)	DeploymentSteps.java:1106
helmChartIsRedeployed()	DeploymentSteps.java:291
reinstallSavedHelmChart()	DeploymentSteps.java:573
helmUpgradeWithRestartingPods(...)