 Task 6: Helm Deployment Actions — Complete Code

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  PCC Test Framework — HelmApi Interface (Key Methods)

  // File: com.ericsson.pcc.integration.helm.HelmApi

  public interface HelmApi extends SimpleFetchLog {

      /** helm install <chart> --name <release> -n <namespace> --set key=val --version <ver> */
      void installHelmChart(String chart, String releaseName, String namespace,
              Map<String, String> values, String version);

      /** helm install with values files + additional arguments */
      void installHelmChart(String chart, String releaseName, String namespace,
              Map<String, String> values, List<File> files,
              Map<String, String> arguments, String version);

      /** helm upgrade <release> <chart> -n <namespace> --set ... -f ... */
      void upgradeHelmChart(String releaseName, String chart, String namespace,
              Map<String, String> values, List<File> files,
              Map<String, String> commandLineParameters, List<String> commandLineSwitches);

      /** helm upgrade --no-debug (suppresses debug output) */
      void upgradeHelmChartNoDebug(String releaseName, String chart, String namespace,
              Map<String, String> values, List<File> files,
              Map<String, String> commandLineParameters, List<String> commandLineSwitches);

      /** helm upgrade --atomic (auto-rollback on failure) */
      boolean upgradeHelmChartAtomic(String releaseName, String chart, String namespace,
              Map<String, String> values, List<File> files,
              Map<String, String> commandLineParameters, List<String> commandLineSwitches);

      /** helm uninstall <release> -n <namespace> */
      void deleteChart(String releaseName, String namespace);

      /** helm uninstall with additional arguments */
      void deleteChart(String releaseName, String namespace, Map<String, String> arguments);

      /** helm rollback <release> <revision> -n <namespace> */
      void rollbackToProvidedRevision(String releaseName, int revisionNumber, String namespace);

      /** helm list --all */
      List<HelmRelease> getAllReleases();

      /** helm list -n <namespace> */
      List<HelmRelease> getReleasesInNamespace(String namespace);

      /** helm status -n <namespace> <release> */
      String getHelmStatus(String releaseName, String namespace);

      /** helm history <release> -n <namespace> */
      String getHelmHistory(String releaseName, String namespace);

      /** helm get values <release> -n <namespace> -o json */
      String getHelmValues(String release, String namespace);

      /** helm get manifest <release> -n <namespace> */
      String getManifest(String release, String namespace);

      /** helm fetch <chart> --version <ver> -d <dest> */
      File fetchHelmChart(String chart, String version, File destination, boolean untar);

      /** helm repo update */
      void repoUpdate();

      /** helm test -n <namespace> <release> */
      String getHelmTest(String releaseName, String namespace);
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  PCC Test Framework — DeploymentSteps.java (PCC TF Pattern)

  // File: DeploymentSteps.java (PCC TF — direct HelmApi usage)

  // --- Install with set values ---
  // Gherkin: When helm chart is installed with additional set flags:
  //   | key                    | value         |
  //   | global.pullSecret      | regcred       |
  //   | persistence.size       | 10Gi          |
  @TestStep
  @When("helm chart is installed with additional set flags:")
  public void installHelmChartWithSetFlags(final DataTableWrapper table) {
      final Map<String, String> values = new HashMap<>();
      for (Map<String, String> row : table.getEntries()) {
          values.put(row.get("key"), row.get("value"));
      }
      helmApi.installHelmChart(helmProperties.getChart(), helmProperties.getRelease(),
              kubectlProperties.getNamespace(), values, helmProperties.getVersion());
  }

  // --- Upgrade with set values ---
  @TestStep
  @When("helm chart is upgraded with additional set flags:")
  public void upgradeHelmChart(final DataTableWrapper table) {
      final Map<String, String> values = new HashMap<>();
      for (Map<String, String> row : table.getEntries()) {
          values.put(row.get("key"), row.get("value"));
      }
      helmApi.upgradeHelmChart(helmProperties.getRelease(), helmProperties.getChart(),
              kubectlProperties.getNamespace(), values, List.of(),
              Map.of("timeout", "600s"), List.of("--install"));
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework — DeploymentSteps.java (Helm Install in Namespace)

  // File: DeploymentSteps.java (Beets — table-driven install)

  // Gherkin: When the helm chart is installed in namespace "pcg2" with the following parameters:
  //   | Release Name | Chart Name       | Values                                         | Values Files     | Package Files         | Arguments   |
  //   | pcg2         | eric-pc-gateway  | mongo.persistence.storageClass="network-block" | helm_values.yaml | values.minimal.yaml   | timeout=200 |
  @GeoredCriticalTestStep
  @When("the helm chart is installed in namespace {string}{cluster_selector} with the following parameters:")
  public void theHelmChartWithGivenNamespaceIsInstalled(final String namespace,
          final ClustersToExecute clustersToExecute, final DataTableWrapper table) {
      final String cluster = clustersToExecute.getCurrentCluster();
      table.verifyHeaders(DeploymentStepsHelper.getInstallTableHeaders().get(0),
              DeploymentStepsHelper.getInstallTableHeaders().get(1));
      installHelmChart(table, cluster, namespace);
  }

  private void installHelmChart(final DataTableWrapper table, final String cluster, final String namespace) {
      final List<Map<String, String>> entries = table.getEntries();
      // Delete existing releases if present
      DeploymentStepsHelper.deleteExistingReleases(entries, helmApiClusterMap.get(cluster), namespace);
      // Handle namespace deletion/recreation
      DeploymentStepsHelper.handleNamespaceDeletion(entries, kubectlApiClusterMap.get(cluster), namespace);
      // Install each entry
      for (final Map<String, String> entry : entries) {
          ActionsHelper.addArmSecretToNamespace(kubectlApiClusterMap.get(cluster), PCX_SECRET_NAME,
                  beetsConfiguration, namespace);
          helmChartInstall(entry, namespace, cluster);
      }
  }

  private void helmChartInstall(final Map<String, String> entry, final String namespace, final String cluster) {
      final HelmApplication helmApp = helmApplicationHandler.getHelmApplication();
      final String version = helmApp.getVersion();
      final Path productChartPath = helmApp.getComponentPath(HelmApplication.Component.PRODUCT_CHART);
      final Path supportingFilesPath = helmApp.getComponentPath(HelmApplication.Component.SUPPORTING_FILES);

      // Build values files from table entry + product chart + supporting files
      final List<File> valuesFiles = DeploymentStepsHelper.buildValuesFiles(entry,
              productChartPath, supportingFilesPath, beetsConfiguration.getConfiguration());

      final Map<String, String> additionalValues = new HashMap<>();
      additionalValues.put("global.pullSecret", PCX_SECRET_NAME);

      DeploymentStepsHelper.installChart(helmApiClusterMap.get(cluster), entry, namespace,
              version, productChartPath.toFile(), valuesFiles, additionalValues);
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework — Remove and Redeploy

  // File: DeploymentSteps.java — Clean reinstall

  // Gherkin: Given the Helm chart is removed and redeployed
  @CriticalTestStep
  @Given("the Helm chart is removed and redeployed")
  public void helmChartIsRedeployed() {
      reinstallChart();
  }

  private void reinstallChart() {
      final String cluster = clusterRegistryProvider.get().getActiveCluster();
      final HelmApi helmApi = helmApiClusterMap.get(cluster);

      // 1. Uninstall existing release
      helmApi.deleteChart(helmProperties.getRelease(), kubectlProperties.getNamespace());

      // 2. Delete namespace
      ActionsHelper.deleteNamespace(kubectlApiClusterMap.get(cluster), kubectlProperties, helmApi, helmProperties);

      // 3. Fresh install
      installSavedHelmChart();
  }

  // Gherkin: When saved helm chart is reinstalled
  @TestStep
  @When("saved helm chart is reinstalled")
  public void reinstallSavedHelmChart() {
      final String cluster = clusterRegistryProvider.get().getActiveCluster();

      // 1. Delete existing release
      helmApiClusterMap.get(cluster).deleteChart(helmProperties.getRelease(), kubectlProperties.getNamespace());
      ActionsHelper.deleteNamespace(kubectlApiClusterMap.get(cluster), kubectlProperties,
              helmApiClusterMap.get(cluster), helmProperties);

      // 2. Install original build
      installSavedHelmChart();

      // 3. Setup users
      sutActionsProvider.get().setupUsersWithIntermediatePassword();

      // 4. Restore certificates
      restoreCertificateFilesOnPCSM(temporaryAdpCmProvider);

      // 5. Restore configuration
      restoreConfigurationUsingIntermediatePassword(temporaryAdpCmProvider);

      // 6. Change password
      sutActionsProvider.get().changeUserPasswordOnFirstLogin();

      // 7. Verify pods are up
      verifyExpectedPodsAreUp(cluster, new ArrayList<>());
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework — UpgradeSteps.java (Helm Upgrade with Concurrent Pod Restarts)

  // File: UpgradeSteps.java — Helm upgrade + pod restart concurrently

  // Gherkin: When Helm chart is upgraded while 10 pods from the following prefixes are restarted with 1 seconds interval:
  //   | Pod prefixes           |
  //   | eric-pc-routing-engine |
  //   | eric-sec               |
  //   | eric-pc-up             |
  @GeoredCriticalTestStep
  @When("Helm chart is {helm_action}{cluster_selector} while {int} pods from the following prefixes are restarted{cluster_selector} with {int} seconds interval:")
  public void helmUpgradeWithRestartingPods(final HelmAction helmAction,
          final ClustersToExecute upgradeClustersToExecute, final int numberOfPods,
          final ClustersToExecute podsRestartedClusterToExecute, final int restartInterval,
          final DataTableWrapper prefixesTable) {

      final HelmInstallOptions helmInstallOptions = new HelmInstallOptions.Builder()
              .withShouldAddHelmValuesFile()
              .withHelmAction(helmAction)
              .withUpdateCrdRelease()
              .build();

      helmUpgradeWithRestartingPods(upgradeClustersToExecute, numberOfPods,
              podsRestartedClusterToExecute, restartInterval, prefixesTable, helmInstallOptions);
  }

  private void helmUpgradeWithRestartingPods(final ClustersToExecute upgradeClusters,
          final int numberOfPods, final ClustersToExecute restartClusters,
          final int restartInterval, final DataTableWrapper prefixesTable,
          final HelmInstallOptions options) {

      final String upgradeCluster = upgradeClusters.getCurrentCluster();
      final String restartCluster = restartClusters.getCurrentCluster();

      // Build pod list from prefixes table
      final List<String> prefixes = prefixesTable.getEntries().stream()
              .map(row -> row.get("Pod prefixes"))
              .toList();

      // Start concurrent tasks:
      // Thread 1: Helm upgrade
      // Thread 2: Kill random pods at interval
      final CompletableFuture<Void> upgradeFuture = CompletableFuture.runAsync(() ->
              performHelmUpgrade(upgradeCluster, options));

      final CompletableFuture<Void> restartFuture = CompletableFuture.runAsync(() ->
              restartRandomPods(restartCluster, prefixes, numberOfPods, restartInterval));

      CompletableFuture.allOf(upgradeFuture, restartFuture).join();

      // Wait for all pods to come up after upgrade + restarts
      K8sPodPerformer.waitAllPodsComeUp(kubectlApiClusterMap.get(upgradeCluster),
              Duration.ofMinutes(15), Duration.ofSeconds(30), kubectlProperties.getIgnorePodPrefix());
  }

  private void restartRandomPods(final String cluster, final List<String> prefixes,
          final int numberOfPods, final int intervalSeconds) {
      final KubectlApi kubectl = kubectlApiClusterMap.get(cluster);
      for (int i = 0; i < numberOfPods; i++) {
          final String randomPrefix = prefixes.get(RandomUtil.random.nextInt(prefixes.size()));
          final Pod pod = K8sPodRetriever.getRunningPodMatchingFilter(
                  PodFilterType.podWithNameStartingWithPrefix(randomPrefix),
                  cluster, null, kubectlApiClusterMap);
          PodAction.RESTARTED.execute(k8sNodeApisClusterMapProvider.get().get(cluster),
                  kubectl, kubectlProperties, pod.getMetadata().getName());
          Delay.sleep(Duration.ofSeconds(intervalSeconds));
      }
  }