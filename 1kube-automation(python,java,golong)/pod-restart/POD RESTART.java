package com.ericsson.pcc.testcases.helpers;

import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SecureRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pod restart and kill action patterns extracted from the PCC and Beets test frameworks.
 * This file is self-contained for documentation and reference purposes.
 */
public final class PodRestartActionsDocumentation {

    private static final Duration DELAY_PARAM_IN_SEC = Duration.ofSeconds(5);
    private static final String CRE_POD_WITHOUT_API = "cre-pod.*";
    private static final String SIMPLE_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final Duration HELM_CHART_FETCH_TIMEOUT = Duration.ofSeconds(300);
    private static final Duration HELM_CHECK_INTERVAL = Duration.ofSeconds(10);
    private static final Duration MAX_EXECUTION_INTERVAL = Duration.ofSeconds(300);
    private static final String TIMEOUT_REACHED_MSG = "Timeout reached";

    private PodRestartActionsDocumentation() {
    }

    public record KillRequest(
            String podName,
            String containerName,
            String containerId,
            KillSignal signal) {
    }

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
                final KubectlProperties kubectlProperties, final String podName);
    }

    public record PodData(String podReason, String podStatus, String podName, PodAction podAction) {
    }

    public static final class AppPropMap {
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

        private static final Logger LOGGER = new Logger();
    }

    public static final class KubeHelpers {
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

        private static final class Constants {
            private static final String MESOS = "mesos";
        }
    }

    public static final class ActionsHelper {
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

        public static void deleteNamespace(final KubectlApi kubectlApi, final KubectlProperties kubectlProperties,
                final HelmApi helmApi, final HelmProperties helmProperties) {
            kubectlApi.deleteNamespace(kubectlProperties.getNamespace());
        }
    }

    public static final class DeploymentSteps {
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

        private void installSavedHelmChart() {
        }

        private SutActions sutActionsProvider = new SutActions();
        private ClusterRegistryProvider clusterRegistryProvider = new ClusterRegistryProvider();
        private Map<String, HelmApi> helmApiClusterMap = Map.of();
        private Map<String, KubectlApi> kubectlApiClusterMap = Map.of();
        private BeetsConfiguration beetsConfiguration = new BeetsConfiguration();
        private HelmProperties helmProperties = new HelmProperties();
        private KubectlProperties kubectlProperties = new KubectlProperties();
    }

    public static final class UpfCreIntegrationSteps {
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

        private KubectlApi kubectlApi = new KubectlApi();
        private KubectlProperties kubectlProperties = new KubectlProperties();
        private K8sNodeApis k8sNodeApis = new K8sNodeApis();
    }

    public static final class AdpOamRobustnessStepDefinition {
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

        private KubectlApi kubectlApi = new KubectlApi();
    }

    public static final class PcgStepDefinition {
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

        private String getHomePodNameOfGivenIpsec(String ipsecVPN) {
            return "home-pod-" + ipsecVPN;
        }

        private List<KubectlApi> kubectlApiClusterMap = List.of(new KubectlApi());
        private K8sNodeApis k8sNodeApis = new K8sNodeApis();
    }

    public static final class UpgradeSteps {
        @GeoredCriticalTestStep
        @When("Helm chart is {helm_action}{cluster_selector} while {int} pods from the following prefixes are restarted{cluster_selector} with {int} seconds interval:")
        public void helmUpgradeWithRestartingPods(final HelmAction helmAction,
                final ClustersToExecute upgradeClustersToExecute, final int numberOfPods,
                final ClustersToExecute podsRestartedClusterToExecute, final int restartInterval,
                final DataTableWrapper prefixesTable) {
            final HelmInstallOptions.Builder helmInstallOptionsBuilder = new HelmInstallOptions.Builder()
                    .withShouldAddHelmValuesFile()
                    .withHelmAction(helmAction);
            if (isHelmActionUpgrade(helmAction)) {
                helmInstallOptionsBuilder.withUpdateCrdRelease();
            }
            final HelmInstallOptions helmInstallOptions = helmInstallOptionsBuilder.build();
            helmUpgradeWithRestartingPods(upgradeClustersToExecute, numberOfPods, podsRestartedClusterToExecute,
                    restartInterval, prefixesTable, helmInstallOptions);
        }

        private void helmUpgradeWithRestartingPods(final ClustersToExecute upgradeClustersToExecute, final int numberOfPods,
                final ClustersToExecute podsRestartedClusterToExecute, final int restartInterval,
                final DataTableWrapper prefixesTable, final HelmInstallOptions helmInstallOptions) {
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
                        HELM_CHART_FETCH_TIMEOUT, HELM_CHECK_INTERVAL);
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

        private List<Pattern> getPrefixPatterns(List<String> podPrefixes) {
            return podPrefixes.stream().map(Pattern::compile).collect(Collectors.toList());
        }

        private boolean isHelmActionUpgrade(HelmAction helmAction) {
            return helmAction == HelmAction.UPGRADE;
        }

        private void executeParallelTaskWhileUpgradingDowngradingHelm(String upgradeCluster, ThrowingRunnable restartPods,
                Duration maxExecutionInterval, StringBuilder stringBuilder, HelmInstallOptions helmInstallOptions) {
            try {
                restartPods.run();
            } catch (Throwable t) {
                throw new TestException(t.getMessage());
            }
        }

        private Map<String, Boolean> isHelmUpgradePrepareComplete = Map.of();
        private Map<String, KubectlApi> kubectlApiClusterMap = Map.of();
        private Map<String, K8sNodeApis> k8sNodeApisClusterMap = Map.of();
        private Map<String, KubectlProperties> kubectlProperties = Map.of();
        private FetchLogHelperProvider fetchLogHelperProvider = new FetchLogHelperProvider();

        @FunctionalInterface
        private interface ThrowingRunnable {
            void run() throws Throwable;
        }
    }

    public static final class PatchSnmpAlarmProviderSecret {
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
    }

    public enum KillSignal {
        SIGTERM(15),
        SIGKILL(9);

        private final int value;

        KillSignal(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum HelmAction {
        UPGRADE,
        DOWNGRADE
    }

    public static class HelmInstallOptions {
        private final HelmAction helmAction;
        private final boolean shouldAddHelmValuesFile;
        private final boolean updateCrdRelease;

        private HelmInstallOptions(Builder builder) {
            this.helmAction = builder.helmAction;
            this.shouldAddHelmValuesFile = builder.shouldAddHelmValuesFile;
            this.updateCrdRelease = builder.updateCrdRelease;
        }

        public static class Builder {
            private HelmAction helmAction;
            private boolean shouldAddHelmValuesFile;
            private boolean updateCrdRelease;

            public Builder withHelmAction(HelmAction helmAction) {
                this.helmAction = helmAction;
                return this;
            }

            public Builder withShouldAddHelmValuesFile() {
                this.shouldAddHelmValuesFile = true;
                return this;
            }

            public Builder withUpdateCrdRelease() {
                this.updateCrdRelease = true;
                return this;
            }

            public HelmInstallOptions build() {
                return new HelmInstallOptions(this);
            }
        }
    }

    public static class Pod {
        private final Metadata metadata;

        public Pod(Metadata metadata) {
            this.metadata = metadata;
        }

        public Metadata getMetadata() {
            return metadata;
        }

        public static class Metadata {
            private final String name;

            public Metadata(String name) {
                this.name = name;
            }

            public String getName() {
                return name;
            }
        }
    }

    public interface K8sNodeApis {
        K8sNodeApi getWithHostname(String hostname);
    }

    public interface K8sNodeApi {
        void killContainers(List<KillRequest> killRequests);

        void killContainers(List<String> containerIds, KillSignal signal);

        Duration getContainerRestartTimeout();
    }

    public interface KubectlApi {
        String[] getContainers(String podName);

        String getContainerId(String podName, String containerName);

        String getNodeName(String podName);

        boolean checkResourcesUp(String podName);

        boolean checkAllResourcesUp(String ignorePodPrefix);

        boolean isAllResourcesUp();

        int getShellTimeout();

        void setShellTimeout(int timeoutMs);

        void deletePodGrace(String podName);

        void deletePodForce(String podName);

        void deletePod(String podName);

        List<String> getPodsWithPrefix(String prefix);

        List<String> getPodsWithPattern(Pattern pattern);

        List<Pod> findAllRunningPodsWithPattern(Pattern pattern);

        String kubectl(String command);

        String describeResource(String resourceType, String resourceName);

        String getNamespace();

        void deleteNamespace(String namespace);
    }

    public interface KubectlProperties {
        int getShellTimeoutMsForDelete();

        String getNamespace();

        String getIgnorePodPrefix();
    }

    public interface SutProperties {
        String getSutClusterType();
    }

    public interface SutActions {
        void setupUsersWithIntermediatePassword();
    }

    public interface ClusterRegistryProvider {
        String getActiveCluster();
    }

    public interface HelmApi {
        void deleteChart(String releaseName, String namespace);
    }

    public interface HelmProperties {
        String getRelease();
    }

    public interface BeetsConfiguration {
        Configuration getConfiguration();
    }

    public interface Configuration {
    }

    public interface DataTableWrapper {
        void verifyHeader(String header);

        List<Map<String, String>> getEntries();
    }

    public interface ClustersToExecute {
        String getCurrentCluster();
    }

    public interface Delay {
        static void sleep(Duration duration) {
        }
    }

    public interface Poll {
        /**
         * Poll until condition succeeds or timeout expires.
         * Loop: while (!condition.run() && elapsed < timeout) { sleep(interval); }
         */
        static boolean isActionCompleted(Runnable condition, Duration timeout, Duration interval) {
            return true;
        }
    }

    public interface JcatAssertApi {
        static void assertTrue(String message, boolean condition) {
            if (!condition) {
                throw new TestException(message);
            }
        }
    }

    public interface JcatLoggingApi {
        static void setTestInfo(String message, Object... args) {
        }

        static void beginStep(String stepName) {
        }

        static void endStep() {
        }
    }

    public interface Logger {
        default void info(String message, Object... args) {
        }
    }

    public interface KubeCtl {
    }

    public interface PreparePhaseConstants {
        String CONTROLLER_SNMP_ALARM_PROVIDER_CONFIG = "eric-fh-snmp-alarm-provider";
    }

    public interface SnmpHelper {
        static void createSnmpSecretFromFile(KubectlApi kubectlApi, String secretName, Path configFilePath) {
        }
    }

    public interface AdpPodName {
        static List<String> getNames() {
            return List.of("adp-pod-1", "adp-pod-2", "adp-pod-3");
        }
    }

    public interface FetchLogHelperProvider {
        default FetchLogHelper get() {
            return new FetchLogHelper();
        }
    }

    public interface FetchLogHelper {
        default String getTestCaseLogDir() {
            return "logs";
        }
    }

    public interface LoggingHelper {
        static void createFileAndAppendContentVerbose(String logDir, String content, String fileName, Object ignored, boolean verbose) {
        }
    }

    public interface TestStep {
    }

    public interface Given {
        String value();
    }

    public interface Then {
        String value();
    }

    public interface When {
        String value();
    }

    public interface GeoredCriticalTestStep {
    }

    public interface TestException extends RuntimeException {
    }

    public static class TestExceptionImpl extends RuntimeException {
        public TestExceptionImpl(String message) {
            super(message);
        }

        public TestExceptionImpl(String message, Object... args) {
            super(String.format(message, args));
        }
    }

    public static class Logger {
        public void info(String message, Object... args) {
        }
    }
}
