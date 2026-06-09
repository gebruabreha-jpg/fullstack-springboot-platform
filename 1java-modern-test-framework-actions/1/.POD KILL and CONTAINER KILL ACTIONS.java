package com.ericsson.pcc.testcases.helpers;
import com.ericsson.commonlibrary.linux.commands.kill.KillSignal;

public record KillRequest(String podName, String containerName, String containerId, KillSignal signal) {
}

RESTARTED {
    @Override
    public void execute(final K8sNodeApis k8sNodeApis, final KubectlApi kubectlApi,
            final KubectlProperties kubectlProperties, final String podName) {
        // Killing all containers in a pod will restart it.
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

        // wait until K8s notice the POD is restarted
        final Duration restartTimeoutInSec = k8sNodeApi.getContainerRestartTimeout();
        final Duration intervalInSec = Duration.ofSeconds(1);
        final boolean restartFinished = Poll.isActionCompleted(
                () -> !kubectlApi.checkResourcesUp(podName), restartTimeoutInSec, intervalInSec);
        JcatAssertApi.assertTrue(
                String.format("'%s' action not finished within '%d' seconds", RESTARTED, restartTimeoutInSec.getSeconds()),
                restartFinished);
    }
}


@Override
public void killContainers(final List<String> containerIds, final KillSignal signal,
        final TimeRegistry timeRegistry) {
    synchronized (ExtendedShellNavigator.class) {
        if (containerIds.getFirst().startsWith(CONTAINERD_ID_PREFIX)) {
            killContainersUsingCrictl(
                    containerIds.stream().map(id -> id.replace(CONTAINERD_ID_PREFIX, ""))
                            .collect(Collectors.toList()),
                    signal, timeRegistry);
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
public void killContainers(final List<KillRequest> killRequests) {
    Map<KillSignal, List<String>> signalToContainerIds = killRequests.stream().collect(Collectors.groupingBy(
            KillRequest::signal, Collectors.mapping(KillRequest::containerId, Collectors.toList())));
    signalToContainerIds.forEach((signal, containerIds) -> killContainers(containerIds, signal));
}

protected void killContainersUsingCrictl(
        final List<String> containerIds, final KillSignal signal, final TimeRegistry timeRegistry) {
    final List<String> processIds = getPidsForContainerUsingCrictl(containerIds);
    if (timeRegistry != null) {
        timeRegistry.put(Constants.KVDB_PODS_KILLED_TIMESTAMP, ZonedDateTime.now());
    }
    final String killCmd = String.format("sudo kill -%d %s", signal.getValue(), String.join(" ", processIds));
    final CommandResult result = execute(killCmd);
    if (!result.isSuccessful()) {