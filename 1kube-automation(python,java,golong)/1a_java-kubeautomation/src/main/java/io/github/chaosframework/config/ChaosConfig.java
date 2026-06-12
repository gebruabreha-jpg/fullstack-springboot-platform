package io.github.chaosframework.config;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class ChaosConfig {
    private final Properties properties;

    public ChaosConfig() {
        this.properties = new Properties();
        loadDefaults();
        loadFromEnv();
    }

    public ChaosConfig(String configPath) throws IOException {
        this.properties = new Properties();
        loadDefaults();
        loadFromFile(configPath);
        loadFromEnv();
    }

    private void loadDefaults() {
        properties.setProperty("kubeconfig.path", "~/.kube/config");
        properties.setProperty("cluster.name", "default");
        properties.setProperty("namespace", "default");
        properties.setProperty("experiment.timeout", "300");
        properties.setProperty("chaos.result.ttl", "3600");
        properties.setProperty("experiment.retries", "3");
        properties.setProperty("experiment.retry.delay", "5");
        properties.setProperty("helm.chart.timeout", "120");
        properties.setProperty("infra.ipmi.timeout", "10");
    }

    private void loadFromFile(String configPath) throws IOException {
        Path resolved = Paths.get(configPath).toAbsolutePath().normalize();
        String lower = resolved.toString().toLowerCase();
        if (lower.endsWith(".yaml") || lower.endsWith(".yml")) {
            loadFromYaml(resolved.toString());
        } else if (lower.endsWith(".properties")) {
            try (InputStream is = new FileInputStream(resolved.toFile())) {
                properties.load(is);
            }
        } else {
            try (InputStream is = new FileInputStream(resolved.toFile())) {
                properties.load(is);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromYaml(String path) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream is = new FileInputStream(path)) {
            Map<String, Object> map = yaml.load(is);
            if (map == null) return;
            flattenMap("", map).forEach(properties::setProperty);
        }
    }

    private java.util.Map<String, String> flattenMap(String prefix, Map<String, Object> map) {
        java.util.Map<String, String> flat = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map) {
                flat.putAll(flattenMap(key, (Map<String, Object>) entry.getValue()));
            } else {
                flat.put(key, String.valueOf(entry.getValue()));
            }
        }
        return flat;
    }

    private void loadFromEnv() {
        System.getenv().forEach((key, value) -> {
            if (key.startsWith("CHAOS_")) {
                String propKey = key.substring(6).toLowerCase().replace('_', '.');
                properties.setProperty(propKey, value);
            }
        });
        if (System.getenv("KUBECONFIG") != null) {
            properties.setProperty("kubeconfig.path", System.getenv("KUBECONFIG"));
        }
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public Path getKubeConfigPath() {
        String path = get("kubeconfig.path");
        if (path == null) return Paths.get(System.getProperty("user.home"), ".kube", "config");
        if (path.startsWith("~")) {
            String home = System.getProperty("user.home");
            return Paths.get(path.replaceFirst("^~", home));
        }
        return Paths.get(path);
    }

    public String getClusterName() {
        return get("cluster.name", "default");
    }

    public String getNamespace() {
        return get("namespace", "default");
    }

    public int getExperimentTimeout() {
        return getInt("experiment.timeout", 300);
    }

    public int getChaosResultTtl() {
        return getInt("chaos.result.ttl", 3600);
    }

    public int getMaxRetries() {
        return getInt("experiment.retries", 3);
    }

    public int getRetryDelaySeconds() {
        return getInt("experiment.retry.delay", 5);
    }
}
