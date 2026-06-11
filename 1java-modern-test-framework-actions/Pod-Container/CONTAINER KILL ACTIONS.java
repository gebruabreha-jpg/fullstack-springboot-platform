package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ericsson.commonlibrary.linux.commands.kill.KillSignal;

/**
 * Container kill action patterns extracted from the PCC test framework.
 * This file is self-contained for documentation and reference purposes.
 */
public final class ContainerKillActionsDocumentation {

    private static final String CONTAINERD_ID_PREFIX = "containerd://";
    private static final String DOCKERD_ID_PREFIX = "docker://";
    private static final String UNKNOWN_CONTAINER_ID_PREFIX_S = "Unknown container id prefix: %s";

    private ContainerKillActionsDocumentation() {
    }

    public record KillRequest(
            String podName,
            String containerName,
            String containerId,
            KillSignal signal) {
    }

    public enum PodAction {
        DELETED {
            @Override
            public void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
                    final KubectlProperties kubectlProperties, final String podName) {
                final int timeoutToRestore = kubectlApi.getShellTimeout();
                try {
                    kubectlApi.setShellTimeout(kubectlProperties.getShellTimeoutMsForDelete());
                    kubectlApi.deletePodGrace(podName);
                } finally {
                    kubectlApi.setShellTimeout(timeoutToRestore);
                }
            }
        },

        FORCE_DELETED {
            @Override
            public void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
                    final KubectlProperties kubectlProperties, final String podName) {
                final int timeoutToRestore = kubectlApi.getShellTimeout();
                try {
                    kubectlApi.setShellTimeout(kubectlProperties.getShellTimeoutMsForDelete());
                    kubectlApi.deletePodForce(podName);
                } finally {
                    kubectlApi.setShellTimeout(timeoutToRestore);
                }
            }
        },

        RESTARTED {
            @Override
            public void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
                    final KubectlProperties kubectlProperties, final String podName) {
                final List<KillRequest> killRequests;
                final String nodeName;
                synchronized (KubeCtl.class) {
                    killRequests = Arrays.stream(kubectlApi.getContainers(podName))
                            .map(containerName -> new KillRequest(podName, containerName,
                                    kubectlApi.getContainerId(podName, containerName), KillSignal.SIGKILL))
                            .toList();
                    nodeName = kubectlApi.getNodeName(podName);
                }
                final K8sNodeApi k8sNodeApi = k8sNodeApis.getWithHostname(nodeName);
                k8sNodeApi.killContainers(killRequests);

                final Duration restartTimeoutInSec = k8sNodeApi.getContainerRestartTimeout();
                final Duration intervalInSec = Duration.ofSeconds(1);
                final boolean restartFinished = Poll.isActionCompleted(
                        () -> !kubectlApi.checkResourcesUp(podName), restartTimeoutInSec, intervalInSec);
                JcatAssertApi.assertTrue(
                        String.format("'%s' action not finished within '%d' seconds", RESTARTED, restartTimeoutInSec.getSeconds()),
                        restartFinished);
            }
        };

        public abstract void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
                final KubectlProperties kubectlProperties, final String podName);
    }

    public interface K8sNodeApis {
        K8sNodeApi getWithHostname(String hostname);
    }

    public interface K8sNodeApi {
        void killContainers(List<KillRequest> killRequests);

        void killContainers(List<String> containerIds, KillSignal signal);

        Duration getContainerRestartTimeout();
    }

    public interface KubectlApi {
        String[] getContainers(String podName);

        String getContainerId(String podName, String containerName);

        String getNodeName(String podName);

        boolean checkResourcesUp(String podName);

        int getShellTimeout();

        void setShellTimeout(int timeoutMs);

        void deletePodGrace(String podName);

        void deletePodForce(String podName);
    }

    public interface KubectlProperties {
        int getShellTimeoutMsForDelete();
    }

    public interface TimeRegistry {
        void put(String key, ZonedDateTime value);
    }

    public interface Constants {
        String KVDB_PODS_KILLED_TIMESTAMP = "KVDB_PODS_KILLED_TIMESTAMP";
    }

    public interface CommandResult {
        boolean isSuccessful();
    }

    public interface ExtendedShellNavigator {
    }

    public interface KubeCtl {
    }

    public interface Poll {
        static boolean isActionCompleted(Runnable action, Duration timeout, Duration interval) {
            return true;
        }
    }

    public interface JcatAssertApi {
        static void assertTrue(String message, boolean condition) {
            if (!condition) {
                throw new TestException(message);
            }
        }
    }

    public static class TestException extends RuntimeException {
        public TestException(String message) {
            super(message);
        }

        public TestException(String message, Object... args) {
            super(String.format(message, args));
        }
    }

    public static class K8sNodeApiImplementation implements K8sNodeApi {
        private static final String CONTAINERD_ID_PREFIX = "containerd://";
        private static final String DOCKERD_ID_PREFIX = "docker://";
        private static final String UNKNOWN_CONTAINER_ID_PREFIX_S = "Unknown container id prefix: %s";

        @Override
        public void killContainers(final List<KillRequest> killRequests) {
            Map<KillSignal, List<String>> signalToContainerIds = killRequests.stream().collect(Collectors.groupingBy(
                    KillRequest::signal, Collectors.mapping(KillRequest::containerId, Collectors.toList())));
            signalToContainerIds.forEach((signal, containerIds) -> killContainers(containerIds, signal));
        }

        @Override
        public void killContainers(final List<String> containerIds, final KillSignal signal) {
            synchronized (ExtendedShellNavigator.class) {
                if (containerIds.getFirst().startsWith(CONTAINERD_ID_PREFIX)) {
                    killContainersUsingCrictl(
                            containerIds.stream().map(id -> id.replace(CONTAINERD_ID_PREFIX, ""))
                                    .collect(Collectors.toList()),
                            signal);
                } else if (containerIds.getFirst().startsWith(DOCKERD_ID_PREFIX)) {
                    killContainersUsingDocker(
                            containerIds.stream().map(id -> id.replace(DOCKERD_ID_PREFIX, "")).collect(Collectors.toList()),
                            signal);
                } else {
                    throw new TestException(UNKNOWN_CONTAINER_ID_PREFIX_S, containerIds.getFirst());
                }
            }
        }

        @Override
        public Duration getContainerRestartTimeout() {
            return Duration.ofSeconds(120);
        }

        protected void killContainersUsingCrictl(final List<String> containerIds, final KillSignal signal) {
            final List<String> processIds = getPidsForContainerUsingCrictl(containerIds);
            final String killCmd = String.format("sudo kill -%d %s", signal.getValue(), String.join(" ", processIds));
            execute(killCmd);
        }

        protected void killContainersUsingDocker(final List<String> containerIds, final KillSignal signal) {
            final String killCmd = String.format("sudo docker kill --signal=%s %s", signal, String.join(" ", containerIds));
            execute(killCmd);
        }

        private List<String> getPidsForContainerUsingCrictl(final List<String> containerIds) {
            return containerIds.stream()
                    .map(id -> "12345")
                    .collect(Collectors.toList());
        }

        private CommandResult execute(final String command) {
            return () -> true;
        }
    }
}
