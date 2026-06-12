package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class StorageVolumeOperationsDocumentation {

    private StorageVolumeOperationsDocumentation() {}

    public record VolumeRequest(
            String podName,
            String pvcName,
            String pvName,
            String volumeAction) {}

    public enum VolumeAction {
        PVC_DELETED {
            @Override
            public void execute(final KubectlApi kubectlApi, final String pvcName) {
                kubectlApi.deletePVC(pvcName);
            }
        },
        VOLUME_DETACHED {
            @Override
            public void execute(final KubectlApi kubectlApi, final String podName) {
                kubectlApi.deletePodGrace(podName);
            }
        },
        VOLUME_ATTACHED {
            @Override
            public void execute(final KubectlApi kubectlApi, final String podName) {
                kubectlApi.waitForPodReady(podName, Duration.ofSeconds(120));
            }
        },
        SLOW_DISK_SIMULATED {
            @Override
            public void execute(final K8sNodeApi k8sNodeApi, final String interfaceName,
                    final Duration delay, final Duration variance) {
                k8sNodeApi.simulateSlowDisk(interfaceName, delay, variance);
            }
        },
        IO_FAILURE_SIMULATED {
            @Override
            public void execute(final K8sNodeApi k8sNodeApi, final String mountPath) {
                k8sNodeApi.simulateIOFailure(mountPath);
            }
        };

        public abstract void execute(final KubectlApi kubectlApi, final String target);
        public abstract void execute(final K8sNodeApi k8sNodeApi, final String target, final Duration delay,
                final Duration variance);
        public abstract void execute(final K8sNodeApi k8sNodeApi, final String mountPath);
    }

    public static final class VolumeSteps {
        @TestStep
        @When("PVC {string} is deleted")
        public void deletePVC(final String pvcName) {
            VolumeAction.PVC_DELETED.execute(kubectlApi, pvcName);
        }

        @TestStep
        @When("volume is detached from pod {string}")
        public void detachVolumeFromPod(final String podName) {
            VolumeAction.VOLUME_DETACHED.execute(kubectlApi, podName);
        }

        @TestStep
        @When("slow disk is simulated on node for {int} milliseconds{delay_variance}")
        public void simulateSlowDisk(final int delayMs, final Duration variance) {
            VolumeAction.SLOW_DISK_SIMULATED.execute(k8sNodeApi, "sda",
                    Duration.ofMillis(delayMs), variance);
        }

        @TestStep
        @When("I/O failure is simulated on mount path {string}")
        public void simulateIOFailure(final String mountPath) {
            VolumeAction.IO_FAILURE_SIMULATED.execute(k8sNodeApi, mountPath);
        }

        @TestStep
        @Then("PVC {string} is bound and pod using it is running")
        public void verifyPVCAndPodRunning(final String pvcName) {
            final String pvcStatus = kubectlApi.getPVCStatus(pvcName);
            JcatAssertApi.assertTrue(
                    String.format("PVC '%s' is not bound (status: %s)", pvcName, pvcStatus),
                    "Bound".equalsIgnoreCase(pvcStatus));
            final List<String> pods = kubectlApi.getPodsUsingPVC(pvcName);
            for (final String pod : pods) {
                Poll.isActionCompleted(() -> kubectlApi.isPodReady(pod),
                        Duration.ofSeconds(120), Duration.ofSeconds(5));
            }
        }

        private KubectlApi kubectlApi = new KubectlApi();
        private K8sNodeApi k8sNodeApi = new K8sNodeApi();
    }

    public static final class StorageStepDefinition {
        @TestStep
        @When("PVC {string} is deleted")
        public void deletePVC(final String pvcName) {
            kubectlApi.deletePVC(pvcName);
        }

        @TestStep
        @When("volume is detached from pod {string}")
        public void detachVolumeFromPod(final String podName) {
            kubectlApi.deletePodGrace(podName);
        }

        @TestStep
        @When("I/O failure is simulated on mount path {string}")
        public void simulateIOFailure(final String mountPath) {
            k8sNodeApi.simulateIOFailure(mountPath);
        }

        private KubectlApi kubectlApi = new KubectlApi();
        private K8sNodeApi k8sNodeApi = new K8sNodeApi();
    }

    public interface KubectlApi {
        void deletePVC(String pvcName);
        void deletePodGrace(String podName);
        void waitForPodReady(String podName, Duration timeout);
        String getPVCStatus(String pvcName);
        List<String> getPodsUsingPVC(String pvcName);
        boolean isPodReady(String podName);
    }

    public interface K8sNodeApi {
        void simulateSlowDisk(String interfaceName, Duration delay, Duration variance);
        void simulateIOFailure(String mountPath);
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
    public interface When { String value(); }
    public interface Then { String value(); }

    public static class TestException extends RuntimeException {
        public TestException(String message) { super(message); }
        public TestException(String message, Object... args) { super(String.format(message, args)); }
    }
}
