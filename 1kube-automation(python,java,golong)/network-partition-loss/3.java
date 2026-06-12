 Task 3: Network Loss/Partition — Complete Code

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  PCC Test Framework — K8sNode.java (Core Network Commands)

  // File: K8sNode.java — Interface control and traffic control primitives

  public abstract class K8sNode implements K8sNodeApi {

      // Command constants
      private static final String INTERFACE_SET_DOWN_CMD = "sudo ip link set %s down";
      private static final String INTERFACE_SET_UP_CMD = "sudo ip link set %s up";
      private static final String INTERFACE_SET_DOWN_AND_UP_CMD = "sudo ip link set %s down && sleep %d && sudo ip link set %s up";
      private static final String INTERFACE_SET_SLEEP_CMD = "sleep %.3f";
      private static final String INTERFACE_SET_CHAINED_CMD_DELIMITER = " && ";

      /**
       * Bring down interfaces, sleep, then bring them back up.
       * Uses 'ip link set down/up' — a simple L2 interface disable.
       */
      @Override
      public void bringDownAndUpInterfaces(final Set<String> interfaces, final Duration timeout) {
          final double sleepSeconds = timeout.getSeconds() + (timeout.getNano() / 1_000_000_000.0);

          // Build: "sudo ip link set eth1 down && sudo ip link set eth2 down"
          final String downCmds = interfaces.stream()
                  .map(iface -> String.format(INTERFACE_SET_DOWN_CMD, iface))
                  .collect(Collectors.joining(INTERFACE_SET_CHAINED_CMD_DELIMITER));

          // Build: "sudo ip link set eth1 up && sudo ip link set eth2 up"
          final String upCmds = interfaces.stream()
                  .map(iface -> String.format(INTERFACE_SET_UP_CMD, iface))
                  .collect(Collectors.joining(INTERFACE_SET_CHAINED_CMD_DELIMITER));

          // Combined: down → sleep → up
          final String downSleepUpCmds = String.join(INTERFACE_SET_CHAINED_CMD_DELIMITER,
                  downCmds,
                  String.format(INTERFACE_SET_SLEEP_CMD, sleepSeconds),
                  upCmds);

          executeCheckResult(downSleepUpCmds, new CommandOptions.Builder()
                  .withLinuxResultCode().withRecordTime().withTimeout((int) TIMEOUT.getSeconds()).build());
      }

      /**
       * Drop all packets using tc (traffic control) netem.
       * Uses BashScheduleBuilder to generate a timed script that:
       *   1. At startTime: tc qdisc add dev <iface> root netem loss 100%
       *   2. At startTime + duration: tc qdisc del dev <iface> root netem
       */
      @Override
      public void dropAllPacketsOnInterfaces(final Set<String> interfaces, final Duration timeout,
              final long startTimeMillis) {
          final String command = BashScheduleBuilder.tcDropAndRestore(interfaces, timeout, startTimeMillis);
          executeCheckResult(command, new CommandOptions.Builder()
                  .withLinuxResultCode().withRecordTime().withTimeout((int) TIMEOUT.getSeconds()).build());
      }

      @Override
      public boolean doesInterfaceExist(final String interfaceName) {
          final CommandResult result = navigator.sendAsResult(
                  String.format("ip link show %s", interfaceName), LinuxShellId.LINUX_SHELL, COMMAND_OPTIONS);
          return result.isSuccessful();
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  PCC Test Framework — BashScheduleBuilder.java (tc qdisc Script Generator)

  // File: BashScheduleBuilder.java — Generates scheduled tc traffic control bash scripts

  public final class BashScheduleBuilder {

      private final List<ScheduledActionAndInterfaces> commands = new ArrayList<>();

      public static BashScheduleBuilder create() {
          return new BashScheduleBuilder();
      }

      public BashScheduleBuilder addScheduledAction(final long timestamp, final String action,
              final Set<String> interfaces) {
          this.commands.add(new ScheduledActionAndInterfaces(timestamp, action, interfaces));
          return this;
      }

      /**
       * Convenience method: drop all packets at startTime, restore at startTime + duration.
       * Generates tc commands:
       *   ADD: "tc qdisc add dev eth1 root netem loss 100%"
       *   DEL: "tc qdisc del dev eth1 root netem"
       */
      public static String tcDropAndRestore(final Set<String> interfaces, final Duration timeout,
              final long startTime) {
          return create()
                  .addScheduledAction(startTime, "add", interfaces)
                  .addScheduledAction(startTime + timeout.toMillis(), "del", interfaces)
                  .build();
      }

      public String build() {
          final String scriptBody = generateBashScript(this.commands);
          return "sudo bash <<'EOF'\n" + scriptBody + "\nEOF";
      }

      // Generated script uses run_at_timestamp() helper to wait until target timestamp,
      // then executes tc commands. All commands run as background jobs (&) with wait at end.

      private static String buildTcCommand(final ScheduledActionAndInterfaces cmd) {
          final List<String> tcCommands = new ArrayList<>();
          for (final String iface : cmd.interfaces) {
              if ("add".equals(cmd.action)) {
                  tcCommands.add("tc qdisc add dev " + iface + " root netem loss 100%");
              } else if ("del".equals(cmd.action)) {
                  tcCommands.add("tc qdisc del dev " + iface + " root netem");
              }
          }
          return String.join(" && ", tcCommands);
      }

      private record ScheduledActionAndInterfaces(long timestamp, String action, Set<String> interfaces) {}
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  PCC Test Framework — K8sNodeSteps.java (Network Step Definitions)

  // File: K8sNodeSteps.java — Network chaos step definitions

  // --- STEP 1: Bring down interface(s) on random nodes for duration ---
  // Gherkin: Then concurrently bring down network interface eth2 in 3 random worker nodes for 5 seconds
  @Deprecated(forRemoval = true) // replaced by table-based step below
  @GeoredTestStep
  @Then("concurrently bring down network interface(s) {interface_list} in {int} random {node_type} node(s) for {int} {time_unit}{cluster_selector}")
  public void concurrentlyTakeNetworkInterfaceOfflineInRandomNodesForDurationNew(
          final Set<String> interfaces, final int numberOfNodes, final NodeType nodeType,
          final int downtime, final TimeUnit timeUnit, final ClustersToExecute clustersToExecute) {
      final String cluster = clustersToExecute.getCurrentCluster();
      final K8sNodeApis k8sNodeApis = k8sNodeApisClusterMapProvider.get().get(cluster)
              .getAnyNodesWithNodeType(nodeType, numberOfNodes);
      final Duration duration = Duration.of(downtime, timeUnit.toChronoUnit());
      // Verify interfaces exist
      concurrentlyCheckNetworkInterfacesInNodes(k8sNodeApis, interfaces, cluster);
      // Bring them down and up
      concurrentlyBringDownNetworkInterfacesInNodesForDuration(k8sNodeApis, interfaces, duration, cluster);
  }

  // --- STEP 2: Table-based bring down interfaces ---
  // Gherkin: Then concurrently bring down network interfaces on the following nodes:
  //   | Node type     | Number of nodes | Interfaces | Duration | Time unit    |
  //   | worker        | 1               | eth1, eth2 | 2        | seconds      |
  //   | control-plane | 2               | eth1       | 1500     | milliseconds |
  @GeoredTestStep
  @Then("concurrently bring down network interface(s) on the following nodes{cluster_selector}:")
  public void concurrentlyBringDownNetworkInterfacesInNodesForDuration(
          final ClustersToExecute clustersToExecute, final DataTableWrapper dataTable) {
      final List<String> expectedColumns = List.of(NODE_TYPE, NUMBER_OF_NODES, INTERFACES, DURATION, TIME_UNIT);
      dataTable.verifyHeader(expectedColumns);
      // ... parse table, select random nodes, run parallel bring-down tasks
  }

  // --- STEP 3: Drop all packets via tc qdisc (netem loss 100%) ---
  // Gherkin: Then drop all packets on given interfaces for given duration on the following random nodes:
  //   | Node type     | Number of nodes | Interfaces | Duration | Time unit |
  //   | worker        | 2               | eth1, eth2 | 2        | seconds   |
  //   | control-plane | 1               | eth1       | 30       | seconds   |
  @GeoredTestStep
  @Then("drop all packets on given interfaces for given duration on the following random nodes{cluster_selector}:")
  public void concurrentlyDropAllPacketsOnNetworkInterfacesForDuration(
          final ClustersToExecute clustersToExecute, final DataTableWrapper dataTable) {
      final String cluster = clustersToExecute.getCurrentCluster();
      dataTable.verifyHeader(List.of(NODE_TYPE, NUMBER_OF_NODES, INTERFACES, DURATION, TIME_UNIT));

      // Parse table rows into records
      final List<NodeTypeAndInterfaces> tableRecords = getRecordsOfNodeTypesAndInterfacesFromTable(dataTable, cluster);

      // Phase 1: Check all interfaces exist
      final List<RunnableTask> checkTasks = getCheckInterfacesTasks(tableRecords, cluster);
      runParallelTasks(checkTasks, "check interfaces", checkTasks.size());

      // Phase 2: Drop packets (synchronized start time across all nodes)
      final List<RunnableTask> dropTasks = getDropAppPacketsTasks(tableRecords, cluster);
      runParallelTasks(dropTasks, "drop packets", dropTasks.size());
  }

  // Internal: builds parallel drop tasks with synchronized startTime
  private List<RunnableTask> getDropAppPacketsTasks(final List<NodeTypeAndInterfaces> tableRecords,
          final String cluster) {
      final long startTimeMillis = System.currentTimeMillis() + 10000; // 10s buffer for sync
      return tableRecords.stream()
              .map(record -> new RunnableTask(
                      () -> concurrentlyDropAllPacketsOnInterfacesInNodesForDuration(
                              record.selectedK8sNodeApisOfType(), record.interfaces(),
                              record.duration(), cluster, startTimeMillis),
                      "drop packets on " + record.nodeType()))
              .toList();
  }

  private void concurrentlyDropAllPacketsOnInterfacesInNodesForDuration(
          final List<K8sNodeApi> k8sNodeApis, final Set<String> interfaces,
          final Duration duration, final String cluster, final long startTimeMillis) {
      k8sNodeApis.parallelStream().forEach(k8sNodeApi -> {
          k8sNodeApi.dropAllPacketsOnInterfaces(interfaces, duration, startTimeMillis);
      });
  }

  private record NodeTypeAndInterfaces(NodeType nodeType, String numberOfNodes,
          Set<String> interfaces, Duration duration, List<K8sNodeApi> selectedK8sNodeApisOfType) {}

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework — LitmusExperiments Enum

  // File: LitmusExperiments.java — All supported Litmus chaos experiments

  public enum LitmusExperiments {

      POD_CPU_HOG("pod_cpu_hog", "pod-cpu-hog-",
              "pod_cpu_hog_rbac.yaml", "pod_cpu_hog_experiment.yaml",
              "pod_cpu_hog_engine.yaml", "pod_cpu_hog_engine.template"),

      CONTAINER_KILL("container_kill", "container-kill-",
              "container_kill_rbac.yaml", "container_kill_experiment.yaml",
              "container_kill_engine.yaml", "container_kill_engine.template"),

      POD_NETWORK_PARTITION("pod_network_partition", "pod-network-partition-",
              "pod_network_partition_rbac.yaml", "pod_network_partition_experiment.yaml",
              "pod_network_partition_engine.yaml", "pod_network_partition_engine.template"),

      POD_MEMORY_HOG("pod_memory_hog", "pod-memory-hog-",
              "pod_memory_hog_rbac.yaml", "pod_memory_hog_experiment.yaml",
              "pod_memory_hog_engine.yaml", "pod_memory_hog_engine.template"),

      POD_NETWORK_LATENCY("pod_network_latency", "pod-network-latency-",
              "pod_network_latency_rbac.yaml", "pod_network_latency_experiment.yaml",
              "pod_network_latency_engine.yaml", "pod_network_latency_engine.template"),

      POD_NETWORK_CORRUPTION("pod_network_corruption", "pod-network-corruption-",
              "pod_network_corruption_rbac.yaml", "pod_network_corruption_experiment.yaml",
              "pod_network_corruption_engine.yaml", "pod_network_corruption_engine.template"),

      POD_NETWORK_LOSS("pod_network_loss", "pod-network-loss-",
              "pod_network_loss_rbac.yaml", "pod_network_loss_experiment.yaml",
              "pod_network_loss_engine.yaml", "pod_network_loss_engine.template"),

      POD_IO_STRESS("pod_io_stress", "pod-io-stress-",
              "pod_io_stress_rbac.yaml", "pod_io_stress_experiment.yaml",
              "pod_io_stress_engine.yaml", "pod_io_stress_engine.template"),

      KUBELET_SERVICE_KILL("kubelet_service_kill", "kubelet-service-kill-",
              "kubelet_service_kill_rbac.yaml", "kubelet_service_kill_experiment.yaml",
              "kubelet_service_kill_engine.yaml", "kubelet_service_kill_engine.template"),

      DOCKER_SERVICE_KILL("docker_service_kill", "docker-service-kill-",
              "docker_service_kill_rbac.yaml", "docker_service_kill_experiment.yaml",
              "docker_service_kill_engine.yaml", "docker_service_kill_engine.template");

      private final String experimentName;
      private final String podPrefix;
      private final String rbacFile;
      private final String experimentFile;
      private final String engineFile;
      private final String engineTemplateFile;

      // constructor and getters...
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework — LitmusExperimentSteps.java (Run Experiments)

  // File: LitmusExperimentSteps.java — Orchestrate Litmus chaos experiments

  @ScenarioScoped
  public class LitmusExperimentSteps implements TestStepDefinition {

      // --- Install Litmus operator ---
      // Gherkin: When litmus is installed
      @GeoredTestStep
      @When("litmus is installed{cluster_selector}")
      public void installLitmus(final ClustersToExecute clustersToExecute) {
          litmusHelper.installLitmus(clustersToExecute.getCurrentCluster());
      }

      // --- Install Litmus operator CRDs ---
      // Gherkin: When the litmus operator is installed
      @GeoredTestStep
      @When("the litmus operator is installed{cluster_selector}")
      public void installLitmusOperator(final ClustersToExecute clustersToExecute) {
          final String operatorFile = beetsConfigurationProvider.get().getConfiguration()
                  .getFile(LITMUS_OPERATOR_FILE);
          litmusHelper.installLitmusOperator(operatorFile, clustersToExecute.getCurrentCluster());
      }

      // --- Run experiment (RBAC → Experiment → Engine) ---
      // Gherkin: When the litmus experiment POD_NETWORK_LOSS is run
      //          When the litmus experiment POD_NETWORK_PARTITION is run
      @GeoredTestStep
      @When("the litmus experiment {} is run{cluster_selector}")
      public void runLitmusExperiments(LitmusExperiments experiment, final ClustersToExecute clustersToExecute) {
          final String cluster = clustersToExecute.getCurrentCluster();
          final LitmusExperimentsActions litmusActions = mappedLitmusExperimentsActions.get(cluster);

          // Step 1: Apply RBAC (ServiceAccount, Role, RoleBinding)
          final String rbacFile = beetsConfigurationProvider.get().getConfiguration()
                  .getFile(experiment.getRbacFile());
          litmusActions.applyRbac(rbacFile, experiment.getExperimentName());

          // Step 2: Create experiment CRD
          final String experimentFile = beetsConfigurationProvider.get().getConfiguration()
                  .getFile(experiment.getExperimentFile());
          litmusActions.createExperiment(experimentFile, experiment.getExperimentName());

          // Step 3: Start engine (triggers the experiment)
          final String engineFile = beetsConfigurationProvider.get().getConfiguration()
                  .getFile(experiment.getEngineFile());
          litmusActions.startEngine(engineFile, experiment.getExperimentName());
      }

      // --- Configure experiment parameters ---
      // Gherkin: When the litmus engine POD_NETWORK_LOSS is configured:
      //   | variable name                     | value |
      //   | experiment_name                   | pod-network-loss-chaos |
      //   | applabel                          | chaos=network-loss |
      //   | chaos_duration                    | 300 |
      //   | network_packet_loss_percentage    | 100 |
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
          ConfigurationHelper.replacePlaceholdersInConfigFile(
                  new File(engineTemplateFile), propertyMap, false, new File(engineFile));
      }

      // --- Collect logs after experiment ---
      // Gherkin: When pod logs and describe outputs for the experiment POD_NETWORK_LOSS is collected
      @GeoredTestStep
      @When("pod logs and describe outputs for the experiment {} is collected{cluster_selector}")
      public void collectPodLogsAndDescribeOutput(LitmusExperiments experiment,
              final ClustersToExecute clustersToExecute) {
          mappedLitmusExperimentsActions.get(clustersToExecute.getCurrentCluster())
                  .collectPodLogs(experiment.getPodPrefix());
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework — DcgwStepDefinition.java (DCGW Interface Control)

  // File: DcgwStepDefinition.java — External gateway network simulation

  // --- Simulate DCGW shutdown (disable all interfaces) ---
  // Gherkin: When DCGW shutdown is simulated by disabling all interfaces towards SUT
  @TestStep
  @When("DCGW shutdown is simulated by disabling all interfaces towards SUT")
  public void simulateDcgwShutdown() {
      final String dcgwHost = dcgwProperties.getHost();
      scenarioRegistry.put("dcgw_simulated_restart", dcgwHost);
      DcgwHelper.switchNavigatorToAGivenDcgwIfAllowed(dcgwProperties, connectionLogWriter, dcgwNode, dcgwHost);
      DcgwHelper.simulateDcgwStartOrShutdown(dcgwHost, dcgwNode, false, PropertyFactory.getStpProperty());
  }

  // --- Simulate DCGW start (enable all interfaces) ---
  // Gherkin: When DCGW start is simulated by enabling all interfaces towards SUT
  @TestStep
  @When("DCGW start is simulated by enabling all interfaces towards SUT")
  public void simulateDcgwStart() {
      final String dcgwHost = dcgwProperties.getHost();
      DcgwHelper.switchNavigatorToAGivenDcgwIfAllowed(dcgwProperties, connectionLogWriter, dcgwNode, dcgwHost);
      DcgwHelper.simulateDcgwStartOrShutdown(dcgwHost, dcgwNode, true, PropertyFactory.getStpProperty());
      scenarioRegistry.remove("dcgw_simulated_restart");
  }

  // --- Disable L3 interfaces of random DCGW for duration ---
  // Gherkin: Given "sgi1" L3 interfaces of all DCGWs are broken for 10 seconds
  @TestStep
  @Given("{support_sgi_types} L3 interfaces of all DCGWs are broken for {int} seconds")
  public void restartAllDcgwInterfacesWithin(final String networkInstance, final int durationSec) {
      final List<String> dcgwHosts = dcgwProperties.getHosts();
      // Verify link up on all DCGWs
      dcgwHosts.forEach(dcgwHost -> {
          DcgwHelper.switchNavigatorToAGivenDcgwIfAllowed(dcgwProperties, connectionLogWriter, dcgwNode, dcgwHost);
          verifyLinkStatusAndNexthopCountIsExpected(..., UP, ...);
      });
      try {
          // Disable interfaces on all DCGWs
          dcgwHosts.forEach(dcgwHost -> {
              DcgwHelper.switchNavigatorToAGivenDcgwIfAllowed(dcgwProperties, connectionLogWriter, dcgwNode, dcgwHost);
              DcgwHelper.disableDcgwLinkInterface(dcgwInterfaceName, dcgwUpInterfaceName, dcgwNode, dcgwHost);
          });
          Delay.sleep(Duration.ofSeconds(durationSec));
          // Verify links are down
          dcgwHosts.forEach(dcgwHost -> {
              verifyLinkStatusAndNexthopCountIsExpected(..., DOWN, ...);
          });
      } finally {
          // Re-enable interfaces on all DCGWs
          dcgwHosts.forEach(dcgwHost -> {
              DcgwHelper.enableDcgwLinkInterface(dcgwInterfaceName, dcgwUpInterfaceName, dcgwNode, dcgwHost,
                      dcgwUpInterfaceAddresses);
          });
          // Verify links are up again
          dcgwHosts.forEach(dcgwHost -> {
              verifyLinkStatusAndNexthopCountIsExpected(..., UP, ...);
          });
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  PCC Test Framework — NetworkTcSteps.java (tc netem Variations)

  // File: NetworkTcSteps.java — tc netem delay, corruption, duplication

  // --- Add delay on interface ---
  // Gherkin: Then delay of 200ms with 50ms variance is added on interface "eth1" on node "worker-pool1-xxx"
  @GeoredTestStep
  @Then("delay of {int}ms with {int}ms variance is added on interface {string} on node {string}{cluster_selector}")
  public void addDelayOnInterface(final int delayMs, final int varianceMs,
          final String interfaceName, final String nodeName, final ClustersToExecute clustersToExecute) {
      final String cluster = clustersToExecute.getCurrentCluster();
      final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().get(cluster)
              .getWithHostname(nodeName);
      k8sNodeApi.addDelay(interfaceName, delayMs, varianceMs);
      JcatLoggingApi.setTestInfo("Added delay %dms/%dms on %s@%s", delayMs, varianceMs, interfaceName, nodeName);
  }

  // --- Add corruption on interface ---
  // Gherkin: Then 5%% packet corruption is added on interface "eth1" on node "worker-pool1-xxx"
  @GeoredTestStep
  @Then("{int}%% packet corruption is added on interface {string} on node {string}{cluster_selector}")
  public void addCorruptionOnInterface(final int corruptPercent, final String interfaceName,
          final String nodeName, final ClustersToExecute clustersToExecute) {
      final String cluster = clustersToExecute.getCurrentCluster();
      final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().get(cluster)
              .getWithHostname(nodeName);
      k8sNodeApi.addCorruption(interfaceName, corruptPercent);
      JcatLoggingApi.setTestInfo("Added corruption %d%% on %s@%s", corruptPercent, interfaceName, nodeName);
  }

  // --- Add duplication on interface ---
  // Gherkin: Then 5%% packet duplication is added on interface "eth1" on node "worker-pool1-xxx"
  @GeoredTestStep
  @Then("{int}%% packet duplication is added on interface {string} on node {string}{cluster_selector}")
  public void addDuplicateOnInterface(final int duplicatePercent, final String interfaceName,
          final String nodeName, final ClustersToExecute clustersToExecute) {
      final String cluster = clustersToExecute.getCurrentCluster();
      final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().get(cluster)
              .getWithHostname(nodeName);
      k8sNodeApi.addDuplicate(interfaceName, duplicatePercent);
      JcatLoggingApi.setTestInfo("Added duplication %d%% on %s@%s", duplicatePercent, interfaceName, nodeName);
  }

  // --- Remove tc rules ---
  // Gherkin: Then tc qdisc rules are removed from interface "eth1" on node "worker-pool1-xxx"
  @GeoredTestStep
  @Then("tc qdisc rules are removed from interface {string} on node {string}{cluster_selector}")
  public void removeTcRulesOnInterface(final String interfaceName, final String nodeName,
          final ClustersToExecute clustersToExecute) {
      final String cluster = clustersToExecute.getCurrentCluster();
      final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().get(cluster)
              .getWithHostname(nodeName);
      k8sNodeApi.removeTcRules(interfaceName);
      JcatLoggingApi.setTestInfo("Removed tc rules on %s@%s", interfaceName, nodeName);
  }

  // --- IP route manipulation ---
  // Gherkin: Then default route is deleted on node "worker-pool1-xxx"
  @GeoredTestStep
  @Then("default route is deleted on node {string}{cluster_selector}")
  public void deleteDefaultRoute(final String nodeName, final ClustersToExecute clustersToExecute) {
      final String cluster = clustersToExecute.getCurrentCluster();
      final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().get(cluster)
              .getWithHostname(nodeName);
      k8sNodeApi.deleteDefaultRoute();
  }

  // Gherkin: Then default route via "192.168.1.1" is added on node "worker-pool1-xxx"
  @GeoredTestStep
  @Then("default route via {string} is added on node {string}{cluster_selector}")
  public void addDefaultRoute(final String gateway, final String nodeName,
          final ClustersToExecute clustersToExecute) {
      final String cluster = clustersToExecute.getCurrentCluster();
      final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().get(cluster)
              .getWithHostname(nodeName);
      k8sNodeApi.addDefaultRoute(gateway);
  }

  // --- CoreDNS operations ---
  // Gherkin: When CoreDNS pod is deleted in namespace "kube-system"
  @TestStep
  @When("CoreDNS pod is deleted in namespace {string}")
  public void deleteCoreDNSPod(final String namespace) {
      final String corednsPod = kubectlApiClusterMap.getFirst()
              .getPodsWithPrefix("coredns-").getFirst();
      kubectlApiClusterMap.getFirst().deletePod(corednsPod);
      JcatLoggingApi.setTestInfo("CoreDNS pod '%s' deleted in namespace '%s'", corednsPod, namespace);
  }

  // Gherkin: When CoreDNS deployment is restarted in namespace "kube-system"
  @TestStep
  @When("CoreDNS deployment is restarted in namespace {string}")
  public void rolloutRestartCoreDNS(final String namespace) {
      kubectlApiClusterMap.getFirst().kubectl(
              String.format("rollout restart deployment coredns -n %s", namespace));
      JcatLoggingApi.setTestInfo("CoreDNS rollout restarted in namespace '%s'", namespace);
  }

  // --- PCC TF: Dallas master tc rule ---
  // Gherkin: When tc rule "netem loss 100%" is added on Dallas interface "eth1"
  @TestStep
  @When("tc rule {string} is added on Dallas interface {string}")
  public void addTcRuleOnDallasMaster(final String netemDiscipline, final String dallasNetworkName) {
      final String interfaceName = getInterfaceNameFromType(dallasNetworkName);
      toolServerNavigators.getFirst().executeCommand(
              String.format("tc qdisc add dev %s root %s", interfaceName, netemDiscipline), 300);
      JcatLoggingApi.setTestInfo("TC rule added on Dallas interface %s", interfaceName);
  }

  // Gherkin: Then tc rule is deleted from Dallas interface "eth1"
  @TestStep
  @Then("tc rule is deleted from Dallas interface {string}")
  public void deleteTcRuleOnDallasMaster(final String dallasNetworkName) {
      final String interfaceName = getInterfaceNameFromType(dallasNetworkName);
      toolServerNavigators.getFirst().executeCommand(
              String.format("tc qdisc del dev %s root", interfaceName), 300);
      JcatLoggingApi.setTestInfo("TC rule deleted from Dallas interface %s", interfaceName);
  }

  private String getInterfaceNameFromType(final String dallasNetworkName) {
      return dallasNetworkName.toLowerCase().replace(" ", "");
  }

  private List<K8sNodeApisClusterMapProvider> k8sNodeApisClusterMapProvider = new K8sNodeApisClusterMapProvider();
  private Map<String, KubectlApi> kubectlApiClusterMap = Map.of("default", new KubectlApi());

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────
