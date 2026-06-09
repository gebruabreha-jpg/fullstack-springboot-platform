K8sNode.java (base class) - Lines 875-921 handle KERNEL_PANICKED via triggerKernelPanic() which sends kernel panic command asynchronously, then reconnects after timeout.

K8sBaremetalNode.java - Lines 149-218:

SHUT_DOWN: Uses IPMI (ipmitool.shutdownNode()) or kubectl annotation (POWER_OFF_ANNOTATION)
RESTARTED/FORCE_RESTARTED: IPMI restart or CAPI-based VM restart via kubectl command
STARTED: IPMI start or CAPI power-on
K8sMesosNode.java - Lines 98-107: Uses computeHost.shutdownNode(), computeHost.rebootNodeWithForce(), computeHost.destroyNode().


K8sBaremetalNode.java (lines 130-230)
Action	Implementation
SHUT_DOWN	IPMI: ipmitool.shutdownNode(ipAddress) or CAPI: kubectl with POWER_OFF_ANNOTATION
RESTARTED	IPMI: ipmitool.restartNode(ipAddress) or CAPI: power off → on cycle
FORCE_RESTARTED	IPMI: ipmitool.restartNodeWithForce(ipAddress)
KERNEL_PANICKED	Calls performKernelPanic() which triggers panic and reconnects
CORDONED	kubectlApi.cordonNode(getHostname())
UNCORDONED	kubectlApi.uncordonNode(getHostname(), flags)
DRAINED	kubectlApi.drainNode(getHostname(), flags)
K8sMesosNode.java (lines 84-126)
Action	Implementation
SHUT_DOWN	computeHost.shutdownNode(getHostname())
RESTARTED	computeHost.rebootNode(getHostname())
FORCE_RESTARTED	computeHost.rebootNodeWithForce(getHostname())
DESTROYED	computeHost.destroyNode(getHostname())
KERNEL_PANICKED	echo c > /proc/sysrq-trigger via base class
NexusNode.java (lines 85-115)
Action	Implementation
SHUT_DOWN	Not supported (throws exception)
RESTARTED/FORCE_RESTARTED	azureHandler.restartNode(getHostname())
CORDONED	kubectlApi.cordonNode(getHostname())
UNCORDONED	kubectlApi.uncordonNode(getHostname(), flags)
GdceNode.java (lines 73-113)
Action	Implementation
SHUT_DOWN	Not supported (throws exception)
RESTARTED	Pub/Sub NodeRebootRequest with graceful=true
FORCE_RESTARTED	Pub/Sub NodeRebootRequest with graceful=false
CORDONED	kubectlApi.cordonNode(getHostname())
K8sCeeNode.java (lines 141-191)
Action	Implementation
SHUT_DOWN	openstackHandler.shutdownServer(getHostname())
RESTARTED	openstackHandler.rebootServer(getHostname())
FORCE_RESTARTED	openstackHandler.rebootServerWithForce(getHostname())
KERNEL_PANICKED	echo c > /proc/sysrq-trigger
CORDONED	kubectlApi.cordonNode(getHostname())
K8sLpgNode.java (lines 52-74)
Action	Implementation
SHUT_DOWN	ipmitool.shutdownNode(ipAddress)
RESTARTED	ipmitool.restartNode(ipAddress)
FORCE_RESTARTED	ipmitool.restartNodeWithForce(ipAddress)
KERNEL_PANICKED	echo c > /proc/sysrq-trigger


PCC Test Framework - All performAction implementations:-
File	Lines	Cluster Type	Actions Supported
K8sBaremetalNode.java	130-172	Baremetal	All (RESTARTED, FORCE_RESTARTED, SHUT_DOWN, KERNEL_PANICKED, DRAINED, CORDONED, UNCORDONED)
K8sMesosNode.java	84-126	Mesos	SHUT_DOWN, RESTARTED, FORCE_RESTARTED, DESTROYED, KERNEL_PANICKED, DRAINED, CORDONED, UNCORDONED
K8sOpenshiftNode.java	163-190	Openshift	KERNEL_PANICKED only (SHUT_DOWN/STARTED/Destoryed not supported)
NexusNode.java	85-116	Nexus/K8s	RESTARTED, FORCE_RESTARTED, CORDONED, UNCORDONED
GdceNode.java	73-113	GDCE	RESTARTED, FORCE_RESTARTED, CORDONED, UNCORDONED, DRAINED
K8sCeeNode.java	141-192	CEE/OpenStack	All except DESTROYED/POWER_STATUS
K8sLpgNode.java	52-74	LPG	STARTED, RESTARTED, FORCE_RESTARTED, SHUT_DOWN, KERNEL_PANICKED, POWER_STATUS