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