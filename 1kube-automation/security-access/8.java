package com.ericsson.pcc.testcases.helpers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class SecurityAccessFailuresDocumentation {

    private SecurityAccessFailuresDocumentation() {}

    public enum SecurityAction {
        UNAUTHORIZED_API_ACCESS {
            @Override
            public void execute(final KubectlApi kubectlApi, final String user,
                    final String resource, final String verb) {
                final String result = kubectlApi.authCanI(user, resource, verb);
                JcatAssertApi.assertTrue(
                        String.format("User '%s' should NOT have '%s' access to '%s'", user, verb, resource),
                        !"yes".equalsIgnoreCase(result));
            }
        },
        RBAC_MISCONFIGURED {
            @Override
            public void execute(final KubectlApi kubectlApi, final String roleFile) {
                kubectlApi.applyRBAC(roleFile);
            }
        },
        SECRET_MISSING {
            @Override
            public void execute(final KubectlApi kubectlApi, final String secretName,
                    final String namespace) {
                final boolean exists = kubectlApi.secretExists(secretName, namespace);
                JcatAssertApi.assertTrue(
                        String.format("Secret '%s' should not exist in namespace '%s'",
                                secretName, namespace),
                        !exists);
            }
        },
        POD_RUNNING_AS_ROOT {
            @Override
            public void execute(final KubectlApi kubectlApi, final String podName) {
                final Integer runAsUser = kubectlApi.getPodRunAsUser(podName);
                JcatAssertApi.assertTrue(
                        String.format("Pod '%s' should not run as root (runAsUser=%s)", podName, runAsUser),
                        runAsUser != null && runAsUser != 0);
            }
        },
        NETWORK_POLICY_BLOCKING {
            @Override
            public void execute(final KubectlApi kubectlApi, final String policyFile,
                    final String namespace) {
                kubectlApi.applyNetworkPolicy(policyFile, namespace);
            }
        },
        SERVICE_ACCOUNT_TOKEN_FAILURE {
            @Override
            public void execute(final KubectlApi kubectlApi, final String serviceAccount,
                    final String namespace) {
                final boolean valid = kubectlApi.validateServiceAccountToken(serviceAccount, namespace);
                JcatAssertApi.assertTrue(
                        String.format("Service account token for '%s' should be invalid", serviceAccount),
                        !valid);
            }
        };

        public abstract void execute(KubectlApi kubectlApi, String target);
        public abstract void execute(KubectlApi kubectlApi, String target, String namespace);
        public abstract void execute(KubectlApi kubectlApi, String user, String resource, String verb);
    }

    public static final class SecuritySteps {
        @TestStep
        @Then("user {string} does not have {string} access to {string}")
        public void verifyUnauthorizedAccess(final String user, final String verb, final String resource) {
            SecurityAction.UNAUTHORIZED_API_ACCESS.execute(kubectlApi, user, resource, verb);
        }

        @TestStep
        @When("RBAC is misconfigured with role from file {string}")
        public void misconfigureRBAC(final String roleFile) {
            SecurityAction.RBAC_MISCONFIGURED.execute(kubectlApi, roleFile);
        }

        @TestStep
        @Then("secret {string} is missing in namespace {string}")
        public void verifySecretMissing(final String secretName, final String namespace) {
            SecurityAction.SECRET_MISSING.execute(kubectlApi, secretName, namespace);
        }

        @TestStep
        @Then("pod {string} is not running as root")
        public void verifyPodNotRoot(final String podName) {
            SecurityAction.POD_RUNNING_AS_ROOT.execute(kubectlApi, podName);
        }

        @TestStep
        @When("network policy from file {string} is applied in namespace {string}")
        public void applyNetworkPolicy(final String policyFile, final String namespace) {
            SecurityAction.NETWORK_POLICY_BLOCKING.execute(kubectlApi, policyFile, namespace);
        }

        @TestStep
        @Then("service account {string} token is invalid in namespace {string}")
        public void verifyServiceAccountTokenInvalid(final String sa, final String namespace) {
            SecurityAction.SERVICE_ACCOUNT_TOKEN_FAILURE.execute(kubectlApi, sa, namespace);
        }

        private KubectlApi kubectlApi = new KubectlApi();
    }

    public interface KubectlApi {
        String authCanI(String user, String resource, String verb);
        void applyRBAC(String roleFile);
        boolean secretExists(String secretName, String namespace);
        Integer getPodRunAsUser(String podName);
        void applyNetworkPolicy(String policyFile, String namespace);
        boolean validateServiceAccountToken(String serviceAccount, String namespace);
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
