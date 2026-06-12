package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Network loss action patterns extracted from the PCC and Beets test frameworks.
 * This file is self-contained for documentation and reference purposes.
 */
public final class NetworkLossActionsDocumentation {

    private static final Duration TIMEOUT = Duration.ofSeconds(300);

    private NetworkLossActionsDocumentation() {
    }

    public static final class K8sNode {
        private static final String INTERFACE_SET_DOWN_CMD = "sudo ip link set %s down";
        private static final String INTERFACE_SET_UP_CMD = "sudo ip link set %s up";
        private static final String INTERFACE_SET_DOWN_AND_UP_CMD = "sudo ip link set %s down && sleep %d && sudo ip link set %s up";
        private static final String INTERFACE_SET_SLEEP_CMD = "sleep %.3f";
        private static final String INTERFACE_SET_CHAINED_CMD_DELIMITER = " && ";

        public void bringDownInterface(final String interfaceName) {
            executeCheckResult(String.format(INTERFACE_SET_DOWN_CMD, interfaceName));
        }

        public void bringUpInterface(final String interfaceName) {
            executeCheckResult(String.format(INTERFACE_SET_UP_CMD, interfaceName));
        }

        public void bringDownAndUpInterface(final String interfaceName, final int timeoutSec) {
            executeCheckResult(String.format(INTERFACE_SET_DOWN_AND_UP_CMD, interfaceName, timeoutSec, interfaceName),
                    new CommandOptions.Builder().withLinuxResultCode().withRecordTime().withTimeout(3 * timeoutSec).build());
        }

        public void bringDownAndUpInterfaces(final Set<String> interfaces, final Duration timeout) {
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

        public void dropAllPacketsOnInterfaces(final Set<String> interfaces, final Duration timeout,
                final long startTimeMillis) {
            final String command = BashScheduleBuilder.tcDropAndRestore(interfaces, timeout, startTimeMillis);
            LOGGER.debug("Hostname: {}, Command: {}", getHostname(), command);
            final CommandResult result = executeCheckResult(command,
                    new CommandOptions.Builder().withLinuxResultCode().withRecordTime()
                            .withTimeout((int) TIMEOUT.getSeconds()).build());
        }

        private CommandResult executeCheckResult(String command) {
            return new CommandResult(true, command, 0, "");
        }

        private CommandResult executeCheckResult(String command, CommandOptions options) {
            return new CommandResult(true, command, options.timeout, "");
        }

        private String getHostname() {
            return "node-1";
        }

        private static final Logger LOGGER = new Logger();
    }

    public static final class BashScheduleBuilder {
        public static String tcDropAndRestore(final Set<String> interfaces, final Duration timeout, final long startTime) {
            return create()
                    .addScheduledAction(startTime, "add", interfaces)
                    .addScheduledAction(startTime + timeout.toMillis(), "del", interfaces)
                    .build();
        }

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

        private static Builder create() {
            return new Builder();
        }

        public static class Builder {
            private final List<ScheduledActionAndInterfaces> actions = new ArrayList<>();

            public Builder addScheduledAction(long startTime, String action, Set<String> interfaces) {
                actions.add(new ScheduledActionAndInterfaces(startTime, action, interfaces));
                return this;
            }

            public String build() {
                return actions.stream()
                        .map(action -> buildTcCommand(action))
                        .collect(Collectors.joining(" && "));
            }
        }

        private static class ScheduledActionAndInterfaces {
            private final long startTime;
            private final String action;
            private final Set<String> interfaces;

            private ScheduledActionAndInterfaces(long startTime, String action, Set<String> interfaces) {
                this.startTime = startTime;
                this.action = action;
                this.interfaces = interfaces;
            }
        }
    }

    public static final class K8sNodeSteps {
        public void concurrentlyBringDownNetworkInterfacesInNodesForDuration(final ClustersToExecute clustersToExecute,
                final DataTableWrapper dataTable) {
            dataTable.verifyHeader("node_type");
            dataTable.verifyHeader("number_of_nodes");
            dataTable.verifyHeader("interfaces");
            dataTable.verifyHeader("duration");
            dataTable.verifyHeader("time_unit");

            for (final Map<String, String> entry : dataTable.getEntries()) {
                final NodeType nodeType = NodeType.valueOf(entry.get("node_type").toUpperCase());
                final int numberOfNodes = Integer.parseInt(entry.get("number_of_nodes"));
                final Set<String> interfaces = Set.of(entry.get("interfaces").split(","));
                final Duration duration = Duration.ofSeconds(Long.parseLong(entry.get("duration")));
                final K8sNodeApis k8sNodeApis = k8sNodeApisClusterMapProvider.get()
                        .get(clustersToExecute.getCurrentCluster())
                        .getAnyNodesWithNodeType(nodeType, numberOfNodes);

                k8sNodeApis.parallelStream().forEach(k8sNodeApi ->
                        k8sNodeApi.bringDownAndUpInterfaces(interfaces, duration));
            }
        }

        public void concurrentlyDropAllPacketsOnNetworkInterfacesForDuration(final ClustersToExecute clustersToExecute,
                final DataTableWrapper dataTable) {
            dataTable.verifyHeader("node_type");
            dataTable.verifyHeader("number_of_nodes");
            dataTable.verifyHeader("interfaces");
            dataTable.verifyHeader("duration");
            dataTable.verifyHeader("time_unit");

            final long startTimeMillis = System.currentTimeMillis();
            for (final Map<String, String> entry : dataTable.getEntries()) {
                final NodeType nodeType = NodeType.valueOf(entry.get("node_type").toUpperCase());
                final int numberOfNodes = Integer.parseInt(entry.get("number_of_nodes"));
                final Set<String> interfaces = Set.of(entry.get("interfaces").split(","));
                final Duration timeout = Duration.ofSeconds(Long.parseLong(entry.get("duration")));
                final K8sNodeApis k8sNodeApis = k8sNodeApisClusterMapProvider.get()
                        .get(clustersToExecute.getCurrentCluster())
                        .getAnyNodesWithNodeType(nodeType, numberOfNodes);

                k8sNodeApis.parallelStream().forEach(k8sNodeApi ->
                        k8sNodeApi.dropAllPacketsOnInterfaces(interfaces, timeout, startTimeMillis));
            }
        }

        public void bringDownNetworkInterfaceInRandomNodes(final String interfaceName, final int numberOfNodes,
                final NodeType nodeType, final int downtimeInSeconds, final ClustersToExecute clustersToExecute) {
            final String cluster = clustersToExecute.getCurrentCluster();
            final K8sNodeApis k8sNodeApis = k8sNodeApisClusterMapProvider.get().get(cluster)
                    .getAnyNodesWithNodeType(nodeType, numberOfNodes);
            final Duration duration = Duration.ofSeconds(downtimeInSeconds);
            k8sNodeApis.parallelStream().forEach(k8sNodeApi ->
                    k8sNodeApi.bringDownAndUpInterface(interfaceName, downtimeInSeconds));
        }

        private K8sNodeApisClusterMapProvider k8sNodeApisClusterMapProvider = new K8sNodeApisClusterMapProvider();
    }

    public enum LitmusExperiments {
        POD_CPU_HOG("pod_cpu_hog", "pod-cpu-hog-", "pod_cpu_hog_rbac.yaml", "pod_cpu_hog_experiment.yaml",
                "pod_cpu_hog_engine.yaml", "pod_cpu_hog_engine.template"),
        CONTAINER_KILL("container_kill", "container-kill-", "container_kill_rbac.yaml", "container_kill_experiment.yaml",
                "container_kill_engine.yaml", "container_kill_engine.template"),
        POD_NETWORK_PARTITION("pod_network_partition", "pod-network-partition-",
                "pod_network_partition_rbac.yaml", "pod_network_partition_experiment.yaml",
                "pod_network_partition_engine.yaml", "pod_network_partition_engine.template"),
        POD_MEMORY_HOG("pod_memory_hog", "pod-memory-hog-", "pod_memory_hog_rbac.yaml", "pod_memory_hog_experiment.yaml",
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
        POD_IO_STRESS("pod_io_stress", "pod-io-stress-", "pod_io_stress_rbac.yaml", "pod_io_stress_experiment.yaml",
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

        LitmusExperiments(String experimentName, String podPrefix, String rbacFile, String experimentFile,
                String engineFile, String engineTemplateFile) {
            this.experimentName = experimentName;
            this.podPrefix = podPrefix;
            this.rbacFile = rbacFile;
            this.experimentFile = experimentFile;
            this.engineFile = engineFile;
            this.engineTemplateFile = engineTemplateFile;
        }

        public String getExperimentName() {
            return experimentName;
        }

        public String getPodPrefix() {
            return podPrefix;
        }

        public String getRbacFile() {
            return rbacFile;
        }

        public String getExperimentFile() {
            return experimentFile;
        }

        public String getEngineFile() {
            return engineFile;
        }

        public String getEngineTemplateFile() {
            return engineTemplateFile;
        }
    }

    public static final class LitmusExperimentSteps {
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

        private Map<String, LitmusExperimentsActions> mappedLitmusExperimentsActions = Map.of();
        private ConfigurationProvider beetsConfigurationProvider = new ConfigurationProvider();
    }

    public static final class PccStepDefinition {
        private static final String ADD_TC_RULE_DALLAS_MASTER = "tc qdisc add dev %s root %s";
        private static final String DEL_TC_RULE_DALLAS_MASTER = "tc qdisc del dev %s root";
        private static final int DEFAULT_COMMAND_TIMEOUT = 300;

        public void addTcRuleOnDallasMaster(final String netemDiscipline, final String dallasNetworkName) {
            String interfaceName = getInterfaceNameFromType(dallasNetworkName);
            toolServerNavigators.get(0).executeCommand(
                    String.format(ADD_TC_RULE_DALLAS_MASTER, interfaceName, netemDiscipline), DEFAULT_COMMAND_TIMEOUT);
            JcatLoggingApi.setTestInfo("Rule \"%s\" is added on Dallas interface %s", netemDiscipline, interfaceName);
        }

        public void deleteTcRuleOnDallasMaster(final String dallasNetworkName) {
            String interfaceName = getInterfaceNameFromType(dallasNetworkName);
            toolServerNavigators.get(0).executeCommand(String.format(DEL_TC_RULE_DALLAS_MASTER, interfaceName),
                    DEFAULT_COMMAND_TIMEOUT);
            JcatLoggingApi.setTestInfo("Rule is deleted on Dallas interface %s", interfaceName);
        }

        private String getInterfaceNameFromType(String dallasNetworkName) {
            return dallasNetworkName.toLowerCase().replace(" ", "");
        }

        private List<Navigator> toolServerNavigators = List.of(new Navigator());
    }

    public interface ClustersToExecute {
        String getCurrentCluster();
    }

    public interface DataTableWrapper {
        void verifyHeader(String header);

        List<Map<String, String>> getEntries();
    }

    public enum NodeType {
        WORKER,
        MASTER,
        CONTROL_PLANE
    }

    public interface K8sNodeApis extends List<K8sNodeApi> {
        default K8sNodeApis parallelStream() {
            return this;
        }
    }

    public interface K8sNodeApi {
        void bringDownAndUpInterfaces(Set<String> interfaces, Duration duration);

        void bringDownAndUpInterface(String interfaceName, int timeoutSec);

        void dropAllPacketsOnInterfaces(Set<String> interfaces, Duration timeout, long startTimeMillis);
    }

    public interface K8sNodeApisClusterMapProvider {
        default Map<String, K8sNodeApis> get() {
            return Map.of();
        }
    }

    public interface CommandResult {
        boolean isSuccessful();

        String getCommand();

        int getTimeout();

        String getOutput();

        default long getStartTime() {
            return 0;
        }

        default long getEndTime() {
            return 0;
        }
    }

    public static class CommandOptions {
        private int timeout;

        public static class Builder {
            private final CommandOptions options = new CommandOptions();

            public Builder withLinuxResultCode() {
                return this;
            }

            public Builder withRecordTime() {
                return this;
            }

            public Builder withTimeout(int timeout) {
                options.timeout = timeout;
                return this;
            }

            public CommandOptions build() {
                return options;
            }
        }
    }

    public interface Logger {
        default void debug(String message, Object... args) {
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

    public interface LitmusExperimentsActions {
        void applyRbac(String rbacFile, String experimentName);

        void createExperiment(String experimentFile, String experimentName);

        void startEngine(String engineFile, String experimentName);
    }

    public interface ConfigurationProvider {
        default Configuration get() {
            return new Configuration();
        }
    }

    public interface Configuration {
        default String getFile(String fileName) {
            return fileName;
        }
    }

    public interface Navigator {
        default void executeCommand(String command, int timeout) {
        }
    }

    public static class Logger {
        public void debug(String message, Object... args) {
        }
    }
}
