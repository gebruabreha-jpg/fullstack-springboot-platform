package io.github.chaosframework.helm;

import io.github.chaosframework.api.exception.HelmException;
import io.github.chaosframework.api.model.HelmRelease;
import io.github.chaosframework.config.ChaosConfig;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class HelmClient {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HelmClient.class);
    private final ChaosConfig config;

    public HelmClient(ChaosConfig config) {
        this.config = config;
    }

    public void install(String releaseName, String chartName, String valuesPath, String namespace) {
        executeHelm("install " + releaseName + " " + chartName + " -f " + valuesPath + " -n " + namespace + " --create-namespace");
    }

    public void upgrade(String releaseName, String chartPath, String namespace) {
        executeHelm("upgrade " + releaseName + " " + chartPath + " -n " + namespace);
    }

    public void rollback(String releaseName, String namespace) {
        executeHelm("rollback " + releaseName + " -n " + namespace);
    }

    public void uninstall(String releaseName, String namespace) {
        executeHelm("uninstall " + releaseName + " -n " + namespace);
    }

    public Optional<HelmRelease> getRelease(String releaseName, String namespace) {
        try {
            List<HelmRelease> releases = listReleases(namespace);
            return releases.stream()
                    .filter(r -> r.name().equals(releaseName))
                    .findFirst();
        } catch (Exception e) {
            log.warn("Helm release {} not found in namespace {}: {}", releaseName, namespace, e.getMessage());
            return Optional.empty();
        }
    }

    public List<HelmRelease> listReleases(String namespace) {
        List<HelmRelease> releases = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("helm", "list", "-n", namespace, "-o", "json")
                    .redirectErrorStream(true)
                    .start();
            String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines()
                    .collect(java.util.stream.Collectors.joining("\n"));
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new HelmException("helm list failed with exit code " + exitCode);
            }
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> jsonList = mapper.readValue(output, List.class);
            for (Map<String, Object> map : jsonList) {
                releases.add(new HelmRelease(
                        (String) map.get("name"),
                        (String) map.get("namespace"),
                        (String) map.get("chart"),
                        (String) map.get("app_version"),
                        ((Integer) map.get("revision")).toString(),
                        (String) map.get("status"),
                        0, 0
                ));
            }
        } catch (Exception e) {
            throw new HelmException("Failed to list Helm releases: " + e.getMessage(), e);
        }
        return releases;
    }

    public void upgradeWithRestart(String releaseName, String chartPath, String namespace) {
        upgrade(releaseName, chartPath, namespace);
        restartDeployment(releaseName, namespace);
    }

    public void rollbackToRevision(String releaseName, int revision, String namespace) {
        executeHelm("rollback " + releaseName + " " + revision + " -n " + namespace);
    }

    public boolean validateReleaseHealth(String releaseName, String namespace) {
        try {
            Process process = new ProcessBuilder("helm", "test", releaseName, "-n", namespace)
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Helm release {} passed health check", releaseName);
                return true;
            } else {
                log.warn("Helm release {} failed health check (exit code {})", releaseName, exitCode);
                return false;
            }
        } catch (Exception e) {
            throw new HelmException("Failed to validate Helm release health: " + e.getMessage(), e);
        }
    }

    private void restartDeployment(String releaseName, String namespace) {
        try {
            Process process = new ProcessBuilder("kubectl", "rollout", "restart", "deployment", "-l", "app.kubernetes.io/instance=" + releaseName, "-n", namespace)
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
            log.info("Restarted deployments for release {} in namespace {}", releaseName, namespace);
        } catch (Exception e) {
            log.warn("Failed to restart deployments for release {}: {}", releaseName, e.getMessage());
        }
    }

    private void executeHelm(String command) {
        try {
            Process process = new ProcessBuilder("helm " + command)
                    .redirectErrorStream(true)
                    .start();
            String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines()
                    .collect(java.util.stream.Collectors.joining("\n"));
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new HelmException("Helm command failed (exit code " + exitCode + "): " + output);
            }
            log.info("Helm command succeeded: helm {}", command);
        } catch (HelmException e) {
            throw e;
        } catch (Exception e) {
            throw new HelmException("Failed to execute helm command: " + e.getMessage(), e);
        }
    }
}
