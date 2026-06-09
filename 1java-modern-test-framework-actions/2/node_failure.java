protected void triggerKernelPanic(String command, long timeoutS) {
    long timeoutTimeMs = triggerKernelPanicWithoutReconnect(command, timeoutS);
    reconnectAfterKernelPanic(timeoutTimeMs);
}

protected long triggerKernelPanicWithoutReconnect(String command, long timeoutS) {
    connect(Duration.ofSeconds(CONNECT_TIMEOUT.getSeconds()));
    long timeoutTimeMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutS);
    navigator.getExtendedCli()
            .addInnerDecorator(Decorators.isConnectedWithTimeout(Millis.fromSeconds(timeoutS)));
    LOGGER.info("Triggering a kernel panic on '{}'.", getHostname());
    navigator.getExtendedCli().sendAsync(command);
    Delay.sleep(Duration.ofSeconds(1));
    Duration disconnectTimeoutS = Duration.ofMillis(timeoutTimeMs - System.currentTimeMillis());
    if (!Poll.isActionCompleted(() -> !isConnected(), disconnectTimeoutS, CONNECT_INTERVAL)) {
        throw new TestException("Failed to trigger kernel panic (didn't lose connection).", getHostname());
    }
    navigator.getExtendedCli().removeInnerDecorator();
    return timeoutTimeMs;
}


@TestStep
@Given("a random DCGW is restarted within {int} {time_unit}")
public void restartRandomDcgwWithin(int timeout, TimeUnit timeUnit) {
    String dcgwHost = DcgwHelper.getSwitchedNavigatorRandomDcgwIfAllowed(...);
    JcatLoggingApi.setTestInfo("Reloading DCGW '%s'", dcgwHost);
    try {
        DcgwHelper.reloadDcgw(dcgwNode, dcgwHost);
        Delay.sleep(Duration.of(timeout, timeUnit.toChronoUnit()));
        dcgwNode.connect();
        DcgwHelper.verifyDcgwUp(String.format("Reload failed within timeout %s %s", timeout, timeUnit),
            dcgwNode, dcgwHost);
    } finally {
        dcgwNode.restoreToPreviousNavigator();
    }
}


All Node Failure Action Java Code:

K8sBaremetalNode.java (lines 130-172):

@Override
public void performAction(final NodeAction nodeAction, final String... flags) {
    final boolean forceIpmi = ...;
    final String name = parts[parts.length - 2] + "-" + parts[parts.length - 1];
    final String namespace = forceIpmi ? "" : resolveCapiNamespace();
    final boolean useIpmi = forceIpmi || namespace.isEmpty();
    
    switch (nodeAction) {
        case STARTED: performStart(useIpmi, name, namespace); break;
        case RESTARTED: performRestart(useIpmi, name, namespace); break;
        case FORCE_RESTARTED: ipmitool.restartNodeWithForce(ipAddress); break;
        case SHUT_DOWN: performShutDown(useIpmi, name, namespace); break;
        case KERNEL_PANICKED: performKernelPanic(); break;
        case KERNEL_PANICKED_WITHOUT_RECONNECT: 
            super.triggerKernelPanicWithoutVerification(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT.getSeconds()); break;
        case DRAINED: kubectlApi.drainNode(getHostname(), flags); break;
        case DESTROYED: case UNCORDONED: kubectlApi.uncordonNode(getHostname(), flags); break;
        case CORDONED: kubectlApi.cordonNode(getHostname()); break;
        default: throw new TestException("Baremetal nodes does not support node action '%s'", nodeAction);
    }
}
private void performShutDown(final boolean useIpmi, final String name, final String namespace) {
    if (useIpmi) {
        ipmitool.shutdownNode(ipAddress);
    } else {
        kubectlApi.kubectl(String.format(POWER_ON_OR_OFF_BAREMETAL_NODE_COMMAND, name, namespace, POWER_OFF_ANNOTATION));
    }
}
K8sMesosNode.java (lines 84-126):

switch (nodeAction) {
    case RESTARTED: computeHost.rebootNode(getHostname()); break;
    case FORCE_RESTARTED: computeHost.rebootNodeWithForce(getHostname()); break;
    case STARTED: computeHost.startNode(getHostname()); break;
    case SHUT_DOWN: computeHost.shutdownNode(getHostname()); break;
    case DESTROYED: computeHost.destroyNode(getHostname()); break;
    case KERNEL_PANICKED: super.triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT.getSeconds()); break;
    case DRAINED: kubectlApi.drainNode(getHostname(), flags); break;
    case UNCORDONED: kubectlApi.uncordonNode(getHostname(), flags); break;
    case CORDONED: kubectlApi.cordonNode(getHostname()); break;
}
NexusNode.java (lines 85-116):

switch (nodeAction) {
    case DRAINED: kubectlApi.drainNode(getHostname(), flags); break;
    case UNCORDONED: kubectlApi.uncordonNode(getHostname(), flags); break;
    case CORDONED: kubectlApi.cordonNode(getHostname()); break;
    case KERNEL_PANICKED: triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT.getSeconds()); break;
    case KERNEL_PANICKED_WITHOUT_RECONNECT: triggerKernelPanicWithoutVerification(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT.getSeconds()); break;
    case RESTARTED: case FORCE_RESTARTED: 
        azureHandler.restartNode(getHostname()); break;
    case DESTROYED: case STARTED: case SHUT_DOWN: default: 
        throw new TestException("Nexus nodes does not support node action '%s'", nodeAction);
}
GdceNode.java (lines 73-113):

switch (nodeAction) {
    case RESTARTED:
        Request.NodeRebootRequest req = Request.newBuilder().getNodeRebootRequestBuilder()
            .setGraceful(true).setMachineName(getMachineName()).build();
        pubSubHandler.sendAndReceiveMessage(Request.newBuilder().setNodeRebootRequest(req).build()); break;
    case FORCE_RESTARTED:
        Request.NodeRebootRequest req2 = Request.newBuilder().getNodeRebootRequestBuilder()
            .setGraceful(false).setMachineName(getMachineName()).build();
        pubSubHandler.sendAndReceiveMessage(Request.newBuilder().setNodeRebootRequest(req2).build()); break;
    case DRAINED: kubectlApi.drainNode(getHostname(), flags); break;
    case UNCORDONED: kubectlApi.uncordonNode(getHostname(), flags); break;
    case CORDONED: kubectlApi.cordonNode(getHostname()); break;
}
K8sCeeNode.java (lines 141-192):

switch (nodeAction) {
    case RESTARTED: openstackHandler.rebootServer(getHostname()); break;
    case FORCE_RESTARTED: openstackHandler.rebootServerWithForce(getHostname()); break;
    case STARTED: openstackHandler.startServer(getHostname()); break;
    case SHUT_DOWN: openstackHandler.shutdownServer(getHostname()); break;
    case KERNEL_PANICKED: super.triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT.getSeconds()); break;
    case DRAINED: kubectlApi.drainNode(getHostname(), flags); break;
    case UNCORDONED: kubectlApi.uncordonNode(getHostname(), flags); break;
    case CORDONED: kubectlApi.cordonNode(getHostname()); break;
}
K8sLpgNode.java (lines 52-74):

switch (nodeAction) {
    case STARTED: ipmitool.startNode(ipAddress); break;
    case RESTARTED: ipmitool.restartNode(ipAddress); break;
    case FORCE_RESTARTED: ipmitool.restartNodeWithForce(ipAddress); break;
    case SHUT_DOWN: ipmitool.shutdownNode(ipAddress); break;
    case KERNEL_PANICKED: super.triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT.getSeconds()); break;
}
K8sNode.java base class (lines 875-921):

protected void triggerKernelPanic(String command, long timeoutS) {
    long timeoutTimeMs = triggerKernelPanicWithoutReconnect(command, timeoutS);
    reconnectAfterKernelPanic(timeoutTimeMs);
}
protected long triggerKernelPanicWithoutReconnect(String command, long timeoutS) {
    connect(...);
    navigator.getExtendedCli().sendAsync(command);
    // Wait for disconnection, then return timeout
}
DcgwStepDefinition.java (lines 431-450):

@Given("a random DCGW is restarted within {int} {time_unit}")
public void restartRandomDcgwWithin(int timeout, TimeUnit timeUnit) {
    String dcgwHost = DcgwHelper.getSwitchedNavigatorRandomDcgwIfAllowed(...);
    DcgwHelper.reloadDcgw(dcgwNode, dcgwHost);
    Delay.sleep(Duration.of(timeout, timeUnit.toChronoUnit()));
    dcgwNode.connect();
    DcgwHelper.verifyDcgwUp(...);
}