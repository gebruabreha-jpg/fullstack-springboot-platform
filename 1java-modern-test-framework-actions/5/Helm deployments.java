Complete Java Code: Core Helm Actions
Helm.java — Key Methods
@Override
public void installHelmChart(final String chart, final String releaseName, final String namespace,
        final Map<String, String> values, final String version) {
    installHelmChart(chart, releaseName, namespace, values, null, null, version);
}

@Override
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

@Override
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

@Override
public void deleteChart(final String releaseName, final String namespace) {
    LOGGER.info("Deleting Helm release: '{}'", releaseName);
    try {
        helmShell.deleteReleaseWithTimeout(releaseName, namespace);
    } catch (final HelmException e) {
        throwCouldNotDeleteException(releaseName, e);
    }
}

@Override
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
DeploymentSteps.java — Step Definitions
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
UpgradeSteps.java — Helm Upgrade with Pod Restart Coordination
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
    final ThrowingRunnable restartPods = () -> { /* pod restart logic */ };
    // ... helm upgrade preparation + pod restart coordination
}