package io.github.chaosframework.chaos;

import io.github.chaosframework.config.ChaosConfig;
import lombok.Data;
import java.util.Map;

@Data
public class ChaosContext {
    private final String experimentId;
    private final ChaosConfig config;
    private final Map<String, Object> parameters;

    public ChaosContext(String experimentId, ChaosConfig config) {
        this(experimentId, config, Map.of());
    }

    public ChaosContext(String experimentId, ChaosConfig config, Map<String, Object> parameters) {
        this.experimentId = experimentId;
        this.config = config;
        this.parameters = parameters;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }
}
