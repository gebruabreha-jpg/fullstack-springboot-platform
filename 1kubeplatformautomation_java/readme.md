Summary: Java Design Patterns in PCC Framework & Beets
Interface + Implementation Pattern
Naming: Interfaces typically use *Api, *Impl suffix (e.g., AdpBrm/AdpBrmImpl, TRexRunnerApi/TRexRunner, Ixia/IxiaImpl)
Location: Interfaces live in pcc-test-framework, implementations can be in either module
Purpose: Separates API contract from implementation, enables mocking and DI
Dependency Injection
Framework: Google Guice with Jakarta Inject annotations
Modules: GuiceBindingModule (pcc-test-framework) and BeetsGuiceModule (beets)
Injection styles:
Constructor injection via @Inject on constructors (most common)
Method injection via @Provides annotated provider methods
Static injection for test fixtures (Cucumber hooks, TestNG integration)
Scopes: @TestcaseScoped, @Singleton, @Named qualifiers
Factory Pattern
Guice FactoryModuleBuilder: Used for JenkinsApiFactory, HelmApplication.Factory, EricPcMmFactory, DallasPreInstallFactory
Custom factories: PccActionsFactory, PcgActionsFactory, DeploymentSizeHandlerFactory
Cluster/Multi-instance Pattern
ClusterMap suffix: AdpCmProviderClusterMap, K8sNodeApisClusterMap, SearchEngineApiProviderClusterMap
Handles multiple cluster/endpoint instances with getFirst(), get(cluster) methods
Builder Pattern
LinuxNavigatorBuilder for shell navigator configuration
DallasPackage/OutlinePackage using factory methods (create())
Strategy Pattern
SutActions with PccActionsFactory, PcgActionsFactory, WmgActions - selected based on product type



The patterns are indeed abundant, but necessary for this enterprise telecom test automation framework that tests complex systems like:
Multiple products: PCC, PCG, WMG (handled via Strategy pattern with SutActions)
Multi-cluster/Kubernetes environments: Each cluster needs its own API instances (ClusterMap pattern)
External integrations: Artifactory, Jenkins, SFTP, Kubernetes, Helm, etc.
Test lifecycle complexity: Parallel execution, cleanup, timeouts, logging
Configuration management: Multiple config sources, namespace handling, secrets
ActionsHelper.java (line 99) is intentionally final with private constructor - a utility class pattern for static helper methods, avoiding instantiation.
Without these patterns, the framework couldn't manage the complexity of testing Ericsson's cloud-native telecom infrastructure across diverse environments.


Multi-Tenant SaaS Patterns (Keep/Drop):-
Keep (adapted):
Interface + Impl → Tenant-aware service contracts
DI scoping → @RequestScope or @TenantScope (Spring) instead of TestcaseScoped
ClusterMap → Simplified to TenantContext - map tenant ID to resources

Still Drop (telecom-specific):
SSH shell navigators (TRexRunner, ToolServerNavigator)
Port forwarding (KeepAlivePortForwarder)
Hardware/network abstractions (dpdk_nic_bind.py, PCI device management)
Telecom protocols (Yang CLI, SNMP traps)

Modern Java alternatives they could adopt:-
Records for DTOs instead of getters/setters
Pattern matching (instanceof: if (obj instanceof String s) {...})
Virtual threads (Java 21) instead of ParallelExecutionUtil
HttpClient built-in instead of Apache HttpClients
JUnit 5 + Quarkus/Micronaut + Kubernetes Java client
java11+ HttpClient, Virtual threads (Java 21), built-in DI
Core Features (P0):
Configuration loader - YAML/JSON properties system
Single tool driver - Start with simplest like TRexRunner equivalent (start/stop/upload)
Test lifecycle - Setup/teardown hooks (JUnit 5 extensions)
Kubernetes integration - Pod status, exec, port-forward
Assertions API - Fluent assertions for test results
Next features (P1):
Helm chart management
Artifactory/binary downloads
Parallel execution with virtual threads
Cleanup registry pattern
Logging/metrics collection
Later (P2/P3):
Multi-cluster/tenant support
Telecom-specific protocols (SNMP, Yang CLI)
Advanced reporting (JCAT equivalent)
Security/certificates management