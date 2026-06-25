package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;

public final class ResourceExhaustionFailuresDocumentation {

    private ResourceExhaustionFailuresDocumentation() {}

    public enum ResourceAction {
        CPU_SATURATED {
            @Override
            public void execute(final KubectlApi kubectlApi, final String podName,
                    final int cpuCores) {
                kubectlApi.execCommand(podName,
                        String.format("stress-ng --cpu %d --timeout 60", cpuCores));
            }
        },
        MEMORY_EXHAUSTED {
            @Override
            public void execute(final KubectlApi kubectlApi, final String podName,
                    final long memoryMB) {
                kubectlApi.execCommand(podName,
                        String.format("stress-ng --vm 1 --vm-bytes %dM --timeout 60", memoryMB));
            }
        },
        DISK_FULL {
            @Override
            public void execute(final KubectlApi kubectlApi, final String podName,
                    final String mountPath, final long fillSizeMB) {
                kubectlApi.execCommand(podName,
                        String.format("dd if=/dev/zero of=%s/fill bs=1M count=%d",
                                mountPath, fillSizeMB));
            }
        },
        EPHEMERAL_STORAGE_FULL {
            @Override
            public void execute(final KubectlApi kubectlApi, final String podName,
                    final long fillSizeMB) {
                kubectlApi.execCommand(podName,
                        String.format("dd if=/dev/zero of=/tmp/fill bs=1M count=%d && sync",
                                fillSizeMB));
            }
        },
        FILE_HANDLE_EXHAUSTED {
            @Override
            public void execute(final KubectlApi kubectlApi, final String podName,
                    final int fileCount) {
                kubectlApi.execCommand(podName,
                        String.format("bash -c 'for i in $(seq 1 %d); do : > /tmp/f$i; done'",
                                fileCount));
            }
        },
        CONNECTION_POOL_EXHAUSTED {
            @Override
            public void execute(final KubectlApi kubectlApi, final String serviceName,
                    final int connectionCount) {
                kubectlApi.exhaustConnections(serviceName, connectionCount);
            }
        };

        public abstract void execute(KubectlApi kubectlApi, String podName, int value);
        public abstract void execute(KubectlApi kubectlApi, String podName, long value);
        public abstract void execute(KubectlApi kubectlApi, String podName, String mountPath, long value);
        public abstract void execute(KubectlApi kubectlApi, String serviceName, int value);
    }

    public static final class ResourceExhaustionSteps {
        @TestStep
        @When("CPU is saturated on pod {string} with {int} cores")
        public void saturateCPU(final String podName, final int cpuCores) {
            ResourceAction.CPU_SATURATED.execute(kubectlApi, podName, cpuCores);
        }

        @TestStep
        @When("memory is exhausted on pod {string} with {int} MB")
        public void exhaustMemory(final String podName, final int memoryMB) {
            ResourceAction.MEMORY_EXHAUSTED.execute(kubectlApi, podName, memoryMB);
        }

        @TestStep
        @When("disk is filled on pod {string} at mount {string} with {int} MB")
        public void fillDisk(final String podName, final String mountPath, final int fillSizeMB) {
            ResourceAction.DISK_FULL.execute(kubectlApi, podName, mountPath, fillSizeMB);
        }

        @TestStep
        @When("ephemeral storage is filled on pod {string} with {int} MB")
        public void fillEphemeralStorage(final String podName, final int fillSizeMB) {
            ResourceAction.EPHEMERAL_STORAGE_FULL.execute(kubectlApi, podName, fillSizeMB);
        }

        @TestStep
        @When("{int} file handles are opened on pod {string}")
        public void exhaustFileHandles(final int fileCount, final String podName) {
            ResourceAction.FILE_HANDLE_EXHAUSTED.execute(kubectlApi, podName, fileCount);
        }

        @TestStep
        @When("connection pool is exhausted for service {string} with {int} connections")
        public void exhaustConnectionPool(final String serviceName, final int connectionCount) {
            ResourceAction.CONNECTION_POOL_EXHAUSTED.execute(kubectlApi, serviceName, connectionCount);
        }

        @TestStep
        @Then("pod {string} is not OOMKilled")
        public void verifyPodNotOOMKilled(final String podName) {
            final String lastState = kubectlApi.getPodLastState(podName);
            JcatAssertApi.assertTrue(
                    String.format("Pod '%s' was OOMKilled", podName),
                    !lastState.contains("OOMKilled"));
        }

        @TestStep
        @Then("pod {string} restarts due to resource exhaustion")
        public void verifyPodRestartsFromExhaustion(final String podName) {
            final int restartCount = kubectlApi.getPodRestartCount(podName);
            JcatAssertApi.assertTrue(
                    String.format("Pod '%s' has not restarted (restarts: %d)", podName, restartCount),
                    restartCount > 0);
        }

        private KubectlApi kubectlApi = new KubectlApi();
    }

    public interface KubectlApi {
        void execCommand(String podName, String command);
        void exhaustConnections(String serviceName, int connectionCount);
        String getPodLastState(String podName);
        int getPodRestartCount(String podName);
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
