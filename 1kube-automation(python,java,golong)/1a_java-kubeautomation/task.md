phase1:-
Kubernetes API interactions (client libraries)
Chaos injection (pod kill, network tc, node drain, etc.)
Helm/Litmus integration
Gherkin-style test definitions 
        Python: behave/lettuce, 
        Go: godog/cucumber,
        java: Cucumber JVM

Initialize Maven/Gradle project (pom.xml or build.gradle)
Add core dependencies: Kubernetes Java Client (io.kubernetes:client-java), Lombok (optional), JUnit 5, Cucumber (for BDD .feature integration)
Define module structure: api/, chaos/, health/, helm/, infra/, config/
Set up logging (SLF4J + Logback) and config loading (application.yaml / env vars for kubeconfig)

phase2:-
ChaosAction interface - base interface for all chaos actions
ChaosConfig class - loads config from application.yaml + env vars
ChaosResult class - result tracking for experiments
KubeConfigLoader utility - handles kubeconfig loading

entrypoint.sh lets you:-
Option A (current): Shell script handles env vars + checks, then runs java -jar 
Set KUBECONFIG from env at container runtime without rebuilding the image
Pass JVM tuning flags via env (JVM_OPTS="-Xmx512m")


Option B (simpler):- Remove entrypoint.sh, set ENTRYPOINT ["java"] directly in Dockerfile, pass config as CMD ["-jar", "/app/app.jar", "--config", "/app/config/application.yaml"]



phase2:-
KubectlApi facade, HelmClient, InfraClient
api/KubectlApi.java, helm/HelmClient.java, infra/InfraClient.java
✅ KubectlApi pod/node/PVC/event wrapper
✅ HelmClient install/upgrade/rollback/uninstall
✅ Promethus metric/counter/guage

Phase 4 — Kubernetes Client Wrappers 7. Implement KubectlApi facade: pod CRUD, node operations, PVC/PV checks, events 8. Implement HelmClient: install/upgrade/rollback/uninstall, release health validation 9. Implement LitmusClient: run/cleanup experiments, fetch chaos results 10. Implement InfraClient: IPMI, systemctl wrappers (node-level infra)

Phase 5 — Chaos Engine Core 11. Build ChaosEngine orchestrator (executes actions sequentially or in parallel) 12. Implement fault injection per domain: container kill, pod delete, node cordon/drain, network netem, volume detach, control-plane restart 13. Build ActionChain composite executor (e.g., node_drain + network_loss) 14. Implement CleanupManager for post-experiment recovery (uncordon, restore network, delete CRDs)

Phase 6 — Verification & Observability 15. Build HealthChecker: pod ready, node ready/pressure, service availability, PVC status 16. Implement EventFetcher: fetchEvents(), getRecentFailures() with filtering 17. Add result tracking: pass/fail assertion per experiment, chaos result export (JSON/HTML)

Phase 7 — BDD Integration 18. Wire Cucumber step definitions to Java implementations for each .feature file 19. Map Gherkin Given/When/Then → KubectlApi / ChaosEngine calls 20. Configure Cucumber runner classes and reporting

Phase 8 — Robustness 21. Add retry/backoff logic for transient API failures 22. Implement timeout & cancellation per chaos action 23. Add RBAC/service-account validation at startup (fail fast if no cluster access) 24. Write unit tests (mocked ApiClient) and integration tests (real/minikube)

Phase 9 — DevOps / Release 25. Add CI pipeline (.github/workflows/) — lint, unit test, build Docker image 26. Create Helm chart or Kustomize overlay to deploy this tool into a cluster 27. Final README with usage examples, architecture diagram, and failure taxonomy reference

