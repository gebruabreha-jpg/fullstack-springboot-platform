 Task 1: Pod/Container Kill — Complete Code

Here's the complete implementation covering both frameworks:

Key Differences: PCC uses parallel multi-pod kills with synchronization,
while Beets uses sequential single-pod kills for CRE restart scenarios.

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  PCC Test Framework Pattern — K8sContainerSteps.java

  // Step Definitions (Cucumber Gherkin → Java)
  // File: K8sContainerSteps.java

  @ScenarioScoped
  public class K8sContainerSteps implements TestStepDefinition {

      private final KubectlApiClusterMap kubectlApiClusterMap;
      private final Provider<K8sNodeApisClusterMap> k8sNodeApisClusterMapProvider;
      private final ScenarioRegistryClusterMap scenarioRegistryClusterMap;
      private final ClusterRegistry clusterRegistry;

      @Inject
      K8sContainerSteps(final KubectlApiClusterMap kubectlApiClusterMap,
              final KubectlProperties kubectlProperties,
              final Provider<K8sNodeApisClusterMap> k8sNodeApisClusterMapProvider,
              final ScenarioRegistryClusterMap scenarioRegistryClusterMap,
              final ClusterRegistry clusterRegistry) {
          this.kubectlApiClusterMap = kubectlApiClusterMap;
          this.k8sNodeApisClusterMapProvider = k8sNodeApisClusterMapProvider;
          this.scenarioRegistryClusterMap = scenarioRegistryClusterMap;
          this.clusterRegistry = clusterRegistry;
      }

      // --- STEP 1: Kill container by pod prefix ---
      // Gherkin: When a container is killed:
      //   | Pod Prefix  | Container | Kill Signal | Number of pods |
      //   | eric-pc-sm  | confd     | sigkill     | 1              |
      @GeoredTestStep
      @When("a container is killed{cluster_selector}:")
      public void killContainerOnPodPrefix(final ClustersToExecute clustersToExecute, final DataTableWrapper table) {
          final String cluster = clustersToExecute.getCurrentCluster();
          K8sContainerHelper.killContainer(table, K8sConstants.Pod.POD_PREFIX, cluster,
                  k8sNodeApisClusterMapProvider, kubectlApiClusterMap);
      }

      // --- STEP 2: Kill container by resource name ---
      // Gherkin: When a container controlled by resource is killed:
      //   | Pod Resource  | Container | Kill Signal | Number of pods |
      //   | eric-pc-sm-ds | confd     | sigkill     | 1              |
      @GeoredTestStep
      @When("a container (that )controlled by resource is killed{cluster_selector}:")
      public void killContainerOnPodResource(final ClustersToExecute clustersToExecute, final DataTableWrapper table) {
          final String cluster = clustersToExecute.getCurrentCluster();
          K8sContainerHelper.killContainer(table, K8sConstants.Pod.POD_RESOURCE, cluster,
                  k8sNodeApisClusterMapProvider, kubectlApiClusterMap);
          K8sPodPerformer.savePodResourceInformation(cluster, table, scenarioRegistryClusterMap);
      }

      // --- STEP 3: Kill container by pod name pattern (regex) ---
      // Gherkin: When a container controlled by pod that matches name pattern is killed:
      //   | Pod name pattern                 | Container                    | Kill Signal | Number of pods |
      //   | eric-data-object-storage-mn-\\d+ | eric-data-object-storage-mn  | sigkill     | 1              |
      @GeoredTestStep
      @When("a container controlled by pod that matches name pattern is killed{cluster_selector}:")
      public void killContainerOnPodNamePattern(final ClustersToExecute clustersToExecute, final DataTableWrapper table) {
          final String cluster = clustersToExecute.getCurrentCluster();
          K8sContainerHelper.killContainer(table, K8sConstants.Table.TABLE_HEADER_POD_NAME_PATTERN, cluster,
                  k8sNodeApisClusterMapProvider, kubectlApiClusterMap);
      }

      // --- STEP 4: Random resource-based container kill ---
      // Gherkin: When containers controlled by 3 resources are killed randomly:
      //   | Resource                   | Container  | Role   | Kill Signal | Max random resource |
      //   | eric-pc-sm-controller      | controller | active | sigkill     | 1                   |
      @GeoredTestStep
      @When("container(s) controlled by {int} resource(s) is/are killed randomly{cluster_selector}:")
      public void killResourceContainersRandomly(final int randomResourceNumber,
              final ClustersToExecute clustersToExecute, final DataTableWrapper table) {
          final String cluster = clustersToExecute.getCurrentCluster();
          final KubectlApi clusterKubectlApi = kubectlApiClusterMap.get(cluster);
          final List<String[]> randomResources = KubeHelpers.getRandomResources(randomResourceNumber, table);
          final Map<String, String[]> randomPods = K8sPodRetriever
                  .getRandomPodsFromRandomResources(clusterKubectlApi, randomResources);
          randomPods.forEach((pod, resource) -> K8sContainerHelper
                  .killContainer(clusterKubectlApi, cluster, pod, resource, k8sNodeApisClusterMapProvider));
          scenarioRegistryClusterMap.get(cluster).put("last_restarted_pods", new ArrayList<>(randomPods.keySet()));
      }

      // --- STEP 5: Get pod status ---
      // Gherkin: Then pod "eric-pc-sm-smf-pgw-session-0" status is retrieved
      @TestStep
      @Then("pod {string} status is retrieved")
      public void getPodStatus(final String podName) {
          final KubectlApi kubectl = kubectlApiClusterMap.getFirst();
          final String status = kubectl.getPodStatus(podName);
          JcatLoggingApi.setTestInfo("Pod '%s' status: %s", podName, status);
      }

      // --- STEP 6: Exec command in container ---
      // Gherkin: When command "ls -la /tmp" is executed in container "confd" of pod "eric-pc-sm-xxx"
      @TestStep
      @When("command {string} is executed in container {string} of pod {string}")
      public void execInContainer(final String command, final String containerName, final String podName) {
          final KubectlApi kubectl = kubectlApiClusterMap.getFirst();
          final String output = kubectl.execInContainer(podName, containerName, command);
          JcatLoggingApi.setTestInfo("Executed '%s' in pod '%s' container '%s': %s",
                  command, podName, containerName, output.substring(0, Math.min(200, output.length())));
      }

      // --- STEP 7: Simulate crash loop ---
      // Gherkin: When pod "eric-pc-sm-xxx" crash loop is simulated
      @CriticalTestStep
      @When("pod {string} crash loop is simulated")
      public void simulateCrashLoop(final String podName) {
          final String containerName = kubectlApiClusterMap.getFirst().getContainers(podName)[0];
          K8sContainerHelper.killContainer(podName, containerName, KillSignal.SIGKILL,
                  kubectlApiClusterMap.getFirst(), k8sNodeApisClusterMapProvider.get().getFirst());
          JcatLoggingApi.setTestInfo("Crash loop simulated for pod '%s'", podName);
      }

      // --- STEP 8: Simulate liveness probe failure ---
      // Gherkin: When liveness probe of pod "eric-pc-sm-xxx" fails
      @CriticalTestStep
      @When("liveness probe of pod {string} fails")
      public void failLivenessProbe(final String podName) {
          final String containerName = kubectlApiClusterMap.getFirst().getContainers(podName)[0];
          ActionsHelper.killProcessInContainer(podName, containerName, "/healthz", 9,
                  kubectlApiClusterMap.getFirst(), k8sNodeApisClusterMapProvider.get().getFirst());
          JcatLoggingApi.setTestInfo("Liveness probe failure simulated for pod '%s'", podName);
      }

      // --- STEP 9: Simulate readiness probe failure ---
      // Gherkin: When readiness probe of pod "eric-pc-sm-xxx" fails
      @CriticalTestStep
      @When("readiness probe of pod {string} fails")
      public void failReadinessProbe(final String podName) {
          final KubectlApi kubectl = kubectlApiClusterMap.getFirst();
          kubectl.patchPodReadinessProbe(podName, false);
          JcatLoggingApi.setTestInfo("Readiness probe failure simulated for pod '%s'", podName);
      }

      // --- STEP 10: Init container failure ---
      // Gherkin: When init container of pod "eric-pc-sm-xxx" fails
      @CriticalTestStep
      @When("init container of pod {string} fails")
      public void failInitContainer(final String podName) {
          final KubectlApi kubectl = kubectlApiClusterMap.getFirst();
          kubectl.failInitContainer(podName);
          JcatLoggingApi.setTestInfo("Init container failure simulated for pod '%s'", podName);
      }

      // --- STEP 11: ConfigMap missing ---
      // Gherkin: When ConfigMap "eric-pc-sm-config" is deleted
      @TestStep
      @When("ConfigMap {string} is deleted")
      public void deleteConfigMap(final String configMapName) {
          kubectlApiClusterMap.getFirst().deleteConfigMap(configMapName);
          JcatLoggingApi.setTestInfo("ConfigMap '%s' deleted", configMapName);
      }

      // --- STEP 12: DNS resolution failure inside pod ---
      // Gherkin: When DNS resolution fails inside pod "eric-pc-sm-xxx"
      @TestStep
      @When("DNS resolution fails inside pod {string}")
      public void failDNSResolution(final String podName) {
          final String containerName = kubectlApiClusterMap.getFirst().getContainers(podName)[0];
          kubectlApiClusterMap.getFirst().execInContainer(podName, containerName,
                  "iptables -A OUTPUT -p udp --dport 53 -j DROP");
          JcatLoggingApi.setTestInfo("DNS resolution failure simulated inside pod '%s'", podName);
      }

      // --- STEP 13: Sidecar container failure ---
      // Gherkin: When sidecar container "istio-proxy" of pod "eric-pc-sm-xxx" is killed
      @TestStep
      @When("sidecar container {string} of pod {string} is killed")
      public void killSidecarContainer(final String sidecarName, final String podName) {
          final KubectlApi kubectl = kubectlApiClusterMap.getFirst();
          K8sContainerHelper.killContainer(podName, sidecarName, KillSignal.SIGKILL,
                  kubectl, k8sNodeApisClusterMapProvider.get().getFirst());
          JcatLoggingApi.setTestInfo("Sidecar container '%s' killed in pod '%s'", sidecarName, podName);
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  PCC Test Framework — K8sContainerHelper.java (Core Kill Logic)

  // File: K8sContainerHelper.java — The actual kill mechanism

  public final class K8sContainerHelper {

      private K8sContainerHelper() {}

      /**
       * Kill containers in specified pods using parallel execution.
       * Core mechanism:
       *   1. Get container ID via kubectlApi.getContainerId(podName, containerName)
       *   2. Get node name via kubectlApi.getNodeName(podName)
       *   3. Get K8sNodeApi for that node
       *   4. Call k8sNodeApi.killContainers(containerIds, killSignal)
       */
      public static void killContainer(final DataTableWrapper table, final String tablePodHeader,
              final String cluster,
              final Provider<K8sNodeApisClusterMap> k8sNodeApisClusterMapProvider,
              final KubectlApiClusterMap kubectlApiClusterMap) {

          // Validate table headers
          final List<String> headers = new ArrayList<>(List.of(
                  tablePodHeader,
                  K8sConstants.Table.TABLE_HEADER_CONTAINER,
                  K8sConstants.Table.TABLE_HEADER_KILL_SIGNAL,
                  K8sConstants.Table.TABLE_HEADER_NR_OF_PODS));
          final List<String> optionalHeaders = List.of(
                  K8sConstants.Table.ROLE,
                  K8sConstants.Table.TABLE_HEADER_NAMESPACE);
          table.verifyHeaders(headers, optionalHeaders);

          // Build map of K8sNodeApi → List<KillRequest>
          final Map<K8sNodeApi, List<KillRequest>> nodeKillRequestsMap = createNodeKillRequestsMap(
                  table, tablePodHeader, cluster, k8sNodeApisClusterMapProvider.get(), kubectlApiClusterMap);

          // Execute kills in parallel across nodes
          final List<RunnableTask> tasks = nodeKillRequestsMap.entrySet().stream()
                  .map(entry -> new RunnableTask(
                          () -> entry.getKey().killContainers(entry.getValue()),
                          String.format("Killing containers on '%s'", entry.getKey().getHostname())))
                  .toList();

          final ParallelExecutionUtil executor = new ParallelExecutionUtil(
                  Math.min(tasks.size(), K8sConstants.Common.NUMBER_OF_THREADS));
          executor.execute(tasks);
      }

      /**
       * Build the kill requests map: for each row in the table, resolve the pod → node → container ID
       */
      private static Map<K8sNodeApi, List<KillRequest>> createNodeKillRequestsMap(
              final DataTableWrapper table, final String tablePodHeader,
              final String cluster, final K8sNodeApisClusterMap k8sNodeApisClusterMap,
              final KubectlApiClusterMap kubectlApiClusterMap) {

          final Map<K8sNodeApi, List<KillRequest>> nodeKillRequestsMap = new HashMap<>();

          for (final Map<String, String> row : table.getEntries()) {
              final String podRow = row.get(tablePodHeader);
              final String container = row.get(K8sConstants.Table.TABLE_HEADER_CONTAINER);
              final String signalOption = row.get(K8sConstants.Table.TABLE_HEADER_KILL_SIGNAL).toUpperCase();
              final KillSignal killSignal = KillSignal.valueOf(signalOption);
              final PodRole role = K8sPodRetriever.getPodRoleFromRow(row, podRow);
              final int expectedNumberOfPods = K8sPodRetriever.stringToExpectedNumberOfPods(
                      row.get(K8sConstants.Table.TABLE_HEADER_NR_OF_PODS));

              // Find matching pods
              final List<String> podNames = K8sPodRetriever.getPodsMatchingFilter(
                      K8sPodRetriever.getPodsWithPodHeader(tablePodHeader, podRow, container,
                              kubectlApiClusterMap.get(cluster)),
                      expectedNumberOfPods, cluster, role, kubectlApiClusterMap)
                      .stream()
                      .map(pod -> pod.getMetadata().getName())
                      .collect(Collectors.toList());

              // For each pod, resolve container ID and node, build KillRequest
              for (String podName : podNames) {
                  final KubectlApi clusterKubectlApi = kubectlApiClusterMap.get(cluster);
                  final String containerId = clusterKubectlApi.getContainerId(podName, container);
                  final String nodeName = clusterKubectlApi.getNodeName(podName);
                  final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMap.get(cluster).getWithHostname(nodeName);

                  nodeKillRequestsMap
                          .computeIfAbsent(k8sNodeApi, k -> new ArrayList<>())
                          .add(new KillRequest(containerId, killSignal));
              }
          }
          return nodeKillRequestsMap;
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework Pattern — ActionsHelper.java (Simpler Wrapper)

  // File: ActionsHelper.java — Core kill helper for Beets
  // This is the simpler, single-pod-at-a-time approach

  public final class ActionsHelper {

      /**
       * Kill a container in a specific pod.
       * Steps:
       *   1. Get container ID from pod via kubectlApi.getContainerId()
       *   2. Get node hosting the pod via kubectlApi.getNodeName()
       *   3. Get K8sNodeApi for that node
       *   4. Execute kill via k8sNodeApi.killContainers()
       *
       * @param podName       the name of the pod
       * @param containerName the name of the container to kill
       * @param kubectlApi    A KubectlApi object
       * @param k8sNodeApis   A K8sNodeApis object
       */
      public static void killContainerInPod(final String podName, final String containerName,
              final KubectlApi kubectlApi, final K8sNodeApis k8sNodeApis) {
          final KillSignal signal = KillSignal.SIGKILL;
          JcatLoggingApi.setTestInfo("Killing container '%s' in pod '%s' with signal '%s'.",
                  containerName, podName, signal);
          final List<String> containerIds = Collections.singletonList(
                  kubectlApi.getContainerId(podName, containerName));
          final String nodeName = kubectlApi.getNodeName(podName);
          final K8sNodeApi k8sNodeApi = k8sNodeApis.getWithHostname(nodeName);
          k8sNodeApi.killContainers(containerIds, signal);
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework — PcgStepDefinition.java (VPN Container Kill)

  // File: PcgStepDefinition.java — Kill VPN container by home-pod lookup

  // Gherkin: Then the container "eric-pc-network-forwarder-vp" controlled by home-pod of "ikev1-vpn-1" VPN is killed
  @TestStep
  @Then("the container {string} controlled by home-pod of {string} VPN is killed")
  public void killContainerInVPNHomePod(final String containerName, final String ipsecVPN) {
      final String homePodName = getHomePodNameOfGivenIpsec(ipsecVPN);
      JcatLoggingApi.setTestInfo("The container '%s' controlled by home pod '%s' of vpn '%s' is killed",
              containerName, homePodName, ipsecVPN);
      ActionsHelper.killContainerInPod(homePodName, containerName, kubectlApiClusterMap.getFirst(), k8sNodeApis);
  }

  private String getHomePodNameOfGivenIpsec(final String ipsecVPN) {
      final String output = mappedEricPcUp.get(clusterRegistry.getActiveCluster()).runAdpCmCommand(
              String.format("show ipsec vpns vpn %s tunnel-distribution", ipsecVPN), AdpCmShellId.CLI);
      return Arrays.stream(output.split("\\n"))
              .filter(line -> line.contains("home-pod-name"))
              .map(line -> line.trim().split("\\s+")[1])
              .findFirst()
              .orElseThrow(() -> new TestException("Failed to find the home-pod of ipsec '%s'", ipsecVPN));
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework — UpfCreIntegrationSteps.java (CRE Pod Reboot)

  // File: UpfCreIntegrationSteps.java — CRE pod sequential reboot

  // Gherkin: Then cre pod reboot one by one use method kubectl delete pod
  @TestStep
  @Then("cre pod reboot one by one use method kubectl delete pod")
  public void kubectlDeleteCrePod() {
      final List<String> podList = kubectlApi.getPodsWithPattern(Pattern.compile(CRE_POD_WITHOUT_API));
      int iteration = 1;
      for (String pod : podList) {
          kubectlApi.deletePod(pod);
          Delay.sleep(Duration.ofSeconds(5));
          waitForAllPodsUp(12);
          if (iteration++ < podList.size()) {
              Delay.sleep(Duration.ofSeconds(60));
          }
      }
  }

  // Gherkin: Then cre pod reboot one by one use method kill container
  @TestStep
  @Then("cre pod reboot one by one use method kill container")
  public void killContainerInCrePod() {
      final List<String> podList = kubectlApi.getPodsWithPattern(Pattern.compile(CRE_POD_WITHOUT_API));
      int iteration = 1;
      for (String pod : podList) {
          final String firstContainerInPod = kubectlApi.getContainers(pod)[0];
          ActionsHelper.killContainerInPod(pod, firstContainerInPod, kubectlApi, k8sNodeApis);
          Delay.sleep(Duration.ofSeconds(5));
          waitForAllPodsUp(12);
          if (iteration++ < podList.size()) {
              Delay.sleep(Duration.ofSeconds(60));
          }
      }
  }

  private void waitForAllPodsUp(int maxRetries) {
      int retries = 0;
      while (!kubectlApi.checkAllResourcesUp(kubectlProperties.getIgnorePodPrefix()) && retries < maxRetries) {
          Delay.sleep(Duration.ofSeconds(10));
          retries++;
      }
  }
