package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public final class ControlPlaneOperationsDocumentation {

    private ControlPlaneOperationsDocumentation() {}

    public enum ControlPlaneAction {
        API_SERVER_RESTARTED {
            @Override
            public void execute(final KubectlApi kubectlApi, final K8sNodeApi k8sNodeApi) {
                k8sNodeApi.restartComponent("kube-apiserver");
            }
        },
        ETCD_RESTARTED {
            @Override
            public void execute(final KubectlApi kubectlApi, final K8sNodeApi k8sNodeApi) {
                k8sNodeApi.restartComponent("etcd");
            }
        },
        SCHEDULER_RESTARTED {
            @Override
            public void execute(final KubectlApi kubectlApi, final K8sNodeApi k8sNodeApi) {
                k8sNodeApi.restartComponent("kube-scheduler");
            }
        },
        CONTROLLER_MANAGER_RESTARTED {
            @Override
            public void execute(final KubectlApi kubectlApi, final K8sNodeApi k8sNodeApi) {
                k8sNodeApi.restartComponent("kube-controller-manager");
            }
        },
        API_SERVER_STOPPED {
            @Override
            public void execute(final KubectlApi kubectlApi, final K8sNodeApi k8sNodeApi) {
                k8sNodeApi.stopComponent("kube-apiserver");
            }
        },
        ETCD_STOPPED {
            @Override
            public void execute(final KubectlApi kubectlApi, final K8sNodeApi k8sNodeApi) {
                k8sNodeApi.stopComponent("etcd");
            }
        },
        RBAC_MISCONFIGURED {
            @Override
            public void execute(final KubectlApi kubectlApi, final K8sNodeApi k8sNodeApi,
                    final String roleName, final String namespace) {
                kubectlApi.applyMisconfiguredRBAC(roleName, namespace);
            }
        },
        ADMISSION_BLOCKED {
            @Override
            public void execute(final KubectlApi kubectlApi, final K8sNodeApi k8sNodeApi,
                    final String webhookName) {
                k8sNodeApi.blockAdmissionWebhook(webhookName);
            }
        };

        public abstract void execute(KubectlApi kubectlApi, K8sNodeApi k8sNodeApi);
        public abstract void execute(KubectlApi kubectlApi, K8sNodeApi k8sNodeApi,
                String param1, String param2);
    }

    public static final class ControlPlaneSteps {
        @TestStep
        @When("API server is restarted")
        public void restartAPIServer() {
            ControlPlaneAction.API_SERVER_RESTARTED.execute(kubectlApi, k8sNodeApi);
        }

        @TestStep
        @When("etcd is restarted")
        public void restartEtcd() {
            ControlPlaneAction.ETCD_RESTARTED.execute(kubectlApi, k8sNodeApi);
        }

        @TestStep
        @When("scheduler is restarted")
        public void restartScheduler() {
            ControlPlaneAction.SCHEDULER_RESTARTED.execute(kubectlApi, k8sNodeApi);
        }

        @TestStep
        @When("controller-manager is restarted")
        public void restartControllerManager() {
            ControlPlaneAction.CONTROLLER_MANAGER_RESTARTED.execute(kubectlApi, k8sNodeApi);
        }

        @TestStep
        @When("API server is stopped")
        public void stopAPIServer() {
            ControlPlaneAction.API_SERVER_STOPPED.execute(kubectlApi, k8sNodeApi);
        }

        @TestStep
        @When("RBAC is misconfigured with role {string} in namespace {string}")
        public void misconfigureRBAC(final String roleName, final String namespace) {
            ControlPlaneAction.RBAC_MISCONFIGURED.execute(kubectlApi, k8sNodeApi, roleName, namespace);
        }

        @TestStep
        @When("admission webhook {string} is blocked")
        public void blockAdmissionWebhook(final String webhookName) {
            ControlPlaneAction.ADMISSION_BLOCKED.execute(kubectlApi, k8sNodeApi, webhookName);
        }

        @TestStep
        @Then("API server is healthy")
        public void verifyAPIServerHealthy() {
            JcatAssertApi.assertTrue(
                    "API server is not healthy",
                    kubectlApi.isAPIServerHealthy());
        }

        @TestStep
        @Then("etcd health is OK")
        public void verifyEtcdHealthy() {
            JcatAssertApi.assertTrue(
                    "etcd is not healthy",
                    kubectlApi.isEtcdHealthy());
        }

        @TestStep
        @Then("control plane components are running")
        public void verifyControlPlaneRunning() {
            final List<String> components = List.of("kube-apiserver", "etcd",
                    "kube-scheduler", "kube-controller-manager");
            for (final String component : components) {
                JcatAssertApi.assertTrue(
                        String.format("Control plane component '%s' is not running", component),
                        k8sNodeApi.isComponentRunning(component));
            }
        }

        private KubectlApi kubectlApi = new KubectlApi();
        private K8sNodeApi k8sNodeApi = new K8sNodeApi();
    }

    public interface KubectlApi {
        void applyMisconfiguredRBAC(String roleName, String namespace);
        boolean isAPIServerHealthy();
        boolean isEtcdHealthy();
        String getNamespace();
    }

    public interface K8sNodeApi {
        void restartComponent(String component);
        void stopComponent(String component);
        void blockAdmissionWebhook(String webhookName);
        boolean isComponentRunning(String component);
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
        public TestException(String message, Object... args) { super(String.format(message, args)); }
    }
}
