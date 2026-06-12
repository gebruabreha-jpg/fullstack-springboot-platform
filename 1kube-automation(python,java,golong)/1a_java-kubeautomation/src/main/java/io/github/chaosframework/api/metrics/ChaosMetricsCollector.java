package io.github.chaosframework.api.metrics;

import io.github.chaosframework.chaos.ChaosResult;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import io.prometheus.client.SummaryMetricFamily;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ChaosMetricsCollector extends Collector {
    private final AtomicLong totalExperiments = new AtomicLong(0);
    private final AtomicLong successfulExperiments = new AtomicLong(0);
    private final AtomicLong failedExperiments = new AtomicLong(0);
    private final AtomicLong runningExperiments = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> actionExecutions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> actionFailures = new ConcurrentHashMap<>();

    public void incrementTotalExperiments() {
        totalExperiments.incrementAndGet();
    }

    public void incrementSuccessfulExperiments() {
        successfulExperiments.incrementAndGet();
    }

    public void incrementFailedExperiments() {
        failedExperiments.incrementAndGet();
    }

    public void setRunningExperiments(long count) {
        runningExperiments.set(count);
    }

    public void incrementActionExecutions(String actionName) {
        actionExecutions.computeIfAbsent(actionName, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void incrementActionFailures(String actionName) {
        actionFailures.computeIfAbsent(actionName, k -> new AtomicLong(0)).incrementAndGet();
    }

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> samples = new ArrayList<>();

        samples.add(new GaugeMetricFamily(
                "chaos_experiments_total",
                "Total number of chaos experiments executed",
                totalExperiments.get()
        ));

        samples.add(new GaugeMetricFamily(
                "chaos_experiments_successful",
                "Total number of successful chaos experiments",
                successfulExperiments.get()
        ));

        samples.add(new GaugeMetricFamily(
                "chaos_experiments_failed",
                "Total number of failed chaos experiments",
                failedExperiments.get()
        ));

        samples.add(new GaugeMetricFamily(
                "chaos_experiments_running",
                "Number of currently running chaos experiments",
                runningExperiments.get()
        ));

        actionExecutions.forEach((action, count) -> {
            double failures = actionFailures.getOrDefault(action, new AtomicLong(0)).get();
            samples.add(new GaugeMetricFamily(
                    "chaos_action_executions_total",
                    "Executions per chaos action",
                    Arrays.asList(action),
                    Collections.singletonList((double) count.get())
            ));
            samples.add(new GaugeMetricFamily(
                    "chaos_action_failures_total",
                    "Failures per chaos action",
                    Arrays.asList(action),
                    Collections.singletonList(failures)
            ));
        });

        return samples;
    }
}
