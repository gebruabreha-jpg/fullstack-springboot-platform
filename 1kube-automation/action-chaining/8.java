package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;
import java.util.List;

public final class ActionChainingDocumentation {

    private ActionChainingDocumentation() {}

    public record ChainRequest(
            String chainName,
            List<String> actions,
            Duration interval,
            boolean parallel) {}

    public static final class ChainSteps {

        public void runChaosChain(final String chainName, final String... actions) {
            final List<ChaosAction> chaosActions = List.of(actions).stream()
                    .map(ActionCatalog::fromString)
                    .toList();
            final ChaosChain chain = new ChaosChain(chainName, chaosActions, 1);
            chain.execute();
        }

        @TestStep
        @When("chaos chain {chain_name} is executed with interval {chain_interval} seconds")
        public void executeChaosChain(final String chainName, final int intervalSec) {
            final List<ChaosAction> actions = parseChain(chainName);
            final ChaosChain chain = new ChaosChain(chainName, actions, intervalSec);
            chain.execute();
        }

        @TestStep
        @When("composite fault {chain_name} is triggered")
        public void triggerCompositeFault(final String chainName) {
            final List<ChaosAction> actions = parseChain(chainName);
            final ChaosChain chain = new ChaosChain(chainName, actions, 1);
            chain.execute();
        }

        private List<ChaosAction> parseChain(final String chainName) {
            return List.of(chainName.split("\\s*\\+\\s*")).stream()
                    .map(String::trim)
                    .map(ActionCatalog::fromString)
                    .toList();
        }

        public enum ActionCatalog {
            KVDB_KILL("kvdb_kill", new KvdbKillAction()),
            CRE_RESTART("cre_restart", new CreRestartAction()),
            HELM_UPGRADE("helm_upgrade", new HelmUpgradeAction()),
            POD_RESTART("pod_restart", new PodRestartAction()),
            NODE_DRAIN("node_drain", new NodeDrainAction()),
            NETWORK_LOSS("network_loss", new NetworkLossAction()),
            DCGW_SHUTDOWN("dcgw_shutdown", new DcgwShutdownAction()),
            IFACE_RECOVERY("iface_recovery", new InterfaceRecoveryAction());

            private final String key;
            private final ChaosAction action;

            ActionCatalog(String key, ChaosAction action) {
                this.key = key;
                this.action = action;
            }

            public static ChaosAction fromString(final String s) {
                for (final ActionCatalog c : values()) {
                    if (c.key.equalsIgnoreCase(s.trim())) return c.action;
                }
                throw new TestException("Unknown chaos action: " + s);
            }
        }

        public interface ChaosAction {
            void execute();
            void verify();
        }

        public static final class ChaosChain {
            private final String name;
            private final List<ChaosAction> actions;
            private final int intervalSec;
            private final ThreadLocal<Integer> index = new ThreadLocal<>();

            public ChaosChain(String name, List<ChaosAction> actions, int intervalSec) {
                this.name = name;
                this.actions = actions;
                this.intervalSec = intervalSec;
            }

            public void execute() {
                JcatLoggingApi.setTestInfo("Starting chaos chain: %s", name);
                for (int i = 0; i < actions.size(); i++) {
                    index.set(i);
                    final ChaosAction action = actions.get(i);
                    JcatLoggingApi.beginStep(String.format("[%s] Step %d/%d: %s",
                            name, i + 1, actions.size(), action.getClass().getSimpleName()));
                    action.execute();
                    if (i < actions.size() - 1) {
                        Delay.sleep(Duration.ofSeconds(intervalSec));
                    }
                    action.verify();
                    JcatLoggingApi.endStep();
                }
                JcatLoggingApi.setTestInfo("Chaos chain completed: %s", name);
            }
        }

        public static final class KvdbKillAction implements ChaosAction {
            @Override
            public void execute() {
                kubectlApiClusterMap.getFirst().deletePodWithPrefix("eric-pc-kvdb-rd");
            }
            @Override
            public void verify() {
                Poll.isActionCompleted(() -> kubectlApiClusterMap.getFirst()
                                .checkResourcesUp("eric-pc-kvdb-rd"),
                        Duration.ofSeconds(120), Duration.ofSeconds(5));
            }
        }

        public static final class CreRestartAction implements ChaosAction {
            @Override
            public void execute() {
                podRestartController.restartCrePods();
            }
            @Override
            public void verify() {
                podRestartController.verifyCreReady();
            }
        }

        public static final class HelmUpgradeAction implements ChaosAction {
            @Override
            public void execute() {
                helmApiClusterMap.getFirst().upgradeHelmChart("eric-pc-sm",
                        helmProperties.getChart(), "pcc-namespace",
                        Map.of("global.tag", "latest"), List.of(),
                        Map.of("timeout", "600s"), List.of("--atomic"));
            }
            @Override
            public void verify() {
                Poll.isActionCompleted(() -> helmApiClusterMap.getFirst()
                                .getHelmStatus("eric-pc-sm", "pcc-namespace")
                                .contains("deployed"),
                        Duration.ofSeconds(300), Duration.ofSeconds(10));
            }
        }

        public static final class PodRestartAction implements ChaosAction {
            @Override
            public void execute() {
                final List<String> pods = kubectlApiClusterMap.getFirst()
                        .getPodsWithPrefix("eric-pc-sm");
                for (final String pod : pods) {
                    PodAction.RESTARTED.execute(k8sNodeApisClusterMap.getFirst(),
                            kubectlApiClusterMap.getFirst(),
                            kubectlProperties.getPropertiesByName("default"), pod);
                    Delay.sleep(Duration.ofSeconds(5));
                }
            }
            @Override
            public void verify() {
                Poll.isActionCompleted(() -> kubectlApiClusterMap.getFirst()
                                .checkResourcesUp("eric-pc-sm"),
                        Duration.ofSeconds(180), Duration.ofSeconds(10));
            }
        }

        public static final class NodeDrainAction implements ChaosAction {
            @Override
            public void execute() {
                kubectlApiClusterMap.getFirst().drainNode("worker-pool1-xxx",
                        new String[]{"--delete-emptydir-data", "--ignore-daemonsets", "--force"});
            }
            @Override
            public void verify() {
                kubectlApiClusterMap.getFirst().uncordonNode("worker-pool1-xxx");
                Poll.isActionCompleted(() -> kubectlApiClusterMap.getFirst()
                                .checkNodeReady("worker-pool1-xxx"),
                        Duration.ofSeconds(120), Duration.ofSeconds(5));
            }
        }

        public static final class NetworkLossAction implements ChaosAction {
            @Override
            public void execute() {
                final List<K8sNodeApi> nodes = k8sNodeApisClusterMap.getFirst()
                        .getAnyNodesWithNodeType(NodeType.WORKER, 1);
                nodes.getFirst().bringDownAndUpInterfaces(
                        java.util.Set.of("eth1"), Duration.ofSeconds(30));
            }
            @Override
            public void verify() {
                kubectlApiClusterMap.getFirst().checkServiceAvailability("eric-pc-sm-svc");
            }
        }

        public static final class DcgwShutdownAction implements ChaosAction {
            @Override
            public void execute() {
                DcgwHelper.switchNavigatorToAGivenDcgwIfAllowed(
                        dcgwProperties, connectionLogWriter, dcgwNode,
                        dcgwProperties.getHost());
                DcgwHelper.simulateDcgwStartOrShutdown(
                        dcgwProperties.getHost(), dcgwNode, false,
                        PropertyFactory.getStpProperty());
            }
            @Override
            public void verify() {
                DcgwHelper.verifyDcgwDown(dcgwNode, dcgwProperties.getHost());
            }
        }

        public static final class InterfaceRecoveryAction implements ChaosAction {
            @Override
            public void execute() {
                DcgwHelper.switchNavigatorToAGivenDcgwIfAllowed(
                        dcgwProperties, connectionLogWriter, dcgwNode,
                        dcgwProperties.getHost());
                DcgwHelper.simulateDcgwStartOrShutdown(
                        dcgwProperties.getHost(), dcgwNode, true,
                        PropertyFactory.getStpProperty());
            }
            @Override
            public void verify() {
                DcgwHelper.verifyDcgwUp("DCGW recovery failed",
                        dcgwNode, dcgwProperties.getHost());
            }
        }

        public static class PodRestartController {
            public void restartCrePods() {
                final List<String> podList = kubectlApiClusterMap.getFirst()
                        .getPodsWithPattern(java.util.regex.Pattern.compile("cre-pod.*"));
                for (final String pod : podList) {
                    ActionsHelper.killContainerInPod(pod, "confd",
                            kubectlApiClusterMap.getFirst(),
                            k8sNodeApisClusterMap.getFirst());
                }
            }
            public void verifyCreReady() {
                Poll.isActionCompleted(() -> kubectlApiClusterMap.getFirst()
                                .checkResourcesUp("cre"),
                        Duration.ofSeconds(180), Duration.ofSeconds(10));
            }
        }

        private Map<String, KubectlApi> kubectlApiClusterMap = Map.of("default", new KubectlApi());
        private Map<String, K8sNodeApis> k8sNodeApisClusterMap = Map.of("default", new K8sNodeApis());
        private Map<String, KubectlProperties> kubectlProperties = Map.of("default", new KubectlProperties());
        private HelmApi helmApiClusterMap = new HelmApi();
        private HelmProperties helmProperties = new HelmProperties();
        private DcgwProperties dcgwProperties = new DcgwProperties();
        private DcgwNode dcgwNode = new DcgwNode();
        private ConnectionLogWriter connectionLogWriter = new ConnectionLogWriter();
        private PropertyFactory propertyFactory = new PropertyFactory();
    }

    public record KvdbKillAction(String actionId) implements ChaosAction {
        @Override
        public void execute() {
            kubectlApi.deletePodWithPrefix("eric-pc-kvdb-rd");
        }
        @Override
        public void verify() {
            Poll.isActionCompleted(() -> kubectlApi.checkResourcesUp("eric-pc-kvdb-rd"),
                    Duration.ofSeconds(120), Duration.ofSeconds(5));
        }
    }

    public record CreRestartAction(String actionId) implements ChaosAction {
        @Override
        public void execute() {
            podRestartController.restartCrePods();
        }
        @Override
        public void verify() {
            podRestartController.verifyCreReady();
        }
    }

    public record HelmUpgradeAction(String actionId) implements ChaosAction {
        @Override
        public void execute() {
            helmApi.upgradeHelmChart("eric-pc-sm",
                    helmProperties.getChart(), "pcc-namespace",
                    Map.of("global.tag", "latest"), List.of(),
                    Map.of("timeout", "600s"), List.of("--atomic"));
        }
        @Override
        public void verify() {
            Poll.isActionCompleted(() -> helmApi.getHelmStatus("eric-pc-sm", "pcc-namespace")
                            .contains("deployed"),
                    Duration.ofSeconds(300), Duration.ofSeconds(10));
        }
    }

    public interface Delay {
        static void sleep(Duration duration) {}
    }

    public interface Poll {
        static boolean isActionCompleted(Runnable condition, Duration timeout, Duration interval) {
            return true;
        }
    }

    public interface TestStep {}
    public interface When { String value(); }

    public interface JcatLoggingApi {
        static void setTestInfo(String message, Object... args) {}
        static void beginStep(String stepName) {}
        static void endStep() {}
    }

    public interface KubectlApi {
        void deletePodWithPrefix(String prefix);
        boolean checkResourcesUp(String prefix);
        String getPodsWithPrefix(String prefix);
        String[] getContainers(String podName);
        void drainNode(String hostname, String... flags);
        void uncordonNode(String hostname);
        boolean checkNodeReady(String nodeName);
        boolean checkServiceAvailability(String serviceName);
    }

    public interface K8sNodeApis extends java.util.List<K8sNodeApi> {
        K8sNodeApi getFirst();
        List<K8sNodeApi> getAnyNodesWithNodeType(NodeType nodeType, int count);
    }

    public enum NodeType { WORKER, MASTER, CONTROL_PLANE }

    public interface K8sNodeApi {
        void bringDownAndUpInterfaces(java.util.Set<String> interfaces, Duration duration);
    }

    public interface HelmApi {
        void upgradeHelmChart(String release, String chart, String namespace,
                Map<String, String> values, List<File> params, Map<String, String> switches, List<String> args);
        String getHelmStatus(String release, String namespace);
    }

    public interface HelmProperties {
        String getChart();
        String getRelease();
    }

    public interface KubectlProperties {
        KubectlProperties getPropertiesByName(String name);
    }

    public interface DcgwProperties {
        String getHost();
        List<String> getHosts();
    }

    public interface DcgwNode {
        void connect();
        void restoreToPreviousNavigator();
    }

    public interface ConnectionLogWriter {}

    public interface PropertyFactory {
        String getStpProperty();
    }

    public interface DcgwHelper {
        static void switchNavigatorToAGivenDcgwIfAllowed(DcgwProperties p, ConnectionLogWriter w, DcgwNode n, String h) {}
        static void simulateDcgwStartOrShutdown(String host, DcgwNode node, boolean start, String stp) {}
        static void verifyDcgwUp(String msg, DcgwNode node, String host) {}
        static void verifyDcgwDown(DcgwNode node, String host) {}
    }

    public interface Map<K, V> {
        V get(Object key);
    }

    public static class TestException extends RuntimeException {
        public TestException(String message) { super(message); }
        public TestException(String message, Object... args) { super(String.format(message, args)); }
    }
}
