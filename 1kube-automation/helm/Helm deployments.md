# Helm Deployment Actions — Reference

PCC Test Framework (HelmApi.java methods):-
- installHelmChart(chart, releaseName, namespace, values, version) - Full install with values
- installHelmChart(chart, releaseName, namespace, values, files, arguments, version) - Install with files and arguments
- upgradeHelmChart(releaseName, chart, namespace, values, files, commandLineParameters, commandLineSwitches) - Helm upgrade
- deleteChart(releaseName, namespace) - Helm uninstall
- rollbackToProvidedRevision(releaseName, revision, namespace) - Helm rollback

Beets Framework (DeploymentSteps.java):-
- the Helm chart is installed in namespace {string} with the following parameters
- the Helm chart is removed and redeployed
- saved helm chart is reinstalled
- Helm chart is {helm_action} while {int} pods from the following prefixes are restarted

## HELM CHART DEPLOYMENT ACTIONS SUMMARY

| Category | Action/Method | Location |
|----------|---------------|----------|
| Install | installHelmChart(chart, releaseName, namespace, values, version) | Helm.java:287-290 |
| Install | installHelmChart(chart, releaseName, namespace, values, files, arguments, version) | Helm.java:293-336 |
| Upgrade | upgradeHelmChart(releaseName, chart, namespace, values, files, params, switches) | Helm.java:339-354 |
| Upgrade | upgradeHelmChartAtomic(...) | Helm.java:381-408 |
| Delete | deleteChart(releaseName, namespace) | Helm.java:130-137 |
| Delete | deleteChartWithNoHooks(releaseName) | Helm.java:151-158 |
| Rollback | rollbackToProvidedRevision(releaseName, revisionNumber, namespace) | Helm.java:424-434 |

## Key Implementation Patterns

### Install Pattern
```
1. Build InstallOptions via builder
2. Call helmShell.install() with options
3. Check return value, log errors
4. Validate warnings if configured
5. Record timing to test statistics
```

### Upgrade with Pod Restart Pattern
```
1. Build HelmInstallOptions with flags
2. Start parallel task that:
   - Waits for upgrade start signal
   - Randomly selects pods matching prefixes
   - Executes PodAction.RESTARTED on each
   - Logs restarted pod names
3. Execute upgrade with timeout guard
```

### Cleanup Pattern
```
1. Delete release via helm uninstall
2. Delete namespace if configured
3. Apply RBAC cleanup (Litmus) or manual cleanup
```

## Safety Considerations

- Always cleanup in `finally` blocks
- Use `--timeout` flag on Helm operations
- Verify pods come back up after upgrade
- Record timing metrics for performance tracking