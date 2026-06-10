Complete Java Code — Pod Restart & Kill
1. AppPropMap.java — restartPodsWithPrefix
Deletes all pods matching a prefix, waits for all (and all containers) to restart.

public static void restartPodsWithPrefix(final KubectlApi kubectlApi, final String prefix,
        final int totalTimeout) {
    final List<String> podsWithPrefix = kubectlApi.getPodsWithPrefix(prefix);
    for (final String pod : podsWithPrefix) {
        LOGGER.info("Pod: {} will be deleted", pod);
        kubectlApi.deletePod(pod);
    }
    if (!Poll.isActionCompleted(() -> kubectlApi.checkResourcesUp(prefix),
            Duration.ofSeconds(totalTimeout), Duration.ofSeconds(20))) {
        throw new TestException(
                "Not all pods with prefix %s and/or containers are properly started within %s seconds",
                prefix, totalTimeout);
    }
    final List<String> podsWithPrefixAfterDeletion = kubectlApi.getPodsWithPrefix(prefix);
    if (podsWithPrefix.size() != podsWithPrefixAfterDeletion.size()) {
        throw new TestException("The number of pods with prefix %s is not the same after deletion", prefix);
    }
}
2. PodAction.java — DELETED, FORCE_DELETED, RESTARTED
public enum PodAction {

    DELETED {
        @Override
        public void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
                final KubectlProperties kubectlProperties, final String podName) {
            final int timeoutToRestore = kubectlApi.getShellTimeout();
            try {
                kubectlApi.setShellTimeout(kubectlProperties.getShellTimeoutMsForDelete());
                kubectlApi.deletePodGrace(podName);
            } finally {
                kubectlApi.setShellTimeout(timeoutToRestore);
            }
        }
    },

    FORCE_DELETED {
        @Override
        public void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
                final KubectlProperties kubectlProperties, final String podName) {
            final int timeoutToRestore = kubectlApi.getShellTimeout();
            try {
                kubectlApi.setShellTimeout(kubectlProperties.getShellTimeoutMsForDelete());
                kubectlApi.deletePodForce(podName);
            } finally {
                kubectlApi.setShellTimeout(timeoutToRestore);
            }
        }
    },

    RESTARTED {
        @Override
        public void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
                final KubectlProperties kubectlProperties, final String podName) {
            final List<KillRequest> killRequests;
            final String nodeName;
            synchronized (KubeCtl.class) {
                killRequests = Arrays.stream(kubectlApi.getContainers(podName))
                        .map(containerName -> new KillRequest(podName, containerName,
                                kubectlApi.getContainerId(podName, containerName), KillSignal.SIGKILL))
                        .toList();
                nodeName = kubectlApi.getNodeName(podName);
            }
            final K8sNodeApi k8sNodeApi = k8sNodeApis.getWithHostname(nodeName);
            k8sNodeApi.killContainers(killRequests);

            final Duration restartTimeoutInSec = k8sNodeApi.getContainerRestartTimeout();
            final Duration intervalInSec = Duration.ofSeconds(1);
            final boolean restartFinished = Poll.isActionCompleted(
                    () -> !kubectlApi.checkResourcesUp(podName), restartTimeoutInSec, intervalInSec);
            JcatAssertApi.assertTrue(
                    String.format("'%s' action not finished within '%d' seconds", RESTARTED,
                            restartTimeoutInSec.getSeconds()),
                    restartFinished);
        }
    };

    public abstract void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
            final KubectlProperties kubectlProperties, String podName);
}
3. KubeHelpers.java — executePodActionOnPodByStatus
Finds pods by phase (e.g. "Failed") and reason (e.g. "UnexpectedAdmissionError"), then applies a AppPropMap.

public static void executePodActionOnPodByStatus(final List<String> podStatus, final String podPhase,
        final String podReason, final PodAction podAction, final KubectlApi kubectlApi,
        final KubectlProperties kubectlProperties, final K8sNodeApis k8sNodeApis,
        final SutProperties sutProperties) {
    final String clusterType = sutProperties.getSutClusterType();
    for (final String status : podStatus) {
        final String commandGetPod = String.format(
                "get pods -n %s --field-selector=status.phase=%s -o custom-columns=NAME:.metadata.name --no-headers",
                kubectlApi.getNamespace(), podPhase);
        final List<String> podsList = kubectlApi.kubectl(commandGetPod).trim().lines().collect(Collectors.toList());
        if (podsList.isEmpty()) {
            JcatLoggingApi.setTestInfo(
                    "No pods with '%s' phase have been found in namespace \"%s\".", podPhase,
                    kubectlApi.getNamespace());
        } else {
            podsList.forEach(
                    podName -> checkPodReasonStatus(new PodData(podReason, status, podName, podAction),
                            kubectlApi, kubectlProperties, k8sNodeApis, clusterType));
        }
    }
}

public static void checkPodReasonStatus(final PodData podData, final KubectlApi kubectlApi,
        final KubectlProperties kubectlProperties, final K8sNodeApis k8sNodeApis,
        final String clusterType) {
    boolean reasonFound = false;
    boolean statusFound = false;
    final boolean isClusterTypeMesos = Constants.MESOS.equalsIgnoreCase(clusterType);
    final String[] lines = kubectlApi.describeResource("pod", podData.podName()).trim().split("\\n");
    for (int i = 0; i < lines.length; i++) {
        if (lines[i].trim().matches("Reason:\\s*" + podData.podReason())
                && (i > 0 && lines[i - 1].contains("Status:"))) {
            reasonFound = true;
        }
        if ((!isClusterTypeMesos) && (lines[i].trim().matches("Reason:\\s*" + podData.podStatus())
                && (i > 0 && lines[i - 1].contains("State:")))) {
            statusFound = true;
        }
        if ((reasonFound && statusFound) || (reasonFound && isClusterTypeMesos)) {
            JcatLoggingApi.setTestInfo("Pod '%s' matching Status %s and Reason '%s' is found",
                    podData.podName(), podData.podStatus(), podData.podReason());
            podData.podAction().execute(k8sNodeApis, kubectlApi, kubectlProperties, podData.podName());
            break;
        }
    }
}
4. ActionsHelper.java — killContainerInPod
Kills a specific container within a pod using SIGKILL via K8sNodeApi.

public static void killContainerInPod(final String podName, final String containerName,
        final KubectlApi kubectlApi, final K8sNodeApis k8sNodeApis) {
    final KillSignal signal = KillSignal.SIGKILL;
    JcatLoggingApi.setTestInfo(
            "Killing container '%s' in pod '%s' with signal '%s'.", containerName, podName,
            signal);
    final List<String> containerIds = Collections
            .singletonList(kubectlApi.getContainerId(podName, containerName));
    final String nodeName = kubectlApi.getNodeName(podName);
    final K8sNodeApi k8sNodeApi = k8sNodeApis.getWithHostname(nodeName);
    k8sNodeApi.killContainers(containerIds, signal);
}
5. DeploymentSteps.java — reinstallSavedHelmChart
Deletes existing release + namespace, reinstalls saved chart, restores config.

@TestStep
@When("saved helm chart is reinstalled")
public void reinstallSavedHelmChart() {
    final SutActions sutActions = sutActionsProvider.get();
    final String cluster = clusterRegistryProvider.get().getActiveCluster();
    final HelmApi helmApi = helmApiClusterMap.get(cluster);
    final KubectlApi kubectl = kubectlApiClusterMap.get(cluster);
    final Configuration configuration = beetsConfiguration.getConfiguration();

    JcatLoggingApi.beginStep(String.format("Delete existing release '%s'", helmProperties.getRelease()));
    helmApiClusterMap.get(cluster).deleteChart(helmProperties.getRelease(), kubectlProperties.getNamespace());
    ActionsHelper.deleteNamespace(kubectl, kubectlProperties, helmApi, helmProperties);
    JcatLoggingApi.endStep();

    installSavedHelmChart();

    JcatLoggingApi.beginStep("Setup users");
    sutActions.setupUsersWithIntermediatePassword();
    JcatLoggingApi.endStep();
}
6. UpfCreIntegrationSteps.java — Reboot CRE pods one-by-one (delete + kill container)
// deletePod variant — reboot one-by-one with delay
@TestStep
@Given("cre pods reboot one by one use method delete pod")
public void deletePodInCrePod() {
    final int maxLoops = 12;
    int loopCounter = 1;
    int iteration = 1;
    final List<String> podList = kubectlApi.getPodsWithPattern(Pattern.compile(CRE_POD_WITHOUT_API));

    for (String pod : podList) {
        kubectlApi.deletePod(pod);
        Delay.sleep(Duration.ofSeconds(5));
        while (!kubectlApi.checkAllResourcesUp(kubectlProperties.getIgnorePodPrefix())
                && loopCounter < maxLoops) {
            Delay.sleep(Duration.ofSeconds(10));
            loopCounter++;
        }
        if (iteration++ < podList.size()) {
            Delay.sleep(Duration.ofSeconds(60));
            loopCounter = 1;
        }
    }
}

// killContainer variant — reboot one-by-one with delay
@TestStep
@Then("cre pod reboot one by one use method kill container")
public void killContainerInCrePod() {
    final int maxLoops = 12;
    int loopCounter = 1;
    int iteration = 1;
    final List<String> podList = kubectlApi.getPodsWithPattern(Pattern.compile(CRE_POD_WITHOUT_API));

    for (String pod : podList) {
        final String firstContainerInPod = kubectlApi.getContainers(pod)[0];
        ActionsHelper.killContainerInPod(pod, firstContainerInPod, kubectlApi, k8sNodeApis);
        Delay.sleep(Duration.ofSeconds(5));
        while (!kubectlApi.checkAllResourcesUp(kubectlProperties.getIgnorePodPrefix())
                && loopCounter < maxLoops) {
            Delay.sleep(Duration.ofSeconds(10));
            loopCounter++;
        }
        if (iteration++ < podList.size()) {
            Delay.sleep(Duration.ofSeconds(60));
            loopCounter = 1;
        }
    }
}
7. AdpOamRobustnessStepDefinition.java — ADP pod reboot
Randomly select N ADP pods and delete them in loop, wait for all up each iteration.

private void rebootAdpPodsWithTimeout(final int nrOfPods, final Duration timeout,
        final TimeUnit timeUnit, final int nrOfReboots) {
    final SecureRandom random = new SecureRandom();
    final List<String> adpPodPrefixList = AdpPodName.getNames();
    for (int j = 0; j < nrOfReboots; j++) {
        for (int i = 0; i < nrOfPods; i++) {
            kubectlApi.deletePod(adpPodPrefixList.get(random.nextInt(adpPodPrefixList.size())));
            Delay.sleep(DELAY_PARAM_IN_SEC);
        }
        JcatAssertApi.assertTrue(
                String.format("Not all pods are properly started in '%d %s'",
                        (int) timeout.get(timeUnit.toChronoUnit()), timeUnit),
                Poll.isActionCompleted(kubectlApi::isAllResourcesUp, timeout, Duration.ofSeconds(10)));
    }
}

private void rebootSpecificAdpPods(final DataTableWrapper table, final Duration timeout,
        final TimeUnit timeUnit, final int nrOfReboots) {
    table.verifyHeader("pods");
    final List<String> podList = table.getEntries().stream().map(entry -> entry.get("pods"))
            .collect(Collectors.toList());
    for (int i = 0; i < nrOfReboots; i++) {
        for (int j = 0; j < podList.size(); j++) {
            kubectlApi.deletePod(podList.get(j));
            Delay.sleep(DELAY_PARAM_IN_SEC);
        }
        JcatAssertApi.assertTrue(
                String.format("Not all pods are properly started in '%d %s'",
                        (int) timeout.get(timeUnit.toChronoUnit()), timeUnit),
                Poll.isActionCompleted(kubectlApi::isAllResourcesUp, timeout, Duration.ofSeconds(10)));
    }
}
8. PcgStepDefinition.java — VPN home-pod delete & container kill
@TestStep
@Then("the home-pod of {string} VPN is deleted")
public void restartVPNHomePod(final String ipsecVPN) {
    final String homePodName = getHomePodNameOfGivenIpsec(ipsecVPN);
    JcatLoggingApi.setTestInfo("The home pod '%s' of vpn '%s' is deleted", homePodName, ipsecVPN);
    kubectlApiClusterMap.getFirst().deletePod(homePodName);
}

@TestStep
@Then("the container {string} controlled by home-pod of {string} VPN is killed")
public void killContainerInVPNHomePod(final String containerName, final String ipsecVPN) {
    final String homePodName = getHomePodNameOfGivenIpsec(ipsecVPN);
    JcatLoggingApi.setTestInfo("The container '%s' controlled by home pod '%s' of vpn '%s' is killed",
            containerName, homePodName, ipsecVPN);
    ActionsHelper.killContainerInPod(homePodName, containerName, kubectlApiClusterMap.getFirst(),
            k8sNodeApis);
}
9. UpgradeSteps.java — Helm upgrade with concurrent pod restart
private void helmUpgradeWithRestartingPods(final ClustersToExecute upgradeClustersToExecute,
        final int numberOfPods, final ClustersToExecute podsRestartedClusterToExecute,
        final int restartInterval, final DataTableWrapper prefixesTable,
        final HelmInstallOptions helmInstallOptions) {
    final String upgradeCluster = upgradeClustersToExecute.getCurrentCluster();
    final String podsRestartedCluster = podsRestartedClusterToExecute.getCurrentCluster();
    final String headerName = "Pod prefixes";
    prefixesTable.verifyHeader(headerName);
    final List<String> podPrefixes = prefixesTable.getEntries().stream()
            .map(row -> row.get(headerName)).collect(Collectors.toList());
    final List<Pattern> podPrefixPatterns = getPrefixPatterns(podPrefixes);
    final StringBuilder stringBuilder = new StringBuilder();

    final ThrowingRunnable restartPods = () -> {
        stringBuilder.append(String.format("Pods after restart on cluster '%s':%n", podsRestartedCluster));
        final boolean helmFetchConfirmed = Poll.isActionCompleted(
                () -> isHelmUpgradePrepareComplete.getOrDefault(upgradeCluster, false),
                HELM_CHART_FETCH_TIMEOUT.getDuration(), HELM_CHECK_INTERVAL);
        if (helmFetchConfirmed) {
            synchronized (K8sNodeApis.class) {
                synchronized (KubectlApi.class) {
                    Collections.shuffle(podPrefixPatterns);
                    podPrefixPatterns.stream().limit(numberOfPods)
                            .map(kubectlApiClusterMap.get(podsRestartedCluster)::findAllRunningPodsWithPattern)
                            .forEach(runningPodList -> {
                                if (!runningPodList.isEmpty()) {
                                    final Pod randomPod = runningPodList.stream().findAny()
                                            .orElseThrow(() -> new TestException("Could not find random pod"));
                                    final String podName = randomPod.getMetadata().getName();
                                    PodAction.RESTARTED.execute(
                                            k8sNodeApisClusterMap.get(podsRestartedCluster),
                                            kubectlApiClusterMap.get(podsRestartedCluster),
                                            kubectlProperties.getPropertiesByName(podsRestartedCluster),
                                            podName);
                                    final String summaryLine = String.format("%s %s%n",
                                            ZonedDateTime.now().format(
                                                    DateTimeFormatter.ofPattern(SIMPLE_DATE_PATTERN)),
                                            podName);
                                    stringBuilder.append(summaryLine);
                                    LoggingHelper.createFileAndAppendContentVerbose(
                                            fetchLogHelperProvider.get().getTestCaseLogDir(), summaryLine,
                                            "restarted_pods", null, false);
                                    Delay.sleep(Duration.ofSeconds(restartInterval));
                                }
                            });
                }
            }
        } else {
            throw new TestException(TIMEOUT_REACHED_MSG);
        }
    };
    executeParallelTaskWhileUpgradingDowngradingHelm(upgradeCluster, restartPods,
            MAX_EXECUTION_INTERVAL, stringBuilder, helmInstallOptions);
}
10. PatchSnmpAlarmProviderSecret — Pod delete after secret patch
public static void patchSnmpAlarmProviderSecret(final KubectlApi kubectlApi,
        final String configFilePath) {
    if (configFilePath != null && !configFilePath.isEmpty()) {
        JcatLoggingApi.beginStep("Patching Snmp Alarm Provider");
        final String alarmProviderSecret = PreparePhaseConstants.CONTROLLER_SNMP_ALARM_PROVIDER_CONFIG;
        SnmpHelper.createSnmpSecretFromFile(kubectlApi, alarmProviderSecret, Path.of(configFilePath));
        kubectlApi.deletePod("eric-fh-snmp-alarm-provider");
        JcatLoggingApi.endStep();
    }
}