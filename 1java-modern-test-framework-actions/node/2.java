 Task 2: Node Failure Actions — Complete Code

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  NodeAction Enum (The Core)

  // File: com.ericsson.pcc.integration.k8snodes.enums.NodeAction

  public enum NodeAction {

      RESTARTED,
      FORCE_RESTARTED,
      KERNEL_PANICKED,
      KERNEL_PANICKED_WITHOUT_RECONNECT,
      STARTED,
      SHUT_DOWN,
      DESTROYED,
      DRAINED,
      UNCORDONED,
      CORDONED,
      POWER_STATUS,
      UNKNOWN;

      private static final EnumMapUtil<NodeAction> NODE_ACTION_ENUM_MAP_UTIL = EnumMapUtil.create(UNKNOWN)
              .put("restarted", RESTARTED)
              .put("force restarted", FORCE_RESTARTED)
              .put("kernel panicked", KERNEL_PANICKED)
              .put("kernel panicked without reconnect", KERNEL_PANICKED_WITHOUT_RECONNECT)
              .put("started", STARTED)
              .put("shut down", SHUT_DOWN)
              .put("destroyed", DESTROYED)
              .put("drained", DRAINED)
              .put("uncordoned", UNCORDONED)
              .put("cordoned", CORDONED)
              .put("power status", POWER_STATUS).freeze();

      /**
       * Execute an action on a node.
       * Delegates to K8sNodeApi.performAction() which has platform-specific implementations.
       */
      public void execute(final K8sNodeApi k8sNode, final String... flags) {
          k8sNode.performAction(this, flags);
      }

      public static NodeAction fromString(final String nodeAction) {
          if (nodeAction == null || nodeAction.isEmpty()) return null;
          return NODE_ACTION_ENUM_MAP_UTIL.get(nodeAction);
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  K8sNode.java — Base Class (Kernel Panic Implementation)

  // File: K8sNode.java — Abstract base class for all node types
  // The kernel panic mechanism is shared across all node types

  public abstract class K8sNode implements K8sNodeApi {

      // Kernel panic command — writes to sysrq-trigger
      // Used by all concrete implementations
      // KERNEL_PANIC_CMD = "sudo sh -c 'echo c > /proc/sysrq-trigger'"

      /**
       * Trigger kernel panic: send async command, wait for disconnect, then reconnect.
       */
      protected void triggerKernelPanic(final String command, final long timeoutS) {
          final long timeoutTimeMs = triggerKernelPanicWithoutReconnect(command, timeoutS);
          reconnectAfterKernelPanic(timeoutTimeMs);
      }

      protected long triggerKernelPanicWithoutReconnect(final String command, final long timeoutS) {
          connect(Duration.ofSeconds(CONNECT_TIMEOUT.getSeconds()));
          final long timeoutTimeMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutS);
          navigator.getExtendedCli()
                  .addInnerDecorator(Decorators.isConnectedWithTimeout(Millis.fromSeconds(timeoutS)));

          LOGGER.info("Triggering a kernel panic on '{}'.", getHostname());
          navigator.getExtendedCli().sendAsync(command);
          Delay.sleep(Duration.ofSeconds(1));

          // Wait for connection loss (proves kernel panic worked)
          final Duration disconnectTimeoutS = Duration.ofMillis(timeoutTimeMs - System.currentTimeMillis());
          if (!Poll.isActionCompleted(() -> !isConnected(), disconnectTimeoutS, CONNECT_INTERVAL)) {
              throw new TestException("Failed to trigger kernel panic on '%s' (didn't lose connection).",
                      getHostname());
          }
          navigator.getExtendedCli().removeInnerDecorator();
          return timeoutTimeMs;
      }

      protected void triggerKernelPanicWithoutVerification(final String command, final long timeoutS) {
          connect(Duration.ofSeconds(CONNECT_TIMEOUT.getSeconds()));
          navigator.getExtendedCli()
                  .addInnerDecorator(Decorators.isConnectedWithTimeout(Millis.fromSeconds(timeoutS)));
          LOGGER.info("Triggering a kernel panic without reconnect on '{}'.", getHostname());
          navigator.getExtendedCli().sendAsync(command);
          Delay.sleep(Duration.ofSeconds(1));
          navigator.getExtendedCli().removeInnerDecorator();
      }

      protected void reconnectAfterKernelPanic(final long timeoutTimeMs) {
          final Duration connectTimeoutS = Duration.ofMillis(timeoutTimeMs - System.currentTimeMillis());
          LOGGER.info("Trying to reestablish connection within {}s after kernel panic on '{}'.",
                  connectTimeoutS.getSeconds(), getHostname());
          connect(connectTimeoutS);
          LOGGER.info("Successfully triggered a kernel panic on '{}'.", getHostname());
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  K8sCeeNode.java — CEE Implementation (OpenStack-backed)

  // File: K8sCeeNode.java — Virtual/CEE node actions via OpenStack

  public class K8sCeeNode extends K8sNode {

      private static final DefaultTimeouts KERNEL_PANIC_TIMEOUT = DefaultTimeouts.TIMEOUT_10_M;
      private static final String KERNEL_PANIC_CMD = "sudo sh -c 'echo c > /proc/sysrq-trigger'";

      @Override
      public void performAction(final NodeAction nodeAction, final String... flags) {
          switch (nodeAction) {
              case RESTARTED:
                  openstackHandler.rebootServer(getHostname());
                  break;
              case FORCE_RESTARTED:
                  flagNodeIfActive();
                  openstackHandler.rebootServerWithForce(getHostname());
                  if (isActive) {
                      connectActiveControlPlane(HARD_REBOOT_TIMEOUT.getDuration());
                      isActive = false;
                  }
                  break;
              case STARTED:
                  openstackHandler.startServer(getHostname());
                  break;
              case SHUT_DOWN:
                  openstackHandler.shutdownServer(getHostname());
                  break;
              case DESTROYED:
                  throw new TestException("Not supported node action DESTROYED");
              case KERNEL_PANICKED:
                  flagNodeIfActive();
                  if (getHostname().contains("director-0") || isActive) {
                      final long timeoutMs = super.triggerKernelPanicWithoutReconnect(
                              KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT.getSeconds());
                      reconnectAfterRebootActiveControlPlane(timeoutMs);
                      isActive = false;
                  } else {
                      super.triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT.getSeconds());
                  }
                  break;
              case KERNEL_PANICKED_WITHOUT_RECONNECT:
                  super.triggerKernelPanicWithoutVerification(KERNEL_PANIC_CMD,
                          KERNEL_PANIC_TIMEOUT.getSeconds());
                  break;
              case DRAINED:
                  kubectlApi.drainNode(getHostname(), flags);
                  break;
              case UNCORDONED:
                  kubectlApi.uncordonNode(getHostname(), flags);
                  break;
              case CORDONED:
                  kubectlApi.cordonNode(getHostname());
                  break;
              default:
                  throw new TestException("Unknown node action.");
          }
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  K8sBaremetalNode.java — Baremetal Implementation (IPMI/CAPI-backed)

  // File: K8sBaremetalNode.java — Physical node actions via IPMI or CAPI

  public class K8sBaremetalNode extends K8sNode {

      private static final String KERNEL_PANIC_CMD = "sudo sh -c 'echo c > /proc/sysrq-trigger'";
      private static final DefaultTimeouts KERNEL_PANIC_TIMEOUT = DefaultTimeouts.TIMEOUT_20_M;

      @Override
      public void performAction(final NodeAction nodeAction, final String... flags) {
          final boolean forceIpmi = impiToolSupportedCCDs.contains(ccdVersion)
                  || (flags.length > 0 && "all".equals(flags[0]));
          final String[] parts = hostname.split("-");
          final String name = parts[parts.length - 2] + "-" + parts[parts.length - 1];
          final String namespace = forceIpmi ? "" : resolveCapiNamespace();
          final boolean useIpmi = forceIpmi || namespace.isEmpty();

          switch (nodeAction) {
              case STARTED:
                  performStart(useIpmi, name, namespace);  // ipmitool.startNode() or kubectl CAPI
                  break;
              case RESTARTED:
                  performRestart(useIpmi, name, namespace); // ipmitool.restartNode() or kubectl CAPI
                  break;
              case FORCE_RESTARTED:
                  ipmitool.restartNodeWithForce(ipAddress);
                  break;
              case SHUT_DOWN:
                  performShutDown(useIpmi, name, namespace); // ipmitool.shutdownNode() or kubectl CAPI
                  break;
              case KERNEL_PANICKED:
                  performKernelPanic(); // triggerKernelPanic(KERNEL_PANIC_CMD, ...)
                  break;
              case KERNEL_PANICKED_WITHOUT_RECONNECT:
                  super.triggerKernelPanicWithoutVerification(KERNEL_PANIC_CMD,
                          KERNEL_PANIC_TIMEOUT.getSeconds());
                  break;
              case DRAINED:
                  kubectlApi.drainNode(getHostname(), flags);
                  break;
              case UNCORDONED:
              case DESTROYED:
                  kubectlApi.uncordonNode(getHostname(), flags);
                  break;
              case CORDONED:
                  kubectlApi.cordonNode(getHostname());
                  break;
              default:
                  throw new TestException("Baremetal does not support node action '%s'", nodeAction);
          }
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  K8sNodeSteps.java — Step Definitions

  // File: K8sNodeSteps.java — Cucumber step definitions for node actions

  @ScenarioScoped
  public class K8sNodeSteps implements TestStepDefinition {

      @Inject
      K8sNodeSteps(final KubectlApiClusterMap kubectlApiClusterMap,
                   final Provider<K8sNodeApisClusterMap> k8sNodeApisClusterMapProvider,
                   final ScenarioRegistryClusterMap scenarioRegistryClusterMap, ...) { ... }

      // --- STEP 1: Node action on worker with specific pods running ---
      // Gherkin: When a worker node with at least one of the following running pods is restarted:
      //   | Pod Prefix       |
      //   | eric-pc-sm       |
      @GeoredTestStep
      @When("a worker node with at least one of the following running pods is {node_action}{cluster_selector}:")
      public void runActionOnWorkerNodeWithPodsWithPrefixes(final NodeAction nodeAction,
              final ClustersToExecute clustersToExecute, final DataTableWrapper table) {
          final String cluster = clustersToExecute.getCurrentCluster();
          final KubectlApi kubectl = kubectlApiClusterMap.get(cluster);
          final String workerNode = getWorkerNodeToPerformActionOn(table, kubectl);
          final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().get(cluster)
                  .getWithHostname(workerNode);
          JcatLoggingApi.setTestInfo("Executing '%s' on node '%s'", nodeAction, k8sNodeApi.getHostname());
          nodeAction.execute(k8sNodeApi);
          scenarioRegistryClusterMap.get(cluster).put(K8sConstants.Common.LATEST_RESTARTED_NODE, workerNode);
      }

      // --- STEP 2: Node action by node type ---
      // Gherkin: When a control-plane node is restarted
      //          When a worker node is kernel panicked
      @TestStep
      @When("a {node_type} node is {node_action}")
      public void runActionOnAnyNodeType(final NodeType nodeType, final NodeAction nodeAction) {
          final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().getFirst()
                  .getAnyWithNodeType(nodeType);
          nodeAction.execute(k8sNodeApi);
          scenarioRegistryClusterMap.getFirst().put(K8sConstants.Common.LATEST_RESTARTED_NODE,
                  k8sNodeApi.getHostname());
      }

      // --- STEP 3: Node action by name pattern (regex) ---
      // Gherkin: When a node with name matching pattern ".*worker-pool1.*" is restarted
      @CriticalTestStep
      @When("a node with name matching pattern {string} is {node_action}")
      public void performNodeActionOnNodeMatchingPattern(final String namePattern, final NodeAction nodeAction) {
          final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().getFirst()
                  .getAnyWithHostnameMatchingPattern(namePattern);
          k8sNodeApi.performAction(nodeAction);
      }

      // --- STEP 4: Node type not used before (avoids repeating same node) ---
      // Gherkin: When a control-plane node not used before is force restarted
      @TestStep
      @When("a {node_type} node not used before is {node_action}")
      public void runActionOnNodeTypeNotUsed(final NodeType nodeType, final NodeAction nodeAction) {
          final List<String> usedNodes = (List<String>) scenarioRegistryClusterMap.getFirst()
                  .getOrDefault(NODES_WITH_PERFORMED_ACTION_KEY, new ArrayList<>());
          final K8sNodeApis unusedNodeApis = k8sNodeApisClusterMapProvider.get().getFirst().stream()
                  .filter(node -> nodeType.equals(node.getNodeType()) && !usedNodes.contains(node.getHostname()))
                  .collect(Collectors.toCollection(K8sNodeApis::create));
          JcatAssertApi.assertFalse("No unused nodes of type " + nodeType, unusedNodeApis.isEmpty());

          final int randomNodeIdx = RandomUtil.random.nextInt(unusedNodeApis.size());
          nodeAction.execute(unusedNodeApis.get(randomNodeIdx));
          usedNodes.add(unusedNodeApis.get(randomNodeIdx).getHostname());
          scenarioRegistryClusterMap.getFirst().put(NODES_WITH_PERFORMED_ACTION_KEY, usedNodes);
          scenarioRegistryClusterMap.getFirst().put(K8sConstants.Common.LATEST_RESTARTED_NODE,
                  unusedNodeApis.get(randomNodeIdx).getHostname());
      }

      // --- STEP 5: Drain + Restart memorized worker node ---
      // Gherkin: When a memorized worker node "memorized-worker" is drained and restarted
      //          When a memorized worker node "memorized-worker" is drained within 1200 seconds and restarted
      @GeoredTestStep
      @When("a memorized worker node {string} is drained{time_limit_s} and restarted{cluster_selector}")
      public void drainAndRestartMemorizedWorkerNode(final String nodeLabel, final int timeLimit,
              final ClustersToExecute clustersToExecute) {
          final String cluster = clustersToExecute.getCurrentCluster();
          final KubectlApi clusterKubectlApi = kubectlApiClusterMap.get(cluster);
          final String workerName = scenarioRegistryClusterMap.get(cluster).get(nodeLabel, String.class);
          final Optional<Node> worker = clusterKubectlApi.getNodes().stream()
                  .filter(node -> node.getName().contains(workerName)
                          && node.getRoles().contains(NodeType.WORKER.toString()))
                  .findFirst();
          worker.ifPresentOrElse(
                  node -> drainAndRestartNode(cluster, clusterKubectlApi, node, timeLimit),
                  () -> JcatAssertApi.fail("No node found matching memorized worker node '%s'", workerName));
      }

      private void drainAndRestartNode(final String cluster, final KubectlApi clusterKubectlApi,
              final Node node, final int timeout) {
          final K8sNodeApi k8sNodeApi = k8sNodeApisClusterMapProvider.get().get(cluster)
                  .getWithHostname(node.getName());

          // 1. Drain (evict pods, make unschedulable)
          final String[] flags = { "--delete-emptydir-data", "--ignore-daemonsets", "--force",
                  String.format("--timeout=%ds", timeout) };
          kubectlApi.drainNode(node.getName(), flags);

          // 2. Restart
          NodeAction.RESTARTED.execute(k8sNodeApi);

          // 3. Uncordon (make schedulable again)
          kubectlApi.uncordonNode(node.getName());

          // 4. Verify pods come back up
          K8sPodPerformer.waitAllPodsComeUp(clusterKubectlApi,
                  k8sNodeApi.getUncordonTimeout(), Duration.ofSeconds(30), ignoredPods);
      }
  }

  ──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────

  Beets Framework — DcgwStepDefinition.java (DCGW Restart)

  // File: DcgwStepDefinition.java — External gateway restart in Beets

  // Gherkin: Given a random DCGW is restarted within 5 minutes
  @TestStep
  @Given("a random DCGW is restarted within {int} {time_unit}")
  public void restartRandomDcgwWithin(final int timeout, final TimeUnit timeUnit) {
      final String dcgwHost = DcgwHelper.getSwitchedNavigatorRandomDcgwIfAllowed(
              dcgwProperties, connectionLogWriter, dcgwNode);
      JcatLoggingApi.setTestInfo("Reloading DCGW '%s'", dcgwHost);
      try {
          DcgwHelper.reloadDcgw(dcgwNode, dcgwHost);
          Delay.sleep(Duration.of(timeout, timeUnit.toChronoUnit()));
          dcgwNode.connect();
          DcgwHelper.verifyDcgwUp(
                  String.format("Reload failed within timeout %s %s", timeout, timeUnit),
                  dcgwNode, dcgwHost);
      } finally {
          dcgwNode.restoreToPreviousNavigator();
      }
      JcatLoggingApi.setTestInfo("Successfully reloaded '%s'", dcgwHost);
  }
