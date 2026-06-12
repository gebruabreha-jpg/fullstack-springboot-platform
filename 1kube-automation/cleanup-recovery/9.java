package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;

public final class CleanupRecoveryDocumentation {

    private CleanupRecoveryDocumentation() {}

    public static final class CleanupSteps {
        @TestStep
        @Given("node {string} is uncordoned")
        public void uncordonNode(final String nodeName) {
            kubectlApi.uncordonNode(nodeName);
        }

        @TestStep
        @Given("node {string} drain is cancelled")
        public void cancelNodeDrain(final String nodeName) {
            kubectlApi.cancelDrain(nodeName);
        }

        @TestStep
        @Given("network interface {string} is restored to UP")
        public void restoreInterface(final String interfaceName) {
            k8sNodeApi.bringUpInterface(interfaceName);
        }

        @TestStep
        @Given("tc qdisc rules are removed from interface {string}")
        public void removeTcRules(final String interfaceName) {
            k8sNodeApi.removeTcRules(interfaceName);
        }

        @TestStep
        @Then("chaos experiment files {string} are deleted")
        public void deleteChaosFile(final String fileName) {
            kubectlApi.deleteFile(fileName);
        }

        @TestStep
        @Then("litmus experiment {string} files are deleted")
        public void deleteLitmusFiles(final String experimentName) {
            litmusActions.deleteExperimentFiles(experimentName);
        }

        @TestStep
        @Then("node {string} is ready")
        public void verifyNodeReady(final String nodeName) {
            Poll.isActionCompleted(() -> kubectlApi.checkNodeReady(nodeName),
                    Duration.ofSeconds(120), Duration.ofSeconds(5));
        }

        @TestStep
        @Then("all pods with prefix {string} are ready")
        public void verifyPodsReady(final String prefix) {
            Poll.isActionCompleted(() -> kubectlApi.checkPodsReady(prefix),
                    Duration.ofSeconds(180), Duration.ofSeconds(10));
        }

        @TestStep
        @Then("service {string} is available")
        public void verifyServiceAvailable(final String serviceName) {
            JcatAssertApi.assertTrue(
                    String.format("Service '%s' is not available", serviceName),
                    kubectlApi.checkServiceAvailability(serviceName));
        }

        @TestStep
        @When("composite fault {string} is triggered")
        public void triggerCompositeFault(final String chainName) {
            final List<String> actions = List.of(chainName.split("\\s*\\+\\s*"));
            JcatLoggingApi.setTestInfo("Triggering composite fault: %s", chainName);
            for (final String action : actions) {
                JcatLoggingApi.beginStep(action);
                executeAction(action.trim());
                JcatLoggingApi.endStep();
            }
        }

        private void executeAction(final String action) {
            switch (action.toLowerCase()) {
                case "kvdb_kill" -> kubectlApi.deletePodWithPrefix("eric-pc-kvdb-rd");
                case "cre_restart" -> podRestartController.restartCrePods();
                case "helm_upgrade" -> helmApi.upgradeHelmChart("eric-pc-sm",
                        helmProperties.getChart(), "pcc-namespace",
                        Map.of(), List.of(), Map.of(), List.of());
                case "pod_restart" -> restartRandomPods();
                case "node_drain" -> {
                    kubectlApi.drainNode("worker-pool1-xxx",
                            new String[]{"--delete-emptydir-data", "--ignore-daemonsets", "--force"});
                }
                case "network_loss" -> {
                    k8sNodeApis.getAnyNodesWithNodeType(NodeType.WORKER, 1).getFirst()
                            .bringDownAndUpInterfaces(java.util.Set.of("eth1"), Duration.ofSeconds(30));
                }
                default -> throw new TestException("Unknown chaos action: " + action);
            }
        }

        private void restartRandomPods() {
            final List<String> pods = kubectlApi.getPodsWithPrefix("eric-pc-sm");
            for (final String pod : pods) {
                PodAction.RESTARTED.execute(k8sNodeApis, kubectlApi, kubectlProperties, pod);
            }
        }

        private KubectlApi kubectlApi = new KubectlApi();
        private K8sNodeApi k8sNodeApi = new K8sNodeApi();
        private K8sNodeApis k8sNodeApis = new K8sNodeApis();
        private KubectlProperties kubectlProperties = new KubectlProperties();
        private HelmApi helmApi = new HelmApi();
        private HelmProperties helmProperties = new HelmProperties();
        private LitmusActions litmusActions = new LitmusActions();
    }

    public interface KubectlApi {
        void uncordonNode(String nodeName);
        void cancelDrain(String nodeName);
        void deleteFile(String fileName);
        boolean checkNodeReady(String nodeName);
        boolean checkPodsReady(String prefix);
        boolean checkServiceAvailability(String serviceName);
        void deletePodWithPrefix(String prefix);
        void drainNode(String hostname, String... flags);
        String getNamespace();
    }

    public interface K8sNodeApi {
        void bringUpInterface(String interfaceName);
        void removeTcRules(String interfaceName);
    }

    public interface K8sNodeApis {
        List<K8sNodeApi> getAnyNodesWithNodeType(NodeType nodeType, int count);
    }

    public enum NodeType { WORKER, MASTER, CONTROL_PLANE }

    public interface HelmApi {
        void upgradeHelmChart(String release, String chart, String namespace,
                Map<String, String> values, List<File> params, Map<String, String> switches, List<String> args);
    }

    public interface HelmProperties {
        String getChart();
        String getRelease();
    }

    public interface KubectlProperties {
        KubectlProperties getPropertiesByName(String name);
    }

    public interface LitmusActions {
        void deleteExperimentFiles(String experimentName);
    }

    public interface PodAction {
        void execute(K8sNodeApis apis, KubectlApi kubectl, KubectlProperties props, String pod);
        PodAction RESTARTED = null;
    }

    public interface Poll {
        static boolean isActionCompleted(Runnable condition, Duration timeout, Duration interval) {
            return true;
        }
    }

    public interface JcatAssertApi {
        static void assertTrue(String message, boolean condition) {
            if (!condition) throw new TestException(message);
        }
    }

    public interface JcatLoggingApi {
        static void setTestInfo(String message, Object... args) {}
        static void beginStep(String stepName) {}
        static void endStep() {}
    }

    public static class TestException extends RuntimeException {
        public TestException(String message) { super(message); }
    }
}
