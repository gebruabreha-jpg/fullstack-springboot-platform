package io.github.chaosframework.api.metrics;

import io.github.chaosframework.chaos.ChaosResult;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChaosMetrics {
    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Counter> experimentCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> actionCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> actionTimers = new ConcurrentHashMap<>();

    private static final String EXPERIMENT_PREFIX = "chaos.experiment";
    private static final String ACTION_PREFIX = "chaos.action";

    public ChaosMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordExperimentCompleted(ChaosResult result) {
        String counterName = EXPERIMENT_PREFIX + ".completed.total";
        Counter counter = experimentCounters.computeIfAbsent(counterName, k ->
                Counter.builder(k)
                        .description("Total chaos experiments completed")
                        .register(meterRegistry)
        );
        counter.increment(result.isSuccess() ? 1.0 : 0.0);
    }

    public void recordExperimentFailed(String reason) {
        String counterName = EXPERIMENT_PREFIX + ".failed.total";
        Counter counter = experimentCounters.computeIfAbsent(counterName, k ->
                Counter.builder(k)
                        .description("Total chaos experiments failed")
                        .register(meterRegistry)
        );
        counter.increment();
    }

    public void recordActionCompleted(String actionName, long durationMs, boolean success) {
        String counterName = ACTION_PREFIX + "." + sanitize(actionName) + ".completed.total";
        Counter counter = actionCounters.computeIfAbsent(counterName, k ->
                Counter.builder(k)
                        .description("Count of " + actionName + " executions")
                        .tag("action", actionName)
                        .register(meterRegistry)
        );

        counter.increment();

        String timerName = ACTION_PREFIX + "." + sanitize(actionName) + ".duration";
        Timer timer = actionTimers.computeIfAbsent(timerName, k ->
                Timer.builder(k)
                        .description("Duration of " + actionName + " execution")
                        .tag("action", actionName)
                        .register(meterRegistry)
        );
        timer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordActionError(String actionName, String errorType) {
        String counterName = ACTION_PREFIX + "." + sanitize(actionName) + ".error.total";
        Counter counter = actionCounters.computeIfAbsent(counterName, k ->
                Counter.builder(k)
                        .description("Count of errors in " + actionName)
                        .tag("action", actionName)
                        .tag("error", errorType)
                        .register(meterRegistry)
        );
        counter.increment();
    }

    public double getExperimentCompletedCount() {
        Counter counter = experimentCounters.get(EXPERIMENT_PREFIX + ".completed.total");
        return counter != null ? counter.count() : 0.0;
    }

    public double getExperimentFailedCount() {
        Counter counter = experimentCounters.get(EXPERIMENT_PREFIX + ".failed.total");
        return counter != null ? counter.count() : 0.0;
    }

    private String sanitize(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
