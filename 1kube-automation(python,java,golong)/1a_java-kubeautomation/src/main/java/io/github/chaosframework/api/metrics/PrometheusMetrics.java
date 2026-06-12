package io.github.chaosframework.api.metrics;

import io.github.chaosframework.chaos.ChaosResult;
import io.prometheus.client.exporter.HTTPServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;

@Slf4j
public class PrometheusMetrics {
    private final ChaosMetrics mainMetrics;
    private final ChaosMetricsCollector collector;
    private HTTPServer httpServer;

    public PrometheusMetrics(ChaosMetrics mainMetrics, ChaosMetricsCollector collector) {
        this.mainMetrics = mainMetrics;
        this.collector = collector;
    }

    public void start(int port) {
        try {
            io.prometheus.client.CollectorRegistry registry = new io.prometheus.client.CollectorRegistry(true);
            registry.register(collector);

            this.httpServer = new HTTPServer(new InetSocketAddress(port), registry);
            log.info("Prometheus metrics endpoint started on port {}", port);
        } catch (IOException e) {
            log.error("Failed to start Prometheus HTTP server: {}", e.getMessage());
            throw new RuntimeException("Failed to start Prometheus metrics server", e);
        }
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.close();
            log.info("Prometheus metrics endpoint stopped");
        }
    }

    public void recordChaosResult(ChaosResult result) {
        collector.incrementTotalExperiments();
        collector.incrementActionExecutions(result.getActionName());

        if (result.isSuccess()) {
            collector.incrementSuccessfulExperiments();
        } else {
            collector.incrementFailedExperiments();
            collector.incrementActionFailures(result.getActionName());
        }

        mainMetrics.recordExperimentCompleted(result);
    }

    public void updateRunningExperiments(long runningCount) {
        collector.setRunningExperiments(runningCount);
    }

    public boolean isRunning() {
        return httpServer != null;
    }
}
