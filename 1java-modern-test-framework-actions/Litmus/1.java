 Task 5: Litmus Chaos Experiments — Complete Code

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  LitmusExperimentsActions Interface (Contract)

  // File: com.ericsson.pcc.integration.litmus.LitmusExperimentsActions

  public interface LitmusExperimentsActions {

      /** Apply RBAC (ServiceAccount, Role, RoleBinding) for the experiment */
      void applyRbac(final String file, final String experiment);

      /** Create the ChaosExperiment CRD */
      void createExperiment(final String file, final String experiment);

      /** Start the ChaosEngine to trigger execution */
      void startEngine(final String file, final String experiment);

      /** Collect terminated pod logs + describe ChaosEngine/ChaosResults */
      void collectPodLogs(final String podPrefix);

      /** Validate that ChaosResult verdict is 'Pass' */
      void validateChaosResult(final String experiment);

      /** Delete experiment YAML files from cluster */
      void deleteExperimentFiles(final String file, final String experiment);

      /** Generate summary CSV tables */
      void prepareAndStoreLitmusExperimentsSummaryTables();
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  LitmusExperimentsActionsImpl (Execution via kubectl apply)

  // File: LitmusExperimentsActionsImpl.java — Applies each YAML via kubectl

  public class LitmusExperimentsActionsImpl implements LitmusExperimentsActions {

      private static final String CHAOS_RESULTS_LITMUSCHAOS_RESOURCE_TYPE = "chaosresults.litmuschaos.io";

      private final KubectlApi kubectlApi;
      private final FetchLogHelper fetchLogHelper;
      private final String currentCluster;

      @Inject
      public LitmusExperimentsActionsImpl(final KubectlApi kubectlApi, final FetchLogHelper fetchLogHelper,
              final String currentCluster) {
          this.kubectlApi = kubectlApi;
          this.fetchLogHelper = fetchLogHelper;
          this.currentCluster = currentCluster;
      }

      @Override
      public void applyRbac(final String rbacFile, final String experiment) {
          // kubectl apply -f pod_cpu_hog_rbac.yaml -n <namespace>
          kubectlApi.apply(rbacFile, kubectlApi.getNamespace());
      }

      @Override
      public void createExperiment(final String experimentFile, final String experiment) {
          // kubectl apply -f pod_cpu_hog_experiment.yaml -n <namespace>
          kubectlApi.apply(experimentFile, kubectlApi.getNamespace());
      }

      @Override
      public void startEngine(final String engineFile, final String experiment) {
          // kubectl apply -f pod_cpu_hog_engine.yaml -n <namespace>
          kubectlApi.apply(engineFile, kubectlApi.getNamespace());
      }

      @Override
      public void collectPodLogs(final String podPrefix) {
          Delay.sleep(Duration.ofSeconds(10));
          // Get completed experiment pod logs
          getPodLogs(podPrefix);
          // Describe ChaosEngine and ChaosResults CRDs
          describeAndSaveChaos("Chaosengines", "chaos_engine_describe");
          describeAndSaveChaos("Chaosresults", "chaos_results_describe");
      }

      @Override
      public void validateChaosResult(final String experiment) {
          // kubectl get chaosresults.litmuschaos.io -n <namespace> -o wide
          final String outputResult = kubectlApi.kubectl(String.format("get %s -n %s -o wide",
                  CHAOS_RESULTS_LITMUSCHAOS_RESOURCE_TYPE, kubectlApi.getNamespace()));
          final String litmusLabelsName = experiment.replace('_', '-');
          Arrays.stream(outputResult.split(System.lineSeparator()))
                  .filter(line -> line.contains(litmusLabelsName))
                  .forEach(line -> {
                      final String experimentName = line.split(" ")[0];
                      JcatAssertApi.saveAssertTrue(
                              String.format("Chaos result for '%s' is not Pass", experimentName),
                              checkChaosResult(experimentName));
                  });
      }

      private boolean checkChaosResult(final String experimentName) {
          // kubectl describe chaosresults.litmuschaos.io <name> -n <namespace>
          final String describe = kubectlApi.kubectl(String.format("describe %s %s -n %s",
                  CHAOS_RESULTS_LITMUSCHAOS_RESOURCE_TYPE, experimentName, kubectlApi.getNamespace()));
          final Matcher matcher = Pattern.compile(".Verdict:\\s*(\\w+)").matcher(describe);
          return matcher.find() && "Pass".equalsIgnoreCase(matcher.group(1));
      }

      @Override
      public void deleteExperimentFiles(final String fileName, final String experiment) {
          // kubectl delete -f <file>
          kubectlApi.deleteFile(fileName);
      }

      private void describeAndSaveChaos(final String resourceType, final String fileName) {
          final String describeOutput = kubectlApi.describeResource(resourceType, "");
          LoggingHelper.createFileAndOverwriteContentVerbose(fetchLogHelper.getTestCaseLogDir(),
                  describeOutput, fileName, String.format("saved_logs/Litmus/%s", currentCluster), true);
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  LitmusHelper.java — Litmus Installation

  // File: LitmusHelper.java — Installs Litmus via Helm chart

  public class LitmusHelper {

      private final LitmusProperties litmusProperties;
      private final KubectlApiClusterMap kubectlApiClusterMap;
      private final HelmApi helmApi;
      private final ArtifactoryApi artifactoryApi;

      /**
       * Install Litmus ChaosCenter via Helm.
       * Steps:
       *   1. Delete existing namespace if present
       *   2. Create fresh namespace
       *   3. Fetch Litmus Helm chart from Artifactory
       *   4. helm install with storageClass values
       */
      public void installLitmus(final String cluster) {
          final String chartName = litmusProperties.getChartName();
          final String namespace = litmusProperties.getNamespace();
          final String chartVersion = litmusProperties.getChartVersion();
          final String chartRelease = litmusProperties.getChartRelease();
          final KubectlApi kubectlApi = kubectlApiClusterMap.get(cluster);

          // Clean up if already installed
          if (kubectlApi.isNamespaceExists(namespace)) {
              if (HelmChartHelper.isHelmChartExists(chartRelease, helmApi)) {
                  helmApi.deleteChart(chartRelease, namespace);
              }
              kubectlApi.deleteNamespace(namespace);
          }

          // Fresh install
          kubectlApi.createNamespace(namespace);
          kubectlApi.setNamespace(namespace);

          final File chartDir = new File(TemporaryDirectoryProvider.getTemporaryDirectory() + "/" + chartName);
          chartDir.mkdir();
          final String helmChartGlob = String.format("%s/%<s-%s.tgz", chartName, chartVersion);
          final File chartPath = HelmChartHelper.fetchAndUntarHelmChart(
                  artifactoryApi, chartDir, helmChartGlob, chartName, litmusProperties.getRepoName());

          final Map<String, String> values = Map.of("mongo.persistence.storageClass", "network-block");
          helmApi.installHelmChart(chartPath.toString(), chartRelease, namespace, values, chartVersion);
      }

      /**
       * Install Litmus operator (CRDs for ChaosEngine, ChaosExperiment, ChaosResult).
       */
      public void installLitmusOperator(final String operatorFile, final String cluster) {
          kubectlApiClusterMap.get(cluster).apply(operatorFile, litmusProperties.getNamespace());
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  LitmusExperimentSteps.java — Full Workflow Orchestration

  // File: LitmusExperimentSteps.java — Complete Litmus workflow steps

  @ScenarioScoped
  public class LitmusExperimentSteps implements TestStepDefinition {

      // --- 1. Install Litmus ---
      @GeoredTestStep
      @When("litmus is installed{cluster_selector}")
      public void installLitmus(final ClustersToExecute clustersToExecute) {
          litmusHelper.installLitmus(clustersToExecute.getCurrentCluster());
      }

      // --- 2. Install Litmus Operator ---
      @GeoredTestStep
      @When("the litmus operator is installed{cluster_selector}")
      public void installLitmusOperator(final ClustersToExecute clustersToExecute) {
          final String operatorFile = beetsConfigurationProvider.get().getConfiguration()
                  .getFile(LITMUS_OPERATOR_FILE);
          litmusHelper.installLitmusOperator(operatorFile, clustersToExecute.getCurrentCluster());
      }

      // --- 3. Configure Experiment Engine (replace placeholders in template) ---
      @TestStep
      @When("the litmus engine {} is configured:")
      public void configureLitmusExperimentEngine(final LitmusExperiments experiment, final DataTableWrapper table) {
          table.verifyHeader("variable name", "value");
          final String engineTemplateFile = beetsConfigurationProvider.get().getConfiguration()
                  .getFile(experiment.getEngineTemplateFile());
          final String engineFile = beetsConfigurationProvider.get().getConfiguration()
                  .getFile(experiment.getEngineFile());
          final Map<String, String> propertyMap = new HashMap<>();
          for (final Map<String, String> entry : table.getEntries()) {
              propertyMap.put(entry.get("variable name"), entry.get("value"));
          }
          // Replace {{ placeholders }} in template → write to engine file
          ConfigurationHelper.replacePlaceholdersInConfigFile(
                  new File(engineTemplateFile), propertyMap, false, new File(engineFile));
      }

      // --- 4. Run Experiment (RBAC → Experiment CRD → Engine CRD) ---
      @GeoredTestStep
      @When("the litmus experiment {} is run{cluster_selector}")
      public void runLitmusExperiments(LitmusExperiments experiment, final ClustersToExecute clustersToExecute) {
          final String cluster = clustersToExecute.getCurrentCluster();
          final LitmusExperimentsActions litmusActions = mappedLitmusExperimentsActions.get(cluster);

          // Apply RBAC
          litmusActions.applyRbac(
                  beetsConfigurationProvider.get().getConfiguration().getFile(experiment.getRbacFile()),
                  experiment.getExperimentName());

          // Create experiment definition
          litmusActions.createExperiment(
                  beetsConfigurationProvider.get().getConfiguration().getFile(experiment.getExperimentFile()),
                  experiment.getExperimentName());

          // Start engine (triggers experiment)
          litmusActions.startEngine(
                  beetsConfigurationProvider.get().getConfiguration().getFile(experiment.getEngineFile()),
                  experiment.getExperimentName());
      }

      // --- 5. Collect Logs ---
      @GeoredTestStep
      @When("pod logs and describe outputs for the experiment {} is collected{cluster_selector}")
      public void collectPodLogsAndDescribeOutput(LitmusExperiments experiment,
              final ClustersToExecute clustersToExecute) {
          mappedLitmusExperimentsActions.get(clustersToExecute.getCurrentCluster())
                  .collectPodLogs(experiment.getPodPrefix());
      }

      // --- 6. Validate Result ---
      @GeoredTestStep
      @Then("the litmus experiment {} result is validated{cluster_selector}")
      public void validateLitmusExperiment(LitmusExperiments experiment,
              final ClustersToExecute clustersToExecute) {
          mappedLitmusExperimentsActions.get(clustersToExecute.getCurrentCluster())
                  .validateChaosResult(experiment.getExperimentName());
      }

      // --- 7. Cleanup ---
      @GeoredTestStep
      @When("the litmus experiment {} files are deleted{cluster_selector}")
      public void deleteLitmusExperimentFiles(LitmusExperiments experiment,
              final ClustersToExecute clustersToExecute) {
          final LitmusExperimentsActions litmusActions = mappedLitmusExperimentsActions
                  .get(clustersToExecute.getCurrentCluster());
          litmusActions.deleteExperimentFiles(
                  beetsConfigurationProvider.get().getConfiguration().getFile(experiment.getRbacFile()),
                  experiment.getExperimentName());
          litmusActions.deleteExperimentFiles(
                  beetsConfigurationProvider.get().getConfiguration().getFile(experiment.getExperimentFile()),
                  experiment.getExperimentName());
          litmusActions.deleteExperimentFiles(
                  beetsConfigurationProvider.get().getConfiguration().getFile(experiment.getEngineFile()),
                  experiment.getExperimentName());
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  ChaosEngine YAML Templates

  pod_cpu_hog_engine.template:

  apiVersion: litmuschaos.io/v1alpha1
  kind: ChaosEngine
  metadata:
    name: {{ experiment_name }}
  spec:
    engineState: 'active'
    appinfo:
      applabel: '{{ applabel }}'
      appkind: 'deployment'
    chaosServiceAccount: pod-cpu-hog-sa
    experiments:
      - name: pod-cpu-hog
        spec:
          components:
            env:
              - name: TOTAL_CHAOS_DURATION
                value: '{{ chaos_duration }}'
              - name: CPU_CORES
                value: '{{ cpu_core }}'
              - name: CPU_LOAD
                value: '{{ cpu_load }}'
              - name: PODS_AFFECTED_PERC
                value: '{{ affected_pods }}'
              - name: SEQUENCE
                value: '{{ sequence }}'

  pod_network_loss_engine.template:

  apiVersion: litmuschaos.io/v1alpha1
  kind: ChaosEngine
  metadata:
    name: {{ experiment_name }}
  spec:
    engineState: 'active'
    appinfo:
      applabel: '{{ applabel }}'
      appkind: 'deployment'
    chaosServiceAccount: pod-network-loss-sa
    experiments:
      - name: pod-network-loss
        spec:
          components:
            env:
              - name: TOTAL_CHAOS_DURATION
                value: '{{ chaos_duration }}'
              - name: NETWORK_PACKET_LOSS_PERCENTAGE
                value: '{{ network_packet_loss_percentage }}'
              - name: PODS_AFFECTED_PERC
                value: '{{ affected_pods }}'
              - name: SEQUENCE
                value: '{{ sequence }}'