package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

public final class VerificationHealthChecksDocumentation {

    private VerificationHealthChecksDocumentation() {}

    public static final class HealthCheckSteps {
        @TestStep
        @Then("all pods with prefix {string} are ready")
        public void verifyPodsReady(final String prefix) {
            Poll.isActionCompleted(() -> kubectlApi.checkPodsReady(prefix),
                    Duration.ofSeconds(120), Duration.ofSeconds(5));
        }

        @TestStep
        @Then("all pods with prefix {string} are running")
        public void waitForPodsRunning(final String prefix) {
            Poll.isActionCompleted(() -> kubectlApi.waitForPodsRunning(prefix),
                    Duration.ofSeconds(180), Duration.ofSeconds(10));
        }

        @TestStep
        @Then("pod {string} is healthy")
        public void verifyPodHealth(final String podName) {
            final PodHealth health = kubectlApi.getPodHealth(podName);
            JcatAssertApi.assertTrue(
                    String.format("Pod '%s' is not healthy: %s", podName, health.status()),
                    health.healthy());
        }

        @TestStep
        @Then("service {string} has available endpoints")
        public void verifyServiceEndpoints(final String serviceName) {
            final int endpointCount = kubectlApi.getServiceEndpointCount(serviceName);
            JcatAssertApi.assertTrue(
                    String.format("Service '%s' has %d endpoints (expected > 0)", serviceName, endpointCount),
                    endpointCount > 0);
        }

        @TestStep
        @Then("service {string} is available")
        public void verifyServiceAvailability(final String serviceName) {
            JcatAssertApi.assertTrue(
                    String.format("Service '%s' is not available", serviceName),
                    kubectlApi.checkServiceAvailability(serviceName));
        }

        @TestStep
        @Then("link status of interface {string} is {string}")
        public void verifyLinkStatus(final String interfaceName, final String expectedStatus) {
            final String actualStatus = k8sNodeApi.getLinkStatus(interfaceName);
            JcatAssertApi.assertTrue(
                    String.format("Interface '%s' status is '%s', expected '%s'",
                            interfaceName, actualStatus, expectedStatus),
                    expectedStatus.equalsIgnoreCase(actualStatus));
        }

        @TestStep
        @Then("node {string} is ready")
        public void verifyNodeReady(final String nodeName) {
            JcatAssertApi.assertTrue(
                    String.format("Node '%s' is not ready", nodeName),
                    kubectlApi.checkNodeReady(nodeName));
        }

        @TestStep
        @Then("node {string} is not ready")
        public void verifyNodeNotReady(final String nodeName) {
            JcatAssertApi.assertTrue(
                    String.format("Node '%s' is unexpectedly ready", nodeName),
                    kubectlApi.checkNodeNotReady(nodeName));
        }

        @TestStep
        @Then("node {string} has no pressure conditions")
        public void verifyNodeNoPressure(final String nodeName) {
            final List<String> pressures = kubectlApi.checkNodePressure(nodeName);
            JcatAssertApi.assertTrue(
                    String.format("Node '%s' has pressure conditions: %s", nodeName, pressures),
                    pressures.isEmpty());
        }

        @TestStep
        @Then("cluster capacity is sufficient")
        public void verifyClusterCapacity() {
            final ClusterCapacity capacity = kubectlApi.getClusterCapacity();
            JcatAssertApi.assertTrue(
                    String.format("Insufficient cluster capacity: CPU=%s, Memory=%s",
                            capacity.cpuAvailable(), capacity.memoryAvailable()),
                    capacity.cpuAvailable() > 0 && capacity.memoryAvailable() > 0);
        }

        private KubectlApi kubectlApi = new KubectlApi();
        private K8sNodeApi k8sNodeApi = new K8sNodeApi();
    }

    public record PodHealth(String podName, String status, boolean healthy) {}
    public record ClusterCapacity(int cpuAvailable, long memoryAvailable) {}

    public interface KubectlApi {
        boolean checkPodsReady(String prefix);
        boolean waitForPodsRunning(String prefix);
        PodHealth getPodHealth(String podName);
        int getServiceEndpointCount(String serviceName);
        boolean checkServiceAvailability(String serviceName);
        boolean checkNodeReady(String nodeName);
        boolean checkNodeNotReady(String nodeName);
        List<String> checkNodePressure(String nodeName);
        ClusterCapacity getClusterCapacity();
    }

    public interface K8sNodeApi {
        String getLinkStatus(String interfaceName);
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

    public interface TestStep {}
    public interface Then { String value(); }

    public static class TestException extends RuntimeException {
        public TestException(String message) { super(message); }
    }
}
