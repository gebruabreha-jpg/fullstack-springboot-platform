Task 4: Pod Restart Control — Complete Code

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  PodAction Enum (The Core)

  // File: com.ericsson.pcc.integration.kubectl.enums.PodAction

  public enum PodAction {

      /**
       * Graceful delete: kubectl delete pod <name>
       */
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

      /**
       * Force delete: kubectl delete pod <name> --force --grace-period=0
       */
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

      /**
       * Restart by killing ALL containers in the pod (pod auto-restarts via controller).
       * This is NOT kubectl delete — it's a container-level kill that triggers CrashLoopBackOff/restart.
       */
      RESTARTED {
          @Override
          public void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
                  final KubectlProperties kubectlProperties, final String podName) {
              // Get all containers and their IDs
              final List<KillRequest> killRequests;
              final String nodeName;
              synchronized (KubeCtl.class) {
                  killRequests = Arrays.stream(kubectlApi.getContainers(podName))
                          .map(containerName -> new KillRequest(podName, containerName,
                                  kubectlApi.getContainerId(podName, containerName), KillSignal.SIGKILL))
                          .toList();
                  nodeName = kubectlApi.getNodeName(podName);
              }

              // Kill all containers on the hosting node
              final K8sNodeApi k8sNodeApi = k8sNodeApis.getWithHostname(nodeName);
              k8sNodeApi.killContainers(killRequests);

              // Wait until K8s notices the restart
              final Duration restartTimeout = k8sNodeApi.getContainerRestartTimeout();
              final boolean restartFinished = Poll.isActionCompleted(
                      () -> !kubectlApi.checkResourcesUp(podName),
                      restartTimeout, Duration.ofSeconds(1));
              JcatAssertApi.assertTrue(
                      String.format("RESTARTED action not finished within '%d' seconds",
                              restartTimeout.getSeconds()),
                      restartFinished);
          }
      };

      public static PodAction fromString(final String s) {
          return valueOf(s.toUpperCase().replace(" ", "_"));
      }

      public abstract void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
              final KubectlProperties kubectlProperties, String podName);
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  K8sPodManagementSteps.java — Step Definitions

  // File: K8sPodManagementSteps.java — Pod action step definitions

  @ScenarioScoped
  public class K8sPodManagementSteps implements TestStepDefinition {

      // --- STEP 1: Single pod by prefix ---
      // Gherkin: When a pod with prefix "eric-pc-sm" is deleted
      //          When a pod with prefix "eric-pc-sm" is restarted
      @GeoredTestStep
      @When("a pod with prefix {string} is {pod_action}{cluster_selector}")
      public void runActionOnPodWithPrefix(final String prefix, final PodAction podAction,
              final ClustersToExecute clustersToExecute) {
          final String cluster = clustersToExecute.getCurrentCluster();
          PodRole role = null;
          if (prefix.contains(K8sConstants.Controller.ERIC_PC_SM_CONTROLLER)) {
              role = PodRole.ACTIVE;
          }
          final Pod foundPod = K8sPodRetriever.getRunningPodMatchingFilter(
                  PodFilterType.podWithNameStartingWithPrefix(prefix), cluster, role, kubectlApiClusterMap);
          K8sPodPerformer.runActionOnPod(foundPod, podAction, cluster,
                  k8sNodeApisClusterMapProvider.get(), kubectlApiClusterMap, kubectlProperties, fetchLogHelper);
      }

      // --- STEP 2: Multiple pods by prefix with count ---
      // Gherkin: When pods with prefix are deleted:
      //   | Number of pods | Prefix           |
      //   | 2              | eric-pc-sm       |
      //   | all            | eric-pc-kvdb-rd  |
      @GeoredTestStep
      @When("pods with prefix are {pod_action}{cluster_selector}:")
      public void runActionOnMultiplePodsWithPrefixes(final PodAction podAction,
              final ClustersToExecute clustersToExecute, final DataTableWrapper table) {
          table.verifyHeaders(
                  List.of(K8sConstants.Table.TABLE_HEADER_NR_OF_PODS, K8sConstants.Table.TABLE_HEADER_PREFIX),
                  List.of(K8sConstants.Table.ROLE));
          final String cluster = clustersToExecute.getCurrentCluster();
          final List<Pod> foundPods = new ArrayList<>();
          for (final Map<String, String> row : table.getEntries()) {
              final int expectedNumberOfPods = K8sPodRetriever.stringToExpectedNumberOfPods(
                      row.get(K8sConstants.Table.TABLE_HEADER_NR_OF_PODS));
              final String prefix = row.get(K8sConstants.Table.TABLE_HEADER_PREFIX);
              final PodRole role = K8sPodRetriever.getPodRoleFromRow(row, prefix);
              foundPods.addAll(K8sPodRetriever.getRunningPodsMatchingFilter(
                      PodFilterType.podWithNameStartingWithPrefix(prefix),
                      expectedNumberOfPods, cluster, kubectlApiClusterMap));
          }
          K8sPodPerformer.runActionOnPods(foundPods, podAction, cluster,
                  k8sNodeApisClusterMapProvider.get(), kubectlApiClusterMap, kubectlProperties, fetchLogHelper);
      }

      // --- STEP 3: Multiple pods by name pattern (regex) ---
      // Gherkin: When pods with name matching specified pattern are deleted:
      //   | Number of pods | Pod name pattern       |
      //   | 1              | eric-pc-sm-controller.* |
      @GeoredTestStep
      @When("pods with name matching specified pattern are {pod_action}{cluster_selector}:")
      public void runActionOnMultiplePodsWithPattern(final PodAction podAction,
              final ClustersToExecute clustersToExecute, final DataTableWrapper table) {
          table.verifyHeaders(
                  List.of(K8sConstants.Table.TABLE_HEADER_NR_OF_PODS, K8sConstants.Table.TABLE_HEADER_POD_NAME_PATTERN),
                  List.of(K8sConstants.Table.ROLE));
          final String cluster = clustersToExecute.getCurrentCluster();
          final List<Pod> foundPods = new ArrayList<>();
          for (final Map<String, String> row : table.getEntries()) {
              final int expectedNumberOfPods = K8sPodRetriever.stringToExpectedNumberOfPods(
                      row.get(K8sConstants.Table.TABLE_HEADER_NR_OF_PODS));
              final String pattern = row.get(K8sConstants.Table.TABLE_HEADER_POD_NAME_PATTERN);
              final PodRole role = K8sPodRetriever.getPodRoleFromRow(row, pattern);
              foundPods.addAll(K8sPodRetriever.getRunningPodsMatchingFilter(
                      PodFilterType.podWithNameMatchingPattern(pattern),
                      expectedNumberOfPods, cluster, kubectlApiClusterMap));
          }
          K8sPodPerformer.runActionOnPods(foundPods, podAction, cluster,
                  k8sNodeApisClusterMapProvider.get(), kubectlApiClusterMap, kubectlProperties, fetchLogHelper);
      }

      // --- STEP 4: Pods by resource (Deployment/StatefulSet) ---
      // Gherkin: When pods controlled by resource are restarted:
      //   | Number of pods | Resource         |
      //   | 1              | eric-pc-sm-ds    |
      @GeoredTestStep
      @When("pods controlled by resource are {pod_action}{cluster_selector}:")
      public void runActionOnMultiplePodsControlledByResource(final PodAction podAction,
              final ClustersToExecute clustersToExecute, final DataTableWrapper table) {
          table.verifyHeaders(
                  List.of(K8sConstants.Table.TABLE_HEADER_NR_OF_PODS, K8sConstants.Table.TABLE_HEADER_RESOURCE),
                  List.of(K8sConstants.Table.ROLE));
          final String cluster = clustersToExecute.getCurrentCluster();
          final List<Pod> foundPods = new ArrayList<>();
          for (final Map<String, String> row : table.getEntries()) {
              final int expectedNumberOfPods = K8sPodRetriever.stringToExpectedNumberOfPods(
                      row.get(K8sConstants.Table.TABLE_HEADER_NR_OF_PODS));
              final String resource = row.get(K8sConstants.Table.TABLE_HEADER_RESOURCE);
              final PodRole role = K8sPodRetriever.getPodRoleFromRow(row, resource);
              foundPods.addAll(K8sPodRetriever.getRunningPodsMatchingFilter(
                      PodFilterType.podControlledByResource(resource, kubectlApiClusterMap.get(cluster)),
                      expectedNumberOfPods, cluster, kubectlApiClusterMap));
          }
          K8sPodPerformer.runActionOnPods(foundPods, podAction, cluster,
                  k8sNodeApisClusterMapProvider.get(), kubectlApiClusterMap, kubectlProperties, fetchLogHelper);
          K8sPodPerformer.savePodResourceInformation(cluster, table, scenarioRegistryClusterMap);
      }

      // --- STEP 5: Process kill on pod (with optional restart verification) ---
      // Gherkin: Then a pod with label 'eric-pc-networking-cm-agent' is restarted by killing process '/usr/local/bin/agentd' with signal 9
      @CriticalTestStep
      @Then("a pod with label {string} is restarted by killing process {string} with signal {int}{cluster_selector}")
      public void restartPodByKillingProcess(final String podLabel, final String processName,
              final int signal, final ClustersToExecute clustersToExecute) {
          clustersToExecute.parallelStream().forEach(cluster ->
                  K8sPodPerformer.killProcessOnPod(cluster, podLabel, processName, signal, true,
                          kubectlApiClusterMap, k8sNodeApisClusterMapProvider));
      }

      // --- STEP 6: Process kill without restart verification ---
      // Gherkin: When the process '/usr/local/bin/agentd' is killed with signal 9 on a pod with label 'eric-pc-networking-cm-agent'
      @GeoredCriticalTestStep
      @When("the process {string} is killed with signal {int} on a pod with label {string}{cluster_selector}")
      public void killProcessOnPodWithSignal(final String processName, final int signal,
              final String podLabel, final ClustersToExecute clustersToExecute) {
          final String cluster = clustersToExecute.getCurrentCluster();
          K8sPodPerformer.killProcessOnPod(cluster, podLabel, processName, signal, false,
                  kubectlApiClusterMap, k8sNodeApisClusterMapProvider);
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  K8sPodPerformer.java — Execution Engine

  // File: K8sPodPerformer.java — Runs PodAction on resolved pods

  public final class K8sPodPerformer {

      /**
       * Run action on a single pod.
       */
      public static void runActionOnPod(final Pod pod, final PodAction podAction, final String cluster,
              final K8sNodeApisClusterMap k8sNodeApisClusterMap,
              final KubectlApiClusterMap kubectlApiClusterMap,
              final KubectlProperties kubectlProperties, final FetchLogHelper fetchLogHelper) {
          runActionOnPods(List.of(pod), podAction, cluster, k8sNodeApisClusterMap,
                  kubectlApiClusterMap, kubectlProperties, fetchLogHelper);
      }

      /**
       * Run action on a list of pods.
       * For each pod: delegates to PodAction.execute(k8sNodeApis, kubectlApi, props, podName)
       */
      public static void runActionOnPods(final List<Pod> pods, final PodAction podAction, final String cluster,
              final K8sNodeApisClusterMap k8sNodeApisClusterMap,
              final KubectlApiClusterMap kubectlApiClusterMap,
              final KubectlProperties kubectlProperties, final FetchLogHelper fetchLogHelper) {
          final KubectlApi kubectlApi = kubectlApiClusterMap.get(cluster);
          final K8sNodeApis k8sNodeApis = k8sNodeApisClusterMap.get(cluster);
          for (final Pod pod : pods) {
              final String podName = pod.getMetadata().getName();
              JcatLoggingApi.setTestInfo("Executing '%s' on pod '%s' in cluster '%s'", podAction, podName, cluster);
              podAction.execute(k8sNodeApis, kubectlApi, kubectlProperties, podName);
          }
      }

      /**
       * Kill a specific process inside a pod's container.
       * Resolves the pod → finds the node → executes kill on the node via SSH.
       */
      public static void killProcessOnPod(final String cluster, final String podLabel,
              final String processName, final int signal, final boolean waitForRestart,
              final KubectlApiClusterMap kubectlApiClusterMap,
              final Provider<K8sNodeApisClusterMap> k8sNodeApisClusterMapProvider) {
          final KubectlApi kubectlApi = kubectlApiClusterMap.get(cluster);
          final Pod pod = kubectlApi.findPodByLabel(podLabel);
          final String podName = pod.getMetadata().getName();
          final String nodeName = kubectlApi.getNodeName(podName);
          final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().get(cluster)
                  .getWithHostname(nodeName);
          final String containerId = kubectlApi.getContainerId(podName, kubectlApi.getContainers(podName)[0]);

          k8sNodeApi.killProcessInContainer(containerId, processName, signal);

          if (waitForRestart) {
              Poll.isActionCompleted(() -> kubectlApi.checkResourcesUp(podName),
                      Duration.ofSeconds(120), Duration.ofSeconds(2));
          }
      }

      /**
       * Wait for all pods to come up within timeout.
       */
      public static void waitAllPodsComeUp(final KubectlApi kubectlApi, final Duration timeout,
              final Duration interval, final List<String> ignoredPrefixes) {
          Poll.isActionCompleted(() -> kubectlApi.checkAllResourcesUp(ignoredPrefixes), timeout, interval);
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework — GeoRedStepDefinition.java (Routing-Engine Pod Actions)

  // File: GeoRedStepDefinition.java — Pod actions on routing-engine pods

  // Gherkin: When primary routing-engine pod is deleted
  //          When secondary routing-engine pod is restarted
  //          When primary routing-engine pod is force deleted on active cluster
  @GeoredTestStep
  @When("{routing_engine_pod_selection} routing-engine pod is {pod_action}{cluster_selector}")
  public void runActionOnNSelectedCrePods(final RouterEnginePodRole wantedRole,
          final PodAction podAction, final ClustersToExecute clustersToExecute) {
      final String cluster = clustersToExecute.getCurrentCluster();
      runActionOnCrePods(wantedRole, podAction, -1, TimeUnit.MINUTES, cluster);
  }

  private void runActionOnCrePods(final RouterEnginePodRole wantedRole, final PodAction podAction,
          final int timeout, final TimeUnit timeUnit, final String cluster) {
      final KubectlApi kubectlApi = kubectlApiClusterMap.get(cluster);
      final String crePodName = findCrePodByRole(wantedRole, kubectlApi);

      if (podAction.equals(PodAction.DELETED) || podAction.equals(PodAction.FORCE_DELETED)) {
          podAction.execute(k8sNodeApis, kubectlApi, kubectlProperties, crePodName);
      } else if (podAction.equals(PodAction.RESTARTED)) {
          // Kill all containers to trigger restart
          podAction.execute(k8sNodeApis, kubectlApi, kubectlProperties, crePodName);
      }
      // Wait for pod recovery and role reassignment
      waitForCrePodsReady(kubectlApi, timeout, timeUnit);
  }
