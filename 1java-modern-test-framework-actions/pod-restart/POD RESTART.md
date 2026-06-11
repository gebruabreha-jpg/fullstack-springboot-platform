Key classes and methods found:

KubectlApi.deletePod(podName) - used in multiple places
ConfigMapHelper.restartPodsWithPrefix(kubectlApi, podPrefix, timeout) - used in BeetsSuiteFixture
ActionsHelper.killContainerInPod(podName, containerName, kubectlApi, k8sNodeApis) - used in PcgStepDefinition and UpfCreIntegrationSteps
UpgradeSteps.java - has pod restart logic in helmUpgradeWithRestartingPods
AdpOamRobustnessStepDefinition.java - has pod kill logic with random pod selection

POD RESTART:

ConfigMapHelper.restartPodsWithPrefix(kubectlApi, podPrefix, timeout) - used in BeetsSuiteFixture.java
PodAction.RESTARTED.execute(k8sNodeApis, kubectlApi, properties, podName) - used in UpgradeSteps.java for restarting pods during helm upgrade
POD KILL:

ActionsHelper.killContainerInPod(podName, containerName, kubectlApi, k8sNodeApis) - kills container in pod
kubectlApi.deletePod(podName) - deletes a pod
kubectlApi.killContainerInsidePod(podName, containerName) - might exist