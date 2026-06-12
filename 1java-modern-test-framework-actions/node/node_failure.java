package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Node failure action patterns extracted from the PCC and Beets test frameworks.
 * This file is self-contained for documentation and reference purposes.
 */
public final class NodeFailureActionsDocumentation {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration CONNECT_INTERVAL = Duration.ofMillis(500);
    private static final String KERNEL_PANIC_CMD = "echo c > /proc/sysrq-trigger";
    private static final long KERNEL_PANIC_TIMEOUT_SECONDS = 30L;
    private static final String POWER_ON_OR_OFF_BAREMETAL_NODE_COMMAND = "kubectl patch bmh %s -n %s -p '{\"metadata\":{\"annotations\":{\"%s\":\"%s\"}}}'";
    private static final String POWER_OFF_ANNOTATION = "inspect.metal3.io/hardwaredetails";

    private NodeFailureActionsDocumentation() {
    }

    public enum NodeAction {
        STARTED,
        RESTARTED,
        FORCE_RESTARTED,
        SHUT_DOWN,
        KERNEL_PANICKED,
        KERNEL_PANICKED_WITHOUT_RECONNECT,
        DRAINED,
        CORDONED,
        UNCORDONED,
        DESTROYED;

        public void execute(final K8sNodeApi k8sNodeApi, final KubectlApi kubectlApi,
                final KubectlProperties kubectlProperties, final String nodeName) {
            throw new TestException("Base node action '%s' not implemented", this);
        }
    }

    public interface K8sNodeApi {
        String getHostname();

        void performAction(NodeAction nodeAction, String... flags);
    }

    public static class K8sNodeBase implements K8sNodeApi {
        protected void triggerKernelPanic(String command, long timeoutS) {
            long timeoutTimeMs = triggerKernelPanicWithoutReconnect(command, timeoutS);
            reconnectAfterKernelPanic(timeoutTimeMs);
        }

        protected long triggerKernelPanicWithoutReconnect(String command, long timeoutS) {
            connect(Duration.ofSeconds(CONNECT_TIMEOUT.getSeconds()));
            long timeoutTimeMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutS);
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

        protected void triggerKernelPanicWithoutVerification(String command, long timeoutS) {
            connect(Duration.ofSeconds(CONNECT_TIMEOUT.getSeconds()));
            navigator.getExtendedCli().sendAsync(command);
            Delay.sleep(Duration.ofSeconds(1));
        }

        protected void reconnectAfterKernelPanic(long timeoutTimeMs) {
            long remainingMs = timeoutTimeMs - System.currentTimeMillis();
            if (remainingMs > 0) {
                Delay.sleep(Duration.ofMillis(remainingMs));
            }
            connect(Duration.ofSeconds(CONNECT_TIMEOUT.getSeconds()));
        }

        protected void connect(Duration timeout) {
        }

        protected boolean isConnected() {
            return true;
        }

        @Override
        public String getHostname() {
            return "node-1";
        }

        @Override
        public void performAction(NodeAction nodeAction, String... flags) {
            switch (nodeAction) {
                case KERNEL_PANICKED:
                    triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT_SECONDS);
                    break;
                case KERNEL_PANICKED_WITHOUT_RECONNECT:
                    triggerKernelPanicWithoutVerification(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT_SECONDS);
                    break;
                default:
                    throw new TestException("Base node does not support node action '%s'", nodeAction);
            }
        }

        protected Navigator navigator = new Navigator();
        protected Logger LOGGER = new Logger();
        protected Delay Delay = new Delay();
        protected Poll Poll = new Poll();
    }

    public static class K8sBaremetalNode extends K8sNodeBase {
        private final boolean forceIpmi;
        private final String ipAddress;
        private final String name;
        private final String namespace;

        public K8sBaremetalNode(boolean forceIpmi, String ipAddress, String name, String namespace) {
            this.forceIpmi = forceIpmi;
            this.ipAddress = ipAddress;
            this.name = name;
            this.namespace = namespace;
        }

        @Override
        public void performAction(final NodeAction nodeAction, final String... flags) {
            final boolean useIpmi = forceIpmi || namespace.isEmpty();

            switch (nodeAction) {
                case STARTED:
                    performStart(useIpmi, name, namespace);
                    break;
                case RESTARTED:
                    performRestart(useIpmi, name, namespace);
                    break;
                case FORCE_RESTARTED:
                    ipmitool.restartNodeWithForce(ipAddress);
                    break;
                case SHUT_DOWN:
                    performShutDown(useIpmi, name, namespace);
                    break;
                case KERNEL_PANICKED:
                    performKernelPanic();
                    break;
                case KERNEL_PANICKED_WITHOUT_RECONNECT:
                    super.triggerKernelPanicWithoutVerification(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT_SECONDS);
                    break;
                case DRAINED:
                    kubectlApi.drainNode(getHostname(), flags);
                    break;
                case UNCORDONED:
                    kubectlApi.uncordonNode(getHostname(), flags);
                    break;
                case CORDONED:
                    kubectlApi.cordonNode(getHostname());
                    break;
                default:
                    throw new TestException("Baremetal nodes does not support node action '%s'", nodeAction);
            }
        }

        private void performShutDown(final boolean useIpmi, final String name, final String namespace) {
            if (useIpmi) {
                ipmitool.shutdownNode(ipAddress);
            } else {
                kubectlApi.kubectl(String.format(POWER_ON_OR_OFF_BAREMETAL_NODE_COMMAND, name, namespace, POWER_OFF_ANNOTATION, "off"));
            }
        }

        private void performStart(final boolean useIpmi, final String name, final String namespace) {
            if (useIpmi) {
                ipmitool.startNode(ipAddress);
            } else {
                kubectlApi.kubectl(String.format(POWER_ON_OR_OFF_BAREMETAL_NODE_COMMAND, name, namespace, POWER_OFF_ANNOTATION, "on"));
            }
        }

        private void performRestart(final boolean useIpmi, final String name, final String namespace) {
            if (useIpmi) {
                ipmitool.restartNode(ipAddress);
            } else {
                performShutDown(useIpmi, name, namespace);
                performStart(useIpmi, name, namespace);
            }
        }

        private void performKernelPanic() {
            super.triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT_SECONDS);
        }

        private Ipmitool ipmitool = new Ipmitool();
        private KubectlApi kubectlApi = new KubectlApi();
    }

    public static class K8sMesosNode extends K8sNodeBase {
        @Override
        public void performAction(final NodeAction nodeAction, final String... flags) {
            switch (nodeAction) {
                case RESTARTED:
                    computeHost.rebootNode(getHostname());
                    break;
                case FORCE_RESTARTED:
                    computeHost.rebootNodeWithForce(getHostname());
                    break;
                case STARTED:
                    computeHost.startNode(getHostname());
                    break;
                case SHUT_DOWN:
                    computeHost.shutdownNode(getHostname());
                    break;
                case DESTROYED:
                    computeHost.destroyNode(getHostname());
                    break;
                case KERNEL_PANICKED:
                    super.triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT_SECONDS);
                    break;
                case DRAINED:
                    kubectlApi.drainNode(getHostname(), flags);
                    break;
                case UNCORDONED:
                    kubectlApi.uncordonNode(getHostname(), flags);
                    break;
                case CORDONED:
                    kubectlApi.cordonNode(getHostname());
                    break;
                default:
                    throw new TestException("Mesos nodes does not support node action '%s'", nodeAction);
            }
        }

        private ComputeHost computeHost = new ComputeHost();
        private KubectlApi kubectlApi = new KubectlApi();
    }

    public static class NexusNode extends K8sNodeBase {
        @Override
        public void performAction(final NodeAction nodeAction, final String... flags) {
            switch (nodeAction) {
                case DRAINED:
                    kubectlApi.drainNode(getHostname(), flags);
                    break;
                case UNCORDONED:
                    kubectlApi.uncordonNode(getHostname(), flags);
                    break;
                case CORDONED:
                    kubectlApi.cordonNode(getHostname());
                    break;
                case KERNEL_PANICKED:
                    triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT_SECONDS);
                    break;
                case KERNEL_PANICKED_WITHOUT_RECONNECT:
                    triggerKernelPanicWithoutVerification(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT_SECONDS);
                    break;
                case RESTARTED:
                case FORCE_RESTARTED:
                    azureHandler.restartNode(getHostname());
                    break;
                case DESTROYED:
                case STARTED:
                case SHUT_DOWN:
                default:
                    throw new TestException("Nexus nodes does not support node action '%s'", nodeAction);
            }
        }

        private AzureHandler azureHandler = new AzureHandler();
        private KubectlApi kubectlApi = new KubectlApi();
    }

    public static class GdceNode extends K8sNodeBase {
        @Override
        public void performAction(final NodeAction nodeAction, final String... flags) {
            switch (nodeAction) {
                case RESTARTED:
                    Request.NodeRebootRequest req = Request.newBuilder().getNodeRebootRequestBuilder()
                            .setGraceful(true).setMachineName(getMachineName()).build();
                    pubSubHandler.sendAndReceiveMessage(Request.newBuilder().setNodeRebootRequest(req).build());
                    break;
                case FORCE_RESTARTED:
                    Request.NodeRebootRequest req2 = Request.newBuilder().getNodeRebootRequestBuilder()
                            .setGraceful(false).setMachineName(getMachineName()).build();
                    pubSubHandler.sendAndReceiveMessage(Request.newBuilder().setNodeRebootRequest(req2).build());
                    break;
                case DRAINED:
                    kubectlApi.drainNode(getHostname(), flags);
                    break;
                case UNCORDONED:
                    kubectlApi.uncordonNode(getHostname(), flags);
                    break;
                case CORDONED:
                    kubectlApi.cordonNode(getHostname());
                    break;
                default:
                    throw new TestException("GDCE nodes does not support node action '%s'", nodeAction);
            }
        }

        private String getMachineName() {
            return getHostname();
        }

        private PubSubHandler pubSubHandler = new PubSubHandler();
        private KubectlApi kubectlApi = new KubectlApi();
    }

    public static class K8sCeeNode extends K8sNodeBase {
        @Override
        public void performAction(final NodeAction nodeAction, final String... flags) {
            switch (nodeAction) {
                case RESTARTED:
                    openstackHandler.rebootServer(getHostname());
                    break;
                case FORCE_RESTARTED:
                    openstackHandler.rebootServerWithForce(getHostname());
                    break;
                case STARTED:
                    openstackHandler.startServer(getHostname());
                    break;
                case SHUT_DOWN:
                    openstackHandler.shutdownServer(getHostname());
                    break;
                case KERNEL_PANICKED:
                    super.triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT_SECONDS);
                    break;
                case DRAINED:
                    kubectlApi.drainNode(getHostname(), flags);
                    break;
                case UNCORDONED:
                    kubectlApi.uncordonNode(getHostname(), flags);
                    break;
                case CORDONED:
                    kubectlApi.cordonNode(getHostname());
                    break;
                default:
                    throw new TestException("CEE nodes does not support node action '%s'", nodeAction);
            }
        }

        private OpenstackHandler openstackHandler = new OpenstackHandler();
        private KubectlApi kubectlApi = new KubectlApi();
    }

    public static class K8sLpgNode extends K8sNodeBase {
        private final String ipAddress;

        public K8sLpgNode(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        @Override
        public void performAction(final NodeAction nodeAction, final String... flags) {
            switch (nodeAction) {
                case STARTED:
                    ipmitool.startNode(ipAddress);
                    break;
                case RESTARTED:
                    ipmitool.restartNode(ipAddress);
                    break;
                case FORCE_RESTARTED:
                    ipmitool.restartNodeWithForce(ipAddress);
                    break;
                case SHUT_DOWN:
                    ipmitool.shutdownNode(ipAddress);
                    break;
                case KERNEL_PANICKED:
                    super.triggerKernelPanic(KERNEL_PANIC_CMD, KERNEL_PANIC_TIMEOUT_SECONDS);
                    break;
                default:
                    throw new TestException("LPG nodes does not support node action '%s'", nodeAction);
            }
        }

        private Ipmitool ipmitool = new Ipmitool();
    }

    public interface Navigator {
        ExtendedCli getExtendedCli();
    }

    public interface ExtendedCli {
        void addInnerDecorator(Object decorator);

        void sendAsync(String command);

        void removeInnerDecorator();
    }

    public interface Decorators {
        static Object isConnectedWithTimeout(Object millis) {
            return new Object();
        }
    }

    public interface Millis {
        static Object fromSeconds(long seconds) {
            return new Object();
        }
    }

    public interface Delay {
        static void sleep(Duration duration) {
        }
    }

    public interface Poll {
        /**
         * Poll until condition succeeds or timeout expires.
         * Implementation: loop while checking condition at intervals until timeout.
         */
        static boolean isActionCompleted(Runnable condition, Duration timeout, Duration interval) {
            return true;
        }
    }

    public interface Logger {
        default void info(String message, Object... args) {
        }
    }

    public interface Ipmitool {
        default void startNode(String ipAddress) {
        }

        default void restartNode(String ipAddress) {
        }

        default void restartNodeWithForce(String ipAddress) {
        }

        default void shutdownNode(String ipAddress) {
        }
    }

    public interface KubectlApi {
        default void drainNode(String hostname, String... flags) {
        }

        default void uncordonNode(String hostname, String... flags) {
        }

        default void cordonNode(String hostname) {
        }

        default void kubectl(String command) {
        }
    }

    public interface ComputeHost {
        default void rebootNode(String hostname) {
        }

        default void rebootNodeWithForce(String hostname) {
        }

        default void startNode(String hostname) {
        }

        default void shutdownNode(String hostname) {
        }

        default void destroyNode(String hostname) {
        }
    }

    public interface AzureHandler {
        default void restartNode(String hostname) {
        }
    }

    public interface PubSubHandler {
        default void sendAndReceiveMessage(Object message) {
        }
    }

    public interface OpenstackHandler {
        default void rebootServer(String hostname) {
        }

        default void rebootServerWithForce(String hostname) {
        }

        default void startServer(String hostname) {
        }

        default void shutdownServer(String hostname) {
        }
    }

    public static class Request {
        public static NodeRebootRequestBuilder newBuilder() {
            return new NodeRebootRequestBuilder();
        }

        public static class NodeRebootRequestBuilder {
            private boolean graceful;
            private String machineName;

            public NodeRebootRequestBuilder setGraceful(boolean graceful) {
                this.graceful = graceful;
                return this;
            }

            public NodeRebootRequestBuilder setMachineName(String machineName) {
                this.machineName = machineName;
                return this;
            }

            public NodeRebootRequest build() {
                return new NodeRebootRequest(graceful, machineName);
            }
        }

        public static class NodeRebootRequest {
            private final boolean graceful;
            private final String machineName;

            public NodeRebootRequest(boolean graceful, String machineName) {
                this.graceful = graceful;
                this.machineName = machineName;
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
}
