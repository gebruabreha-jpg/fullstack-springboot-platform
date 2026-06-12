Level 1: traditional Linux basics

This is what most people start with:

files (ls, cp, mv, find)
permissions (chmod, chown)
processes (ps, top, kill)
networking (ip, ping, ss, netstat)
services (systemctl)
logs (journalctl)
package managers (apt, yum)

This is the everyday admin layer.

Level 2: system internals basics

This is where namespaces live:

processes and forks
PIDs
signals
sockets
mounts
/proc
cgroups
namespaces
capabilities

This is still “basic Linux” for:

DevOps
SRE
backend engineers
security engineers

because containers depend on it.

Level 3: kernel-level understanding

Deeper topics:

scheduler
virtual memory
page cache
syscall paths
kernel modules
eBPF
NUMA
interrupt handling

That’s closer to systems engineering.