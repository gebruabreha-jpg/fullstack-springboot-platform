package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;
import java.util.List;

public final class ObservabilityDebuggingDocumentation {

    private ObservabilityDebuggingDocumentation() {}

    public static final class ObservabilitySteps {
        @TestStep
        @When("logs of pod {string} are collected")
        public void collectPodLogs(final String podName) {
            final String logs = kubectlApi.getPodLogs(podName);
            fetchLogHelper.storeLogs(podName, logs);
        }

        @TestStep
        @When("describe output of pod {string} is collected")
        public void collectPodDescribe(final String podName) {
            final String describe = kubectlApi.describePod(podName);
            fetchLogHelper.storeDescribe(podName, describe);
        }

        @TestStep
        @When("resource metrics of pod {string} are collected")
        public void collectPodMetrics(final String podName) {
            final String topOutput = kubectlApi.topPod(podName);
            fetchLogHelper.storeMetrics(podName, topOutput);
        }

        @TestStep
        @When("resource metrics of node {string} are collected")
        public void collectNodeMetrics(final String nodeName) {
            final String topOutput = kubectlApi.topNode(nodeName);
            fetchLogHelper.storeMetrics(nodeName, topOutput);
        }

        @TestStep
        @When("events in namespace {string} are collected")
        public void collectNamespaceEvents(final String namespace) {
            final String events = kubectlApi.getEvents(namespace);
            fetchLogHelper.storeEvents(namespace, events);
        }

        @TestStep
        @When("recent failures are fetched")
        public void collectRecentFailures() {
            final List<String> failures = kubectlApi.getRecentFailures();
            fetchLogHelper.storeFailures(failures);
        }

        @TestStep
        @Then("logs of pod {string} contain {string}")
        public void verifyPodLogsContain(final String podName, final String expectedText) {
            final String logs = kubectlApi.getPodLogs(podName);
            JcatAssertApi.assertTrue(
                    String.format("Pod '%s' logs do not contain '%s'", podName, expectedText),
                    logs.contains(expectedText));
        }

        private KubectlApi kubectlApi = new KubectlApi();
        private FetchLogHelper fetchLogHelper = new FetchLogHelper();
    }

    public interface KubectlApi {
        String getPodLogs(String podName);
        String describePod(String podName);
        String topPod(String podName);
        String topNode(String nodeName);
        String getEvents(String namespace);
        List<String> getRecentFailures();
    }

    public interface FetchLogHelper {
        void storeLogs(String podName, String logs);
        void storeDescribe(String podName, String describe);
        void storeMetrics(String target, String metrics);
        void storeEvents(String namespace, String events);
        void storeFailures(List<String> failures);
    }

    public interface TestStep {}
    public interface When { String value(); }
    public interface Then { String value(); }

    public interface JcatAssertApi {
        static void assertTrue(String message, boolean condition) {
            if (!condition) throw new TestException(message);
        }
    }

    public static class TestException extends RuntimeException {
        public TestException(String message) { super(message); }
    }
}
