# Architecture Overview

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Language** | Python 3.11+ |
| **BDD Framework** | Behave (Cucumber for Python) |
| **K8s Client** | kubernetes (official Python client) |
| **HTTP Client** | requests |
| **YAML Processing** | PyYAML |
| **Metrics** | prometheus-client |

## Why Python (Not Go + Python)?

| Requirement | Python Solution | Why Better Than Go |
|-------------|-----------------|------------------|
| 100+ test steps | ~6-7 steps/class → concise code | Go needs 3-5x more boilerplate |
| Helm deployment | subprocess + helm binary | Go adds no value; needs same approach |
| Log collection | kubernetes CoreV1Api | Direct API access, no abstraction needed |
| Development speed | No compilation step | Instant feedback vs Go compile cycle |

## Project Structure

```
kubeplatformautomation/
├── features/              # BDD scenarios (.feature files)
│   ├── 1.feature          # Main E2E flow
│   ├── deployment.feature # Helm deployment
│   ├── health_check.feature
│   ├── system_test.feature
│   └── validation.feature
├── steps/                  # Step definitions (10 files)
│   ├── 1.py                # Infrastructure setup
│   ├── config_steps.py     # Environment setup
│   ├── deployment_steps.py # Helm operations
│   ├── health_steps.py     # Pod readiness
│   ├── log_steps.py        # Log collection
│   ├── metric_steps.py     # Prometheus queries
│   ├── report_steps.py     # Report generation
│   ├── test_steps.py       # Test execution
│   └── validation_steps.py # Assertions
├── infra/
│   ├── README.md           # KWOK documentation
│   ├── setup.sh/.ps1       # Full setup (Go + Docker)
│   ├── script/
│   │   ├── setup_kwok.sh      # Minimal cluster setup
│   │   ├── teardown_kwok.sh  # Cluster teardown
│   │   ├── install_tools.ps1 # Windows installer
│   │   └── health_check.py   # Cluster health
│   └── charts/fake-*.yaml  # Fake K8s resources
└── ARCHITECTURE.md
```

## API Mapping: Beets → Python

| Beets API | Python Equivalent |
|----------|-----------------|
| KubectlApi | `kubernetes.CoreV1Api` |
| HelmRelease | subprocess.run(["helm", ...]) |
| PM Server API | prometheus-client |
| JCAT Assertions | pytest assertions |
| JUnit/TestNG | behave + pytest |

## Target: 15 Step Classes, ~100 Steps

| File | Focus Area | Steps |
|------|------------|-----|
| config_steps.py | Environment | 4 |
| deployment_steps.py | Helm deploy | 4 |
| health_steps.py | Pod checks | 5 |
| log_steps.py | Log collection | 6 |
| metric_steps.py | Prometheus | 5 |
| report_steps.py | Reports | 3 |
| test_steps.py | Execution | 4 |
| validation_steps.py | Assertions | 18 |
| 1.py | Infrastructure | 3 |

## Running Tests

```bash
# Without cluster (mock mode)
behave

# With KWOK cluster
./infra/setup.sh
behave
kwokctl delete cluster kwok-cluster
```