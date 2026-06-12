package com.ericsson.pcc.testcases.helpers;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Helm deployment action patterns extracted from the PCC and Beets test frameworks.
 * This file is self-contained for documentation and reference purposes.
 */
public final class HelmDeploymentsDocumentation {

    private static final String UPGRADING_HELM_RELEASE_MESSAGE = "Upgrading helm release: {} with chart: {}";
    private static final String ERROR_WHILE_UPGRADING_MESSAGE = "Error while upgrading the helm chart: %s";
    private static final String PCX_SECRET_NAME = "pcx-secret";
    private static final String ADDITIONAL_PCG_TIMESTAMP = "ADDITIONAL_PCG_TIMESTAMP";

    private HelmDeploymentsDocumentation() {
    }

    public static final class Helm {
        public void installHelmChart(final String chart, final String releaseName, final String namespace,
                final Map<String, String> values, final String version) {
            installHelmChart(chart, releaseName, namespace, values, null, null, version);
        }

        public void installHelmChart(final String chart, final String releaseName, final String namespace,
                final Map<String, String> values, final List<File> files, final Map<String, String> arguments,
                final String version) {
            final String valuesParam = values != null && !values.isEmpty() ? String.format(", values='%s'", values) : "";
            final String filesParam = files != null && !files.isEmpty() ? String.format(", files='%s'", files) : "";
            final String argumentsParam = arguments != null && !arguments.isEmpty() ? String.format(", arguments='%s'", arguments) : "";
            final String versionParam = version != null && !version.isEmpty() ? String.format(", version='%s'", version) : "";
            LOGGER.info("Installing Helm chart: [chart='{}', release='{}', namespace='{}'{}{}{}{}]", chart,
                    releaseName, namespace, valuesParam, filesParam, argumentsParam, versionParam);

            fixTimeoutArgument(arguments);
            final Instant helmCommandStart = Instant.now();
            final HelmCommandResult installWaitNoDebugResult = ExceptionHandlerUtil.actionWithReturn(
                    () -> helmShell.install(InstallOptions.create(chart, releaseName, namespace)
                            .withAdditionalValues(values).withVersion(version).withFiles(files).withArguments(arguments)),
                    String.format("Error while installing the helm chart: '%s' with version '%s'", chart, version));
            if (installWaitNoDebugResult.getReturnValue() != 0) {
                final Instant helmCommandFail = Instant.now();
                LOGGER.error("Helm install command failed in {} minutes:\n{}",
                        (float) Duration.between(helmCommandStart, helmCommandFail).getSeconds() / 60,
                        installWaitNoDebugResult.getOutput());
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Helm history:\n{}", getHelmHistory(releaseName, namespace));
                }
                logJobStatus(namespace);
                throw new TestException("Failed to install chart: '%s' with version: '%s'", chart, version);
            }

            if (helmProperties.isHelmInstallWarningFailTest()) {
                final List<String> warnings = findWarningsInStdout(installWaitNoDebugResult);
                if (!warnings.isEmpty()) {
                    throw new TestException("Warnings detected during helm install: %s", String.join("\n", warnings));
                }
            }

            final Instant helmCommandEnd = Instant.now();
            testStatisticsProvider.get().addTestStatisticsData("Prepare Phase", "helmInstall",
                    this.getClass().toString(),
                    String.valueOf(Duration.between(helmCommandStart, helmCommandEnd).getSeconds()), true);
        }

        public void upgradeHelmChart(final String releaseName, final String chart, final String namespace,
                final Map<String, String> values, final List<File> files,
                final Map<String, String> commandLineParameters, final List<String> commandLineSwitches) {
            JcatLoggingApi.beginStep("Upgrade helm chart");
            LOGGER.info(UPGRADING_HELM_RELEASE_MESSAGE, releaseName, chart);
            final boolean isHelmUpgrade = ExceptionHandlerUtil.actionWithReturn(
                    () -> helmShell.upgrade(releaseName, chart, namespace, values, files, commandLineParameters,
                            commandLineSwitches),
                    String.format(ERROR_WHILE_UPGRADING_MESSAGE, chart));
            if (!isHelmUpgrade) {
                logJobStatus(namespace);
                throw new TestException("Failed to upgrade release: '%s' with chart: '%s'", releaseName, chart);
            }
            JcatLoggingApi.endStep();
        }

        public void deleteChart(final String releaseName, final String namespace) {
            LOGGER.info("Deleting Helm release: '{}'", releaseName);
            try {
                helmShell.deleteReleaseWithTimeout(releaseName, namespace);
            } catch (final HelmException e) {
                throwCouldNotDeleteException(releaseName, e);
            }
        }

        public void rollbackToProvidedRevision(final String releaseName, final int revisionNumber, final String namespace) {
            LOGGER.info("Rollback Helm release: '{}' to revision number chart: '{}'", releaseName, revisionNumber);
            final boolean isRollbackToProvidedRevision = ExceptionHandlerUtil.actionWithReturn(
                    () -> helmShell.rollbackToProvidedRevision(releaseName, revisionNumber, namespace),
                    String.format("Error while rollback the helm release: '%s' to revision: '%s'", releaseName,
                            revisionNumber));
            if (!isRollbackToProvidedRevision) {
                throw new TestException("Failed to rollback release: '%s' to revision: '%s'", releaseName,
                        revisionNumber);
            }
        }

        private void fixTimeoutArgument(Map<String, String> arguments) {
        }

        private String getHelmHistory(String releaseName, String namespace) {
            return "";
        }

        private void logJobStatus(String namespace) {
        }

        private List<String> findWarningsInStdout(HelmCommandResult result) {
            return List.of();
        }

        private void throwCouldNotDeleteException(String releaseName, HelmException e) {
            throw new TestException("Failed to delete release: %s", releaseName);
        }

        private HelmShell helmShell = new HelmShell();
        private HelmProperties helmProperties = new HelmProperties();
        private TestStatisticsProvider testStatisticsProvider = new TestStatisticsProvider();
        private Logger LOGGER = new Logger();
    }

    public static final class DeploymentSteps {
        @CriticalTestStep
        @Given("the Helm chart is removed and redeployed")
        public void helmChartIsRedeployed() {
            reinstallChart();
        }

        @When("the helm chart is installed in namespace {string}{cluster_selector} with the following parameters:")
        public void theHelmChartWithGivenNamespaceIsInstalled(final String namespace,
                final ClustersToExecute clustersToExecute,
                final DataTableWrapper table) {
            final String cluster = clustersToExecute.getCurrentCluster();
            final List<List<String>> headers = DeploymentStepsHelper.getInstallTableHeaders();
            table.verifyHeaders(headers.get(0), headers.get(1));
            timeRegistryClusterMap.get(cluster).put(ADDITIONAL_PCG_TIMESTAMP, ZonedDateTime.now());
            installHelmChart(table, cluster, namespace);
        }

        private void installHelmChart(final DataTableWrapper table, final String currentCluster,
                final String namespace) {
            final List<Map<String, String>> entries = table.getEntries();
            DeploymentStepsHelper.deleteExistingReleases(entries, helmApiClusterMap.get(currentCluster), namespace);
            DeploymentStepsHelper.handleNamespaceDeletion(entries, kubectlApiClusterMap.get(currentCluster), namespace);
            for (final Map<String, String> entry : entries) {
                ActionsHelper.addArmSecretToNamespace(kubectlApiClusterMap.get(currentCluster), PCX_SECRET_NAME,
                        beetsConfiguration, namespace);
                helmChartInstall(entry, namespace, currentCluster);
            }
        }

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

        private void reinstallChart() {
        }

        private void helmChartInstall(Map<String, String> entry, String namespace, String currentCluster) {
        }

        private void installSavedHelmChart() {
        }

        private Map<String, TimeRegistry> timeRegistryClusterMap = Map.of();
        private Map<String, HelmApi> helmApiClusterMap = Map.of();
        private Map<String, KubectlApi> kubectlApiClusterMap = Map.of();
        private BeetsConfiguration beetsConfiguration = new BeetsConfiguration();
        private SutActions sutActionsProvider = new SutActions();
        private ClusterRegistryProvider clusterRegistryProvider = new ClusterRegistryProvider();
        private HelmProperties helmProperties = new HelmProperties();
        private KubectlProperties kubectlProperties = new KubectlProperties();
    }

    public static final class DeploymentStepsHelper {
        public static List<List<String>> getInstallTableHeaders() {
            return List.of(
                    List.of("chart", "release", "version"),
                    List.of("namespace", "values"));
        }

        public static void deleteExistingReleases(List<Map<String, String>> entries, HelmApi helmApi, String namespace) {
            for (Map<String, String> entry : entries) {
                if (entry.containsKey("release")) {
                    helmApi.deleteChart(entry.get("release"), namespace);
                }
            }
        }

        public static void handleNamespaceDeletion(List<Map<String, String>> entries, KubectlApi kubectlApi, String namespace) {
            kubectlApi.deleteNamespace(namespace);
        }
    }

    public static final class ActionsHelper {
        public static void addArmSecretToNamespace(KubectlApi kubectlApi, String secretName,
                BeetsConfiguration beetsConfiguration, String namespace) {
        }

        public static void deleteNamespace(KubectlApi kubectlApi, KubectlProperties kubectlProperties,
                HelmApi helmApi, HelmProperties helmProperties) {
            kubectlApi.deleteNamespace(kubectlProperties.getNamespace());
        }
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
            final ThrowingRunnable restartPods = () -> {
                // Pod restart logic executed during Helm upgrade
            };
        }

        private List<Pattern> getPrefixPatterns(List<String> podPrefixes) {
            return podPrefixes.stream().map(Pattern::compile).collect(Collectors.toList());
        }

        private boolean isHelmActionUpgrade(HelmAction helmAction) {
            return helmAction == HelmAction.UPGRADE;
        }

        @FunctionalInterface
        private interface ThrowingRunnable {
            void run() throws Throwable;
        }
    }

    public enum HelmAction {
        UPGRADE,
        DOWNGRADE,
        INSTALL
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

    public static class InstallOptions {
        private final String chart;
        private final String releaseName;
        private final String namespace;
        private Map<String, String> values;
        private String version;
        private List<File> files;
        private Map<String, String> arguments;

        private InstallOptions(String chart, String releaseName, String namespace) {
            this.chart = chart;
            this.releaseName = releaseName;
            this.namespace = namespace;
        }

        public static InstallOptions create(String chart, String releaseName, String namespace) {
            return new InstallOptions(chart, releaseName, namespace);
        }

        public InstallOptions withAdditionalValues(Map<String, String> values) {
            this.values = values;
            return this;
        }

        public InstallOptions withVersion(String version) {
            this.version = version;
            return this;
        }

        public InstallOptions withFiles(List<File> files) {
            this.files = files;
            return this;
        }

        public InstallOptions withArguments(Map<String, String> arguments) {
            this.arguments = arguments;
            return this;
        }
    }

    public static class HelmCommandResult {
        private final int returnValue;
        private final String output;

        public HelmCommandResult(int returnValue, String output) {
            this.returnValue = returnValue;
            this.output = output;
        }

        public int getReturnValue() {
            return returnValue;
        }

        public String getOutput() {
            return output;
        }
    }

    public interface HelmShell {
        default HelmCommandResult install(InstallOptions options) {
            return new HelmCommandResult(0, "");
        }

        default boolean upgrade(String releaseName, String chart, String namespace, Map<String, String> values,
                List<File> files, Map<String, String> commandLineParameters, List<String> commandLineSwitches) {
            return true;
        }

        default void deleteReleaseWithTimeout(String releaseName, String namespace) throws HelmException {
        }

        default boolean rollbackToProvidedRevision(String releaseName, int revisionNumber, String namespace) {
            return true;
        }
    }

    public interface HelmProperties {
        default boolean isHelmInstallWarningFailTest() {
            return false;
        }

        default String getRelease() {
            return "test-release";
        }
    }

    public interface HelmException extends RuntimeException {
    }

    public interface ExceptionHandlerUtil {
        static <T> T actionWithReturn(ThrowingSupplier<T> action, String errorMessage) {
            try {
                return action.get();
            } catch (Throwable t) {
                throw new TestException(errorMessage);
            }
        }

        @FunctionalInterface
        interface ThrowingSupplier<T> {
            T get() throws Throwable;
        }
    }

    public interface TestStatisticsProvider {
        default TestStatisticsProvider get() {
            return this;
        }

        default void addTestStatisticsData(String phase, String action, String className, String duration, boolean success) {
        }
    }

    public interface Logger {
        default void info(String message, Object... args) {
        }

        default void error(String message, Object... args) {
        }

        default boolean isInfoEnabled() {
            return true;
        }
    }

    public interface JcatLoggingApi {
        static void beginStep(String stepName) {
        }

        static void endStep() {
        }
    }

    public interface DataTableWrapper {
        void verifyHeaders(List<String> header1, List<String> header2);

        void verifyHeader(String header);

        List<Map<String, String>> getEntries();
    }

    public interface ClustersToExecute {
        String getCurrentCluster();
    }

    public interface TimeRegistry {
        void put(String key, ZonedDateTime value);
    }

    public interface HelmApi {
        void deleteChart(String releaseName, String namespace);
    }

    public interface KubectlApi {
        void deleteNamespace(String namespace);
    }

    public interface KubectlProperties {
        String getNamespace();
    }

    public interface BeetsConfiguration {
        Configuration getConfiguration();
    }

    public interface Configuration {
    }

    public interface SutActions {
        void setupUsersWithIntermediatePassword();
    }

    public interface ClusterRegistryProvider {
        String getActiveCluster();
    }

    public interface TestStep {
    }

    public interface Given {
        String value();
    }

    public interface When {
        String value();
    }

    public interface GeoredCriticalTestStep {
    }

    public interface CriticalTestStep {
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

        public void error(String message, Object... args) {
        }

        public boolean isInfoEnabled() {
            return true;
        }
    }
}
