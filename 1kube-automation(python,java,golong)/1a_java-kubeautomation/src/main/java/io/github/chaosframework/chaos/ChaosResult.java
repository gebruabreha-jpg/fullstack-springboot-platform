package io.github.chaosframework.chaos;

import lombok.Getter;

@Getter
public class ChaosResult {
    private final String experimentId;
    private final String actionName;
    private final boolean success;
    private final String message;
    private final long durationMs;
    private final long timestamp;
    private final ExperimentStatus experimentStatus;

    public ChaosResult(String experimentId, String actionName, boolean success, String message, long durationMs) {
        this.experimentId = experimentId;
        this.actionName = actionName;
        this.success = success;
        this.message = message;
        this.durationMs = durationMs;
        this.timestamp = System.currentTimeMillis();
        this.experimentStatus = success ? ExperimentStatus.PASSED : ExperimentStatus.FAILED;
    }

    public ExperimentStatus getExperimentStatus() {
        return experimentStatus;
    }

    public enum ExperimentStatus {
        PENDING,
        RUNNING,
        PASSED,
        FAILED,
        ERROR
    }
}
