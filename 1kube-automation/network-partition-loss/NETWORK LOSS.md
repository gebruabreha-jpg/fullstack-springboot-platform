Here is the complete implementation across both frameworks, consolidated by action:

## 1. Bring Down Network Interface (Node Interface Control)

PCC Framework — Step Definition (K8sNodeSteps.java:3258)

@GeoredTestStep
@Then("bring down network interface {string} in {int} random {node_type} node(s) for {int} second(s){cluster_selector}")
public void bringDownNetworkInterfaceInRandomNodes(final String interfaceName, final int numberOfNodes,
        final NodeType nodeType, final int downtimeInSeconds, final ClustersToExecute clustersToExecute) {
    final String cluster = clustersToExecute.getCurrentCluster();
    final K8sNodeApis k8sNodeApis = k8sNodeApisClusterMapProvider.get().get(cluster)
            .getAnyNodesWithNodeType(nodeType, numberOfNodes);
    bringDownNetworkInterfaceInNodes(k8sNodeApis, interfaceName, downtimeInSeconds, cluster);
}
PCC Framework — Low-Level Implementation (K8sNode.java:93-97, 296-330)

// Command templates
private static final String INTERFACE_SET_DOWN_CMD = "sudo ip link set %s down";          // line 93
private static final String INTERFACE_SET_UP_CMD = "sudo ip link set %s up";              // line 94
private static final String INTERFACE_SET_DOWN_AND_UP_CMD = "sudo ip link set %s down && sleep %d && sudo ip link set %s up"; // line 95
private static final String INTERFACE_SET_SLEEP_CMD = "sleep %.3f";                        // line 96
private static final String INTERFACE_SET_CHAINED_CMD_DELIMITER = " && ";                  // line 97

@Override
public void bringDownInterface(final String interfaceName) {                              // line 296
    executeCheckResult(String.format(INTERFACE_SET_DOWN_CMD, interfaceName));
}

@Override
public void bringUpInterface(final String interfaceName) {                                // line 302
    executeCheckResult(String.format(INTERFACE_SET_UP_CMD, interfaceName));
}

@Override
public void bringDownAndUpInterface(final String interfaceName, final int timeoutSec) {   // line 307
    executeCheckResult(String.format(INTERFACE_SET_DOWN_AND_UP_CMD, interfaceName, timeoutSec, interfaceName),
            new CommandOptions.Builder().withLinuxResultCode().withRecordTime().withTimeout(3 * timeoutSec).build());
}

@Override
public void bringDownAndUpInterfaces(final Set<String> interfaces, final Duration timeout) { // line 314
    final double sleepSeconds = timeout.getSeconds() + (timeout.getNano() / 1_000_000_000.0);
    final String downCmds = interfaces.stream()
            .map(iface -> String.format(INTERFACE_SET_DOWN_CMD, iface))
            .collect(Collectors.joining(INTERFACE_SET_CHAINED_CMD_DELIMITER));
    final String upCmds = interfaces.stream()
            .map(iface -> String.format(INTERFACE_SET_UP_CMD, iface))
            .collect(Collectors.joining(INTERFACE_SET_CHAINED_CMD_DELIMITER));
    final String downSleepUpCmds = String.join(INTERFACE_SET_CHAINED_CMD_DELIMITER, downCmds,
            String.format(INTERFACE_SET_SLEEP_CMD, sleepSeconds), upCmds);
    final CommandResult result = executeCheckResult(downSleepUpCmds,
            new CommandOptions.Builder().withLinuxResultCode().withRecordTime()
                    .withTimeout((int) TIMEOUT.getSeconds()).build());
}
PCC Framework — Concurrentbring-down (K8sNodeSteps.java:3201)

@GeoredTestStep
@Then("concurrently bring down network interface(s) on the following nodes{cluster_selector}:")
public void concurrentlyBringDownNetworkInterfacesInNodesForDuration(final ClustersToExecute clustersToExecute,
        final DataTableWrapper dataTable) {
    // Uses parallel stream: k8sNodeApis.parallelStream().forEach(k8sNodeApi -> { ... })
    // Calls k8sNodeApi.bringDownAndUpInterfaces(interfaces, duration) per node in parallel
}
2. Drop All Packets on Interfaces (TC-based Packet Loss)
PCC Framework — Step Definition (K8sNodeSteps.java:3069)

@GeoredTestStep
@Then("drop all packets on given interfaces for given duration on the following random nodes{cluster_selector}:")
public void concurrentlyDropAllPacketsOnNetworkInterfacesForDuration(final ClustersToExecute clustersToExecute,
        final DataTableWrapper dataTable) {
    // ... reads DataTable columns: NODE_TYPE, NUMBER_OF_NODES, INTERFACES, DURATION, TIME_UNIT
    // Calls k8sNodeApi.dropAllPacketsOnInterfaces(interfaces, timeout, startTimeMillis)
}
PCC Framework — Low-Level Implementation (K8sNode.java:332-343)

@Override
public void dropAllPacketsOnInterfaces(final Set<String> interfaces, final Duration timeout,
        final long startTimeMillis) {                       // line 332
    final String command = BashScheduleBuilder.tcDropAndRestore(interfaces, timeout, startTimeMillis);
    LOGGER.debug("Hostname: {}, Command: {}", getHostname(), command);
    final CommandResult result = executeCheckResult(command,
            new CommandOptions.Builder().withLinuxResultCode().withRecordTime()
                    .withTimeout((int) TIMEOUT.getSeconds()).build());
}
PCC Framework — TC Command Builder (BashScheduleBuilder.java:65-71, 157-167)

// Factory method
public static String tcDropAndRestore(final Set<String> interfaces, final Duration timeout, final long startTime) { // line 65
    return create()
            .addScheduledAction(startTime, "add", interfaces)
            .addScheduledAction(startTime + timeout.toMillis(), "del", interfaces)
            .build();
}

// TC command generator — emits: tc qdisc add dev <iface> root netem loss 100%
private static String buildTcCommand(final ScheduledActionAndInterfaces cmd) {              // line 157
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
3. Litmus Network Chaos Experiments (Beets)
Beets — Enum Definition (LitmusExperiments.java:8-19)

public enum LitmusExperiments {
    POD_CPU_HOG("pod_cpu_hog", "pod-cpu-hog-", "pod_cpu_hog_rbac.yaml", "pod_cpu_hog_experiment.yaml",
            "pod_cpu_hog_engine.yaml", "pod_cpu_hog_engine.template"),
    CONTAINER_KILL("container_kill", "container-kill-", ...),
    POD_NETWORK_PARTITION("pod_network_partition", "pod-network-partition-",
            "pod_network_partition_rbac.yaml", "pod_network_partition_experiment.yaml",
            "pod_network_partition_engine.yaml", "pod_network_partition_engine.template"),
    POD_MEMORY_HOG("pod_memory_hog", "pod-memory-hog-", ...),
    POD_NETWORK_LATENCY("pod_network_latency", "pod-network-latency-",                   // line 14
            "pod_network_latency_rbac.yaml", "pod_network_latency_experiment.yaml",
            "pod_network_latency_engine.yaml", "pod_network_latency_engine.template"),
    POD_NETWORK_CORRUPTION("pod_network_corruption", "pod-network-corruption-",            // line 15
            "pod_network_corruption_rbac.yaml", "pod_network_corruption_experiment.yaml",
            "pod_network_corruption_engine.yaml", "pod_network_corruption_engine.template"),
    POD_NETWORK_LOSS("pod_network_loss", "pod-network-loss-",                              // line 16
            "pod_network_loss_rbac.yaml", "pod_network_loss_experiment.yaml",
            "pod_network_loss_engine.yaml", "pod_network_loss_engine.template"),
    POD_IO_STRESS("pod_io_stress", "pod-io-stress-", ...),
    KUBELET_SERVICE_KILL("kubelet_service_kill", "kubelet-service-kill-", ...),
    DOCKER_SERVICE_KILL("docker_service_kill", "docker-service-kill-", ...);
    // each enum holds: experimentName, podPrefix, rbacFile, experimentFile, engineFile, engineTemplateFile
}
Beets — Step Definition (LitmusExperimentSteps.java:161-183)

@GeoredTestStep
@When("the litmus experiment {} is run{cluster_selector}")
public void runLitmusExperiments(LitmusExperiments experiment, final ClustersToExecute clustersToExecute) {
    final String cluster = clustersToExecute.getCurrentCluster();
    JcatLoggingApi.setTestInfo("Starting experiment: %s on %s", experiment, cluster);
    final LitmusExperimentsActions litmusActions = mappedLitmusExperimentsActions
            .get(clustersToExecute.getCurrentCluster());
    JcatLoggingApi.beginStep("Applying RBAC");
    final String rbacFile = beetsConfigurationProvider.get().getConfiguration()
            .getFile(experiment.getRbacFile());
    litmusActions.applyRbac(rbacFile, experiment.getExperimentName());
    JcatLoggingApi.endStep();
    JcatLoggingApi.beginStep("Creating and setting the experiment");
    final String experimentFile = beetsConfigurationProvider.get().getConfiguration()
            .getFile(experiment.getExperimentFile());
    litmusActions.createExperiment(experimentFile, experiment.getExperimentName());
    JcatLoggingApi.endStep();
    JcatLoggingApi.beginStep("Running the engine to start the experiment");
    final String engineFile = beetsConfigurationProvider.get().getConfiguration()
            .getFile(experiment.getEngineFile());
    litmusActions.startEngine(engineFile, experiment.getExperimentName());
    JcatLoggingApi.endStep();
}
4. TC Netem Rules on Dallas (Beets)
Beets — Step Definition (PccStepDefinition.java:697-720)

@TroubleShootingTestStep
@TestStep
@Then("add TC rule {string} on {dallas_network_name} interface from Dallas master host")
public void addTcRuleOnDallasMaster(final String netemDiscipline, final String dallasNetworkName) {
    String interfaceName = getInterfaceNameFromType(dallasNetworkName);
    toolServerNavigators.get(0).executeCommand(
            String.format(ADD_TC_RULE_DALLAS_MASTER, interfaceName, netemDiscipline), DEFAULT_COMMAND_TIMEOUT);
    JcatLoggingApi.setTestInfo("Rule \"%s\" is added on Dallas interface %s", netemDiscipline, interfaceName);
}

@TroubleShootingTestStep
@TestStep
@Then("delete TC rule on {dallas_network_name} interface from Dallas master host")
public void deleteTcRuleOnDallasMaster(final String dallasNetworkName) {
    String interfaceName = getInterfaceNameFromType(dallasNetworkName);
    toolServerNavigators.get(0).executeCommand(String.format(DEL_TC_RULE_DALLAS_MASTER, interfaceName),
            DEFAULT_COMMAND_TIMEOUT);
    JcatLoggingApi.setTestInfo("Rule is deleted on Dallas interface %s", interfaceName);
}
The underlying commands (used at lines 697-720):

ADD_TC_RULE_DALLAS_MASTER = "tc qdisc add dev %s root %s"
DEL_TC_RULE_DALLAS_MASTER = "tc qdisc del dev %s root"
Summary of How Each Action Is Implemented
Action	Framework	Mechanism	Underlying Command
Bring down interface	PCC	ip link set <iface> down + sleep + ip link set <iface> up	sudo ip link set eth1 down && sleep 5 && sudo ip link set eth1 up
Drop packets on interfaces	PCC	tc qdisc netem loss 100% scheduled add/del	tc qdisc add dev eth1 root netem loss 100% / tc qdisc del dev eth1 root netem
L3 interface disable (DCGW)	Beets	Interface toggling on DCGW nodes	Similar ip link set pattern
DCGW shutdown/start	Beets	Disable/enable all interfaces toward SUT	Bulk ip link commands
POD_NETWORK_LOSS	Beets (Litmus)	Litmus chaos experiment (ChaosMesh/Istio)	Deploys pod-network-loss engine YAML → applies netem in pod
POD_NETWORK_PARTITION	Beets (Litmus)	Litmus experiment via network policies	pod-network-partition engine YAML
POD_NETWORK_LATENCY	Beets (Litmus)	Litmus experiment (ChaosMesh)	pod-network-latency engine YAML
POD_NETWORK_CORRUPTION	Beets (Litmus)	Litmus experiment (ChaosMesh)	pod-network-corruption engine YAML
TC rule (Dallas/direct)	Beets	Direct tc qdisc add dev <iface> root netem ...	tc qdisc add dev signaling root netem loss 5% 25%
TRex traffic generation	Beets (via PCC)	TRexRunner manages traffic gen separately	DPDK-based packet generation, not impairment