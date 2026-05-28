package com.example.testfw.driver;

import io.fabric8.helm.client.HelmClient;
import io.fabric8.helm.client.HelmClientBuilder;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import com.example.testfw.config.TestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Modern TRex driver using Kubernetes client instead of SSH/shell execution.
 * Manages TRex traffic generator lifecycle in Kubernetes cluster.
 */
public class TRexDriver {
    private static final Logger LOG = LoggerFactory.getLogger(TRexDriver.class);
    
    private final TestConfig config;
    private KubernetesClient kubernetesClient;
    private String trexDeploymentName = "trex-generator";
    private String trexServiceName = "trex-service";
    private String trexConfigMapName = "trex-config";

    public TRexDriver(TestConfig config) {
        this.config = config;
    }

    /**
     * Starts TRex by creating Kubernetes deployment and service.
     */
    public void start() {
        try (KubernetesClient client = createKubernetesClient()) {
            this.kubernetesClient = client;
            
            LOG.info("Starting TRex driver for namespace: {}", config.namespace());
            
            // Create ConfigMap for TRex configuration
            createTrexConfigMap();
            
            // Create TRex deployment
            createTrexDeployment();
            
            // Create service for TRex (optional, for accessing TRex API)
            createTrexService();
            
            // Wait for deployment to be ready
            waitForDeploymentReady(trexDeploymentName, 2, TimeUnit.MINUTES);
            
            LOG.info("TRex started successfully in namespace: {}", config.namespace());
        } catch (Exception e) {
            LOG.error("Failed to start TRex driver", e);
            throw new RuntimeException("Failed to start TRex driver", e);
        }
    }

    /**
     * Starts TRex by deploying a Helm chart.
     * 
     * @param chartRepository URL or OCI reference to the Helm chart
     * @param values          Values to override in the Helm chart
     */
    public void startWithHelmChart(String chartRepository, Map<String, String> values) {
        try (KubernetesClient k8sClient = createKubernetesClient();
             HelmClient helmClient = new HelmClientBuilder()
                     .withKubernetesClient(k8sClient)
                     .build()) {
            
            LOG.info("Starting TRex driver with Helm chart: {}", chartRepository);
            
            // Create namespace if it doesn't exist
            if (k8sClient.namespaces().withName(config.namespace()).get() == null) {
                k8sClient.namespaces().createNew()
                        .withNewMetadata()
                            .withName(config.namespace())
                        .endMetadata()
                        .done();
                LOG.info("Created namespace: {}", config.namespace());
            }
            
            // Deploy or upgrade the Helm release
            helmClient.chart(chartRepository)
                    .releaseName(trexDeploymentName)
                    .namespace(config.namespace())
                    .values(values)
                    .installOrUpgrade();
            
            LOG.info("TRex Helm chart installed/upgraded successfully");
            
            // Wait for deployment to be ready (assuming chart creates a deployment)
            waitForDeploymentReady(trexDeploymentName, 3, TimeUnit.MINUTES);
            
            LOG.info("TRex started successfully via Helm in namespace: {}", config.namespace());
        } catch (Exception e) {
            LOG.error("Failed to start TRex driver with Helm chart", e);
            throw new RuntimeException("Failed to start TRex driver with Helm chart", e);
        }
    }

    /**
     * Stops TRex by deleting Kubernetes resources.
     */
    public void stop() {
        if (kubernetesClient != null) {
            try {
                LOG.info("Stopping TRex driver for namespace: {}", config.namespace());
                
                // Delete deployment
                kubernetesClient.apps().deployments()
                        .inNamespace(config.namespace())
                        .withName(trexDeploymentName)
                        .delete();
                
                // Delete service
                kubernetesClient.service()
                        .inNamespace(config.namespace())
                        .withName(trexServiceName)
                        .delete();
                
                // Delete config map
                kubernetesClient.configMaps()
                        .inNamespace(config.namespace())
                        .withName(trexConfigMapName)
                        .delete();
                
                LOG.info("TRex stopped successfully for namespace: {}", config.namespace());
            } catch (Exception e) {
                LOG.error("Failed to stop TRex driver", e);
                throw new RuntimeException("Failed to stop TRex driver", e);
            } finally {
                kubernetesClient.close();
            }
        }
    }

    /**
     * Stops TRex by uninstalling the Helm release.
     */
    public void stopHelmRelease() {
        try (KubernetesClient k8sClient = createKubernetesClient();
             HelmClient helmClient = new HelmClientBuilder()
                     .withKubernetesClient(k8sClient)
                     .build()) {
            
            LOG.info("Stopping TRex driver by uninstalling Helm release: {}", trexDeploymentName);
            
            // Uninstall the Helm release
            helmClient.release(trexDeploymentName)
                    .namespace(config.namespace())
                    .uninstall();
            
            LOG.info("TRex Helm release uninstalled successfully");
        } catch (Exception e) {
            LOG.error("Failed to stop TRex driver Helm release", e);
            throw new RuntimeException("Failed to stop TRex driver Helm release", e);
        }
    }

    /**
     * Uploads configuration or script files to TRex via ConfigMap.
     * 
     * @param sourcePath Local file path to upload
     * @param targetName Name for the file in ConfigMap
     */
    public void upload(Path sourcePath, String targetName) {
        if (kubernetesClient == null) {
            throw new IllegalStateException("TRex driver not started. Call start() first.");
        }
        
        try {
            byte[] fileContent = Files.readAllBytes(sourcePath);
            String content = new String(fileContent);
            
            // Update existing ConfigMap or create new one
            ConfigMap configMap = kubernetesClient.configMaps()
                    .inNamespace(config.namespace())
                    .withName(trexConfigMapName)
                    .edit()
                    .addToData(targetName, content)
                    .done();
            
            LOG.info("Uploaded {} to ConfigMap {} as {}", sourcePath, trexConfigMapName, targetName);
        } catch (Exception e) {
            LOG.error("Failed to upload file {} to TRex", sourcePath, e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Downloads file from TRex ConfigMap to local path.
     * 
     * @param sourceName Name of file in ConfigMap
     * @param targetPath Local file path to save to
     */
    public void download(String sourceName, Path targetPath) {
        if (kubernetesClient == null) {
            throw new IllegalStateException("TRex driver not started. Call start() first.");
        }
        
        try {
            ConfigMap configMap = kubernetesClient.configMaps()
                    .inNamespace(config.namespace())
                    .withName(trexConfigMapName)
                    .get();
            
            if (configMap == null || configMap.getData() == null) {
                throw new IllegalStateException("ConfigMap not found or empty: " + trexConfigMapName);
            }
            
            String content = configMap.getData().get(sourceName);
            if (content == null) {
                throw new IllegalStateException("File not found in ConfigMap: " + sourceName);
            }
            
            Files.write(targetPath, content.getBytes());
            LOG.info("Downloaded {} from ConfigMap {} to {}", sourceName, trexConfigMapName, targetPath);
        } catch (Exception e) {
            LOG.error("Failed to download file {} from TRex", sourceName, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    /**
     * Collects logs from TRex pod.
     * 
     * @return TRex pod logs as string
     */
    public String collectLogs() {
        if (kubernetesClient == null) {
            throw new IllegalStateException("TRex driver not started. Call start() first.");
        }
        
        try {
            // Find TRex pods (label selector)
            PodList podList = kubernetesClient.pods()
                    .inNamespace(config.namespace())
                    .withLabel("app", "trex")
                    .list();
            
            if (podList.getItems().isEmpty()) {
                LOG.warn("No TRex pods found with label app=trex");
                return "No TRex pods found";
            }
            
            // Get logs from first pod (assuming single replica for simplicity)
            String podName = podList.getItems().get(0).getMetadata().getName();
            String logs = kubernetesClient.pods()
                    .inNamespace(config.namespace())
                    .withName(podName)
                    .getLog();
            
            LOG.info("Collected logs from TRex pod: {}", podName);
            return logs;
        } catch (Exception e) {
            LOG.error("Failed to collect logs from TRex", e);
            return "Error collecting logs: " + e.getMessage();
        }
    }

    /**
     * Collects metrics from TRex (if Prometheus endpoint is exposed).
     * 
     * @return Metrics in Prometheus format
     */
    public String collectMetrics() {
        if (kubernetesClient == null) {
            throw new IllegalStateException("TRex driver not started. Call start() first.");
        }
        
        try {
            // Try to get metrics from TRex service
            // This assumes TRex exposes a Prometrics endpoint on port 9090
            return kubernetesClient.pods()
                    .inNamespace(config.namespace())
                    .withLabel("app", "trex")
                    .list()
                    .getItems()
                    .stream()
                    .findFirst()
                    .map(pod -> {
                        try {
                            return kubernetesClient.pods()
                                    .inNamespace(config.namespace())
                                    .withName(pod.getMetadata().getName())
                                    .getPortForward(9090)
                                    .getInputStream()
                                    .readAllBytes()
                                    .toString();
                        } catch (Exception e) {
                            LOG.warn("Could not get metrics from TRex pod: {}", e.getMessage());
                            return "# Metrics unavailable\n";
                        }
                    })
                    .orElse("# No TRex pods found\n");
        } catch (Exception e) {
            LOG.error("Failed to collect metrics from TRex", e);
            return "# Error collecting metrics\n";
        }
    }

    /**
     * Checks if TRex is running and ready.
     * 
     * @return true if TRex deployment is ready
     */
    public boolean isRunning() {
        if (kubernetesClient == null) {
            return false;
        }
        
        try {
            Deployment deployment = kubernetesClient.apps().deployments()
                    .inNamespace(config.namespace())
                    .withName(trexDeploymentName)
                    .get();
            
            if (deployment == null) {
                return false;
            }
            
            int desiredReplicas = deployment.getSpec().getReplicas();
            int readyReplicas = deployment.getStatus().getReadyReplicas() != null ? 
                    deployment.getStatus().getReadyReplicas() : 0;
            
            return readyReplicas >= desiredReplicas;
        } catch (Exception e) {
            LOG.error("Error checking TRex status", e);
            return false;
        }
    }

    private KubernetesClient createKubernetesClient() {
        Config config;
        if (this.config.kubeconfig() != null && Files.exists(this.config.kubeconfig())) {
            config = new ConfigBuilder()
                    .withKubeconfig(this.config.kubeconfig().toAbsolutePath().toString())
                    .build();
        } else {
            // Try to load from default locations
            config = new ConfigBuilder().build();
        }
        
        return new DefaultKubernetesClient(config);
    }

    private void createTrexConfigMap() {
        Map<String, String> data = new HashMap<>();
        data.put("trex-config.lua", "-- TRex configuration placeholder\n");
        
        kubernetesClient.configMaps()
                .inNamespace(config.namespace())
                .createOrReplace(new ConfigBuilder()
                        .withNewMetadata()
                            .withName(trexConfigMapName)
                            .addToLabels("app", "trex")
                        .endMetadata()
                        .withData(data)
                        .build());
        
        LOG.info("Created/updated ConfigMap: {}", trexConfigMapName);
    }

    private void createTrexDeployment() {
        // Using a simple TRex image - in practice, you'd use your actual TRex image
        Container container = new ContainerBuilder()
                .withName("trex")
                .withImage("trexstat/trex:latest")  // Example image
                .withCommand("./trex-control-plane", "-i", "0.0.0.0")
                .withPorts(new ContainerPort(4500))  // TRex control plane port
                .addNewVolumeMount()
                    .withName("trex-config")
                    .withMountPath("/etc/trex")
                    .endVolumeMount()
                .build();

        Volume volume = new VolumeBuilder()
                .withName("trex-config")
                .withNewConfigMap()
                    .withName(trexConfigMapName)
                .endConfigMap()
                .build();

        PodTemplateSpec template = new PodTemplateSpecBuilder()
                .withNewMetadata()
                    .addToLabels("app", "trex")
                    .addToLabels("app.kubernetes.io/name", "trex")
                .endMetadata()
                .withNewSpec()
                    .withVolumes(volume)
                    .withContainers(container)
                    .endSpec()
                .build();

        Deployment deployment = new DeploymentBuilder()
                .withNewMetadata()
                    .withName(trexDeploymentName)
                    .addToLabels("app", "trex")
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withNewSelector()
                        .addToMatchLabels("app", "trex")
                    .endSelector()
                    .withTemplate(template)
                .endSpec()
                .build();

        kubernetesClient.apps().deployments()
                .inNamespace(config.namespace())
                .createOrReplace(deployment);
        
        LOG.info("Created/updated Deployment: {}", trexDeploymentName);
    }

    private void createTrexService() {
        Service service = new ServiceBuilder()
                .withNewMetadata()
                    .withName(trexServiceName)
                    .addToLabels("app", "trex")
                .endMetadata()
                .withNewSpec()
                    .addToPorts(new ServicePortBuilder()
                            .withPort(4500)
                            .withTargetPort(new IntOrString(4500))
                            .build())
                    .addToSelector("app", "trex")
                    .withType("ClusterIP")
                .endSpec()
                .build();

        kubernetesClient.service()
                .inNamespace(config.namespace())
                .createOrReplace(service);
        
        LOG.info("Created/updated Service: {}", trexServiceName);
    }

    private void waitForDeploymentReady(String deploymentName, long timeout, TimeUnit unit) {
        long start = System.currentTimeMillis();
        long timeoutMillis = unit.toMillis(timeout);
        
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (isRunning()) {
                return;
            }
            try {
                Thread.sleep(5000); // Check every 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for TRex deployment", e);
            }
        }
        
        throw new RuntimeException("TRex deployment did not become ready within " + timeout + " " + unit);
    }
}