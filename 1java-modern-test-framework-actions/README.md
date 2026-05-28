# Modern Test Framework

A modern test framework built with Java, Quarkus, Kubernetes client, Helm, and JUnit 5.

## Project Structure

```
modern-test-framework/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/testfw/
│   │           ├── config/TestConfig.java
│   │           ├── driver/TRexDriver.java
│   │           └── util/ParallelExecutor.java
│   └── test/
│       └── java/
│           └── com/example/testfw/
│               ├── driver/TRexDriverTest.java
│               └── util/ParallelExecutorTest.java
├── src/
│   └── main/
│       └── resources/
│           └── application.yaml
└── README.md
```

## Components Created

1. **TestConfig.java** - Configuration record for test environment
   - Fields: namespace, cluster, kubeconfig (Path)

2. **TRexDriver.java** - Driver for managing TRex traffic generator
   - Methods: start(), stop(), isRunning(), upload(), download(), collectLogs()
   - Uses Kubernetes client for cluster operations (no SSH/shell execution)
   - Manages TRex via Deployments, Services, and ConfigMaps
   - Supports Helm chart deployment for complex environments

3. **ParallelExecutor.java** - Modern parallel execution utility
   - Uses Java 21 StructuredTaskScope and virtual threads
   - Replaces legacy parallel execution utilities
   - Provides structured concurrency with proper error handling

4. **Tests** - JUnit 5 tests verifying:
   - TRexDriver lifecycle and method signatures
   - ParallelExecutor functionality (success, failure, timeout)

5. **application.yaml** - Configuration file with test defaults
   ```yaml
   test:
     namespace: test-ns
     cluster: test-cluster
     kubeconfig: ~/.kube/config
   ```

## Dependencies

- Quarkus Core
- JUnit Jupiter (test scope)
- Fabric8 Kubernetes Client
- Fabric8 Helm Client (for chart-based deployments)
- Quarkus Micrometer Registry Prometheus (for metrics observability)
- SLF4J API (for logging)

## Key Modernizations vs Legacy PCC Framework

| Legacy Pattern | Modern Replacement |
|----------------|-------------------|
| Guice DI Modules | Quarkus CDI (@Inject, @Singleton) |
| SSH/Shell Navigation (ToolServerNavigator) | Fabric8 Kubernetes Client (direct API calls) |
| TestNG + Cucumber Hybrid | JUnit 5 + Quarkus Test |
| Static Injection | Proper DI with @Inject |
| Custom Parallel Execution Utilities | Java 21 StructuredTaskScope (virtual threads) |
| Manual Resource Cleanup | Try-with-resources & Kubernetes client auto-close |
| Imperative Shell Commands | Declarative Kubernetes API (Deployments, ConfigMaps) |
| Manual YAML Application | Helm Chart Management |
| Limited Observability | Prometheus Metrics Export |

## Usage Example

```java
// Configure test environment
TestConfig config = new TestConfig("test-ns", "test-cluster", 
        Path.of(System.getProperty("user.home"), ".kube", "config"));

// Create and start TRex driver using Helm chart
TRexDriver trexDriver = new TRexDriver(config);
trexDriver.startWithHelmChart("oci://registry.example.com/trex-chart", 
        Map.of("replicaCount", "2", "image.tag", "latest"));

// Verify TRex is running
assertTrue(trexDriver.isRunning());

// Upload configuration to TRex
trexDriver.upload(Path.of("local-config.lua"), "trex-config.lua");

// Execute test scenario (example: parallel traffic generation)
List<Callable<Void>> trafficTasks = List.of(
        () -> { trexDriver.startTraffic("http"); return null; },
        () -> { trexDriver.startTraffic("https"); return null; }
);
ParallelExecutor.executeAll(trafficTasks);

// Collect results
String logs = trexDriver.collectLogs();
assertTrue(logs.contains("TRAFFIC STARTED"));

// Collect metrics (if Prometheus integration enabled)
String metrics = trexDriver.collectMetrics();
assertTrue(metrics.contains("test_framework_"));

// Cleanup
trexDriver.stop();
```

## Next Steps

1. **Add more tool drivers** (Ixia, IXIA, etc.) following the same pattern
2. **Create test orchestration service** to manage multiple tools
3. **Implement advanced assertions** for test result validation
4. **Add multi-cluster/tenant support** using Kubernetes contexts
5. **Create JUnit 5 extensions** for automatic setup/teardown
6. **Add custom metrics** for specific test KPIs

## Build and Test

### Prerequisites
- Java 21+
- Maven 3.8+
- Access to Kubernetes cluster (for integration tests)
- Helm v3+ (for chart operations)
- Prometheus server (optional, for metrics scraping)
- Quarkus extensions (optional, for dev mode)

### Commands
```bash
# Compile and test
./mvnw compile test-compile

# Run unit tests
./mvnw test

# Run in Quarkus dev mode (optional)
./mvnw quarkus:dev

# Package as executable jar
./mvnw package
```

## Design Principles

1. **Immutable Configuration** - Using Java records for config objects
2. **Resource Safety** - Try-with-resources for Kubernetes client
3. **Structured Concurrency** - Java 21 StructuredTaskScope for parallel execution
4. **Declarative Infrastructure** - Kubernetes API instead of shell commands
5. **Chart-Based Deployments** - Helm for complex test environments
6. **Observability** - Prometheus metrics for test execution insights
7. **Fail Fast** - Immediate validation and clear error messages
8. **Testability** - Dependency injection and interface segregation

The framework is designed to be extensible - new tool drivers can be added by implementing similar patterns using the Kubernetes client for resource management, with optional Helm chart support for complex deployments.