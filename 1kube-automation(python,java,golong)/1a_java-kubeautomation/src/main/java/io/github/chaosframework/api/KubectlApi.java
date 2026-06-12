package io.github.chaosframework.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chaosframework.api.exception.KubeApiException;
import io.github.chaosframework.api.model.PodInfo;
import io.github.chaosframework.config.ChaosConfig;
import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1APIResource;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Event;
import io.kubernetes.client.openapi.models.V1EventList;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1NodeSystemInfo;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.openapi.models.V1PersistentVolumeList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodStatus;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class KubectlApi {
    private final CoreV1Api coreApi;
    private final ChaosConfig config;
    private final ObjectMapper objectMapper;

    public KubectlApi(ChaosConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.coreApi = createCoreApi();
    }

    private CoreV1Api createCoreApi() {
        try {
            String kubeConfigPath = config.getKubeConfigPath().toString();
            ApiClient client;
            if (!Config.isValidKubeConfigFile(kubeConfigPath) || Config.defaultClient() == null) {
                client = Config.fromCluster();
            } else {
                client = Config.fromConfig(kubeConfigPath);
            }
            Configuration.setDefaultApiClient(client);
            return new CoreV1Api(client);
        } catch (IOException e) {
            throw new KubeApiException("Failed to initialize Kubernetes client: " + e.getMessage(), e);
        }
    }

    private String getNamespace() {
        return config.getNamespace();
    }

    public List<PodInfo> getPods(String namespace, String labelSelector) {
        try {
            V1PodList podList = coreApi.listNamespacedPod(
                    namespace,
                    null, null, null, null,
                    labelSelector, null, null, null, null, null
            );
            return podList.getItems().stream()
                    .map(this::toPodInfo)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            throw new KubeApiException("Failed to list pods: " + e.getResponseBody(), e);
        }
    }

    public List<PodInfo> getPodsByPrefix(String namespace, String prefix) {
        return getPods(namespace, String.format("name~=^%s", prefix));
    }

    public Optional<PodInfo> getPod(String namespace, String podName) {
        try {
            V1Pod pod = coreApi.readNamespacedPod(podName, namespace, null);
            return Optional.of(toPodInfo(pod));
        } catch (ApiException e) {
            if (e.getCode() == 404) return Optional.empty();
            throw new KubeApiException("Failed to read pod: " + e.getMessage(), e);
        }
    }

    public void killPod(String namespace, String podName, String containerName, String signal) {
        try {
            String command = String.format("pkill -%s -f %s", signal, containerName);
            coreApi.connectPostNamespacedPodExec(
                    podName, namespace, command,
                    true, true, null,
                    null, null, null,
                    List.of("/bin/sh", "-c", command),
                    null
            );
            log.info("Sent signal {} to container {} in pod {}/{}", signal, containerName, namespace, podName);
        } catch (ApiException e) {
            throw new KubeApiException("Failed to kill container: " + e.getMessage(), e);
        }
    }

    public void deletePod(String namespace, String podName) {
        try {
            coreApi.deleteNamespacedPod(podName, namespace, null, null, null, null, null, null);
            log.info("Deleted pod {}/{}", namespace, podName);
        } catch (ApiException e) {
            throw new KubeApiException("Failed to delete pod " + podName + ": " + e.getMessage(), e);
        }
    }

    public void restartPod(String namespace, String podName) {
        String fieldSelector = String.format("involvedObject.name=%s", podName);
        try {
            V1EventList events = coreApi.listEventForAllNamespaces(null, null, fieldSelector, null, null, null, null, null, null);
            for (V1Event event : events.getItems()) {
                if ("Killing".equals(event.getReason()) && podName.equals(event.getInvolvedObject().getName())) {
                    log.info("Pod {}/{} already being deleted, skipping", namespace, podName);
                    return;
                }
            }

            V1Pod pod = coreApi.readNamespacedPod(podName, namespace, null);
            Map<String, String> labels = Optional.ofNullable(pod.getMetadata().getLabels()).orElse(Map.of());
            Map<String, String> annotations = Optional.ofNullable(pod.getMetadata().getAnnotations()).orElse(Map.of());

            coreApi.deleteNamespacedPod(podName, namespace, null, null, null, null, null, null);
            log.info("Deleted pod {}/{} for restart", namespace, podName);

            int timeout = config.getInt("experiment.timeout", 300);
            Instant deadline = Instant.now().plusSeconds(timeout);
            while (Instant.now().isBefore(deadline)) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new KubeApiException("Interrupted while waiting for pod restart", e);
                }
                Optional<PodInfo> existing = getPod(namespace, podName);
                if (existing.isPresent()) {
                    log.info("Pod {}/{} recreated after restart", namespace, podName);
                    return;
                }
            }
            throw new KubeApiException("Pod " + podName + " did not restart within timeout");
        } catch (ApiException e) {
            throw new KubeApiException("Failed to restart pod: " + e.getMessage(), e);
        }
    }

    public List<String> listNamespaces(String prefix) {
        try {
            V1NamespaceList list = coreApi.listNamespace(null, null, null, null, null, null, null, null, null);
            return list.getItems().stream()
                    .map(n -> n.getMetadata().getName())
                    .filter(name -> prefix == null || name.startsWith(prefix))
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            throw new KubeApiException("Failed to list namespaces: " + e.getMessage(), e);
        }
    }

    public List<V1Event> getEvents(String namespace) {
        try {
            int timeoutSeconds = config.getInt("experiment.timeout", 300);
            V1EventList eventList = coreApi.listNamespacedEvent(
                    namespace, null, null, null, null, null, null,
                    Integer.toString(Math.max(timeoutSeconds * 10, 100)), null
            );
            return eventList.getItems();
        } catch (ApiException e) {
            throw new KubeApiException("Failed to fetch events: " + e.getMessage(), e);
        }
    }

    public List<V1Event> getEventsByInvolvedObject(String namespace, String kind, String name) {
        return getEvents(namespace).stream()
                .filter(e -> kind.equalsIgnoreCase(e.getInvolvedObject().getKind()))
                .filter(e -> name.equalsIgnoreCase(e.getInvolvedObject().getName()))
                .collect(Collectors.toList());
    }

    public List<NodeInfo> getNodes() {
        try {
            V1NodeList nodeList = coreApi.listNode(null, null, null, null, null, null, null, null, null);
            return nodeList.getItems().stream()
                    .map(this::toNodeInfo)
                    .collect(Collectors.toList());
        } catch (ApiException e) {
            throw new KubeApiException("Failed to list nodes: " + e.getMessage(), e);
        }
    }

    public void cordonNode(String nodeName) {
        updateNodeSchedulability(nodeName, false, "Cordoned by chaos experiment");
    }

    public void uncordonNode(String nodeName) {
        updateNodeSchedulability(nodeName, true, "Uncordoned by chaos experiment cleanup");
    }

    private void updateNodeSchedulability(String nodeName, boolean schedulable, String reason) {
        try {
            V1Node node = getNode(nodeName);
            List<V1NodeCondition> conditions = node.getStatus().getConditions();
            String taintKey = "node.kubernetes.io/unschedulable";
            String taintValue = "";
            String taintEffect = "NoSchedule";

            if (schedulable) {
                removeTaint(node, taintKey);
            } else {
                addTaint(node, new io.kubernetes.client.openapi.models.V1Taint()
                        .key(taintKey)
                        .effect(taintEffect)
                        .value(taintValue));
            }

            V1Node updatedNode = new V1Node()
                    .metadata(new V1ObjectMeta().name(nodeName))
                    .spec(node.getSpec())
                    .status(node.getStatus());

            coreApi.replaceNode(nodeName, updatedNode, null, null, null);
            log.info("Node {} schedulable={} ({})", nodeName, schedulable, reason);
        } catch (ApiException e) {
            throw new KubeApiException("Failed to update node schedulability: " + e.getMessage(), e);
        }
    }

    public void drainNode(String nodeName) {
        cordonNode(nodeName);
        List<PodInfo> pods = getPods("", null);
        List<String> namespaceExclude = List.of(config.getNamespace(), "kube-system", "kube-public", "kube-node-lease");
        List<PodInfo> evictable = pods.stream()
                .filter(p -> p.node().equals(nodeName))
                .filter(p -> !namespaceExclude.contains(p.namespace()))
                .collect(Collectors.toList());

        log.info("Draining node {}, evicting {} pods", nodeName, evictable.size());
        for (PodInfo pod : evictable) {
            try {
                deletePod(pod.namespace(), pod.name());
                log.info("Evicted pod {}/{}", pod.namespace(), pod.name());
            } catch (Exception e) {
                log.warn("Failed to evict pod {}/{}: {}", pod.namespace(), pod.name(), e.getMessage());
            }
        }
    }

    public void addTaint(V1Node node, io.kubernetes.client.openapi.models.V1Taint taint) {
        List<io.kubernetes.client.openapi.models.V1Taint> taints = Optional.ofNullable(node.getSpec().getTaints()).orElse(new ArrayList<>());
        taints.removeIf(t -> t.getKey().equals(taint.getKey()));
        taints.add(taint);
        node.getSpec().setTaints(taints);
    }

    public void removeTaint(V1Node node, String taintKey) {
        List<io.kubernetes.client.openapi.models.V1Taint> taints = Optional.ofNullable(node.getSpec().getTaints()).orElse(new ArrayList<>());
        taints.removeIf(t -> t.getKey().equals(taintKey));
        node.getSpec().setTaints(taints);
    }

    public V1Node getNode(String nodeName) {
        try {
            return coreApi.readNode(nodeName, null);
        } catch (ApiException e) {
            throw new KubeApiException("Failed to read node " + nodeName + ": " + e.getMessage(), e);
        }
    }

    public List<V1PersistentVolumeClaim> getPVCs(String namespace) {
        try {
            V1PersistentVolumeClaimList list = coreApi.listNamespacedPersistentVolumeClaim(
                    namespace, null, null, null, null, null, null, null, null, null
            );
            return list.getItems();
        } catch (ApiException e) {
            throw new KubeApiException("Failed to list PVCs: " + e.getMessage(), e);
        }
    }

    public List<V1PersistentVolume> getPVs() {
        try {
            V1PersistentVolumeList list = coreApi.listPersistentVolume(null, null, null, null, null, null, null, null, null);
            return list.getItems();
        } catch (ApiException e) {
            throw new KubeApiException("Failed to list PVs: " + e.getMessage(), e);
        }
    }

    private NodeInfo toNodeInfo(V1Node node) {
        String name = node.getMetadata().getName();
        boolean ready = node.getStatus().getConditions().stream()
                .anyMatch(c -> "Ready".equals(c.getType()) && "True".equals(c.getStatus()));
        boolean schedulable = Optional.ofNullable(node.getSpec().getUnschedulable()).orElse(false);
        List<String> taintStrings = Optional.ofNullable(node.getSpec().getTaints()).orElse(new ArrayList<>())
                .stream()
                .map(t -> t.getKey() + "=" + t.getValue() + ":" + t.getEffect())
                .collect(Collectors.toList());
        return new NodeInfo(name, ready, !schedulable, taintStrings.toArray(new String[0]));
    }

    private PodInfo toPodInfo(V1Pod pod) {
        String name = pod.getMetadata().getName();
        String namespace = pod.getMetadata().getNamespace();
        String phase = pod.getStatus().getPhase();
        String node = pod.getSpec().getNodeName();
        boolean ready = pod.getStatus().getContainerStatuses().stream()
                .allMatch(c -> c.getReady());
        String[] containers = Optional.ofNullable(pod.getSpec().getContainers())
                .orElse(new ArrayList<>())
                .stream()
                .map(c -> c.getName())
                .toArray(String[]::new);
        return new PodInfo(name, namespace, phase, node, ready, containers);
    }

    public void execInContainer(String namespace, String podName, String containerName, String command) {
        try {
            List<String> commands = Arrays.asList("/bin/sh", "-c", command);
            coreApi.connectPostNamespacedPodExec(
                    podName, namespace, null, null, null,
                    Boolean.TRUE, Boolean.TRUE, null,
                    commands, null, null, null
            );
            log.info("Executed '{}' in {}/{}/{}", command, namespace, podName, containerName);
        } catch (ApiException e) {
            throw new KubeApiException("Failed to exec in container: " + e.getMessage(), e);
        }
    }

    public void detachVolume(String namespace, String podName, String volumeName) {
        try {
            V1Pod pod = coreApi.readNamespacedPod(podName, namespace, null);
            V1PodSpec spec = pod.getSpec();
            spec.setVolumes(spec.getVolumes().stream()
                    .filter(v -> !volumeName.equals(v.getName()))
                    .collect(Collectors.toList()));
            spec.setContainers(spec.getContainers().stream()
                    .peek(c -> c.setVolumeMounts(
                            Optional.ofNullable(c.getVolumeMounts()).orElse(new ArrayList<>()).stream()
                                    .filter(vm -> !volumeName.equals(vm.getName()))
                                    .collect(Collectors.toList())
                    ))
                    .collect(Collectors.toList()));
            coreApi.replaceNamespacedPod(podName, namespace, pod, null, null, null);
            log.info("Detached volume {} from pod {}/{}", volumeName, namespace, podName);
        } catch (ApiException e) {
            throw new KubeApiException("Failed to detach volume: " + e.getMessage(), e);
        }
    }

    public long getClusterCapacity() {
        List<NodeInfo> nodes = getNodes();
        return nodes.size();
    }

    public boolean checkPodsReady(String namespace, String prefix) {
        List<PodInfo> pods = getPodsByPrefix(namespace, prefix);
        return pods.stream().allMatch(PodInfo::ready);
    }

    public boolean waitForPodsRunning(String namespace, String prefix, int timeoutSeconds) {
        Instant deadline = Instant.now().plusSeconds(timeoutSeconds);
        while (Instant.now().isBefore(deadline)) {
            if (checkPodsReady(namespace, prefix)) {
                log.info("All pods matching '{}' are ready in namespace {}", prefix, namespace);
                return true;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        log.warn("Timeout waiting for pods '{}' in namespace {}", prefix, namespace);
        return false;
    }

    public boolean waitForPodsRunning(String namespace, String prefix) {
        return waitForPodsRunning(namespace, prefix, config.getExperimentTimeout());
    }

}
