#!/bin/bash
set -euo pipefail

KUBECONFIG_PATH="${KUBECONFIG:-~/.kube/config}"
JVM_OPTS="${JVM_OPTS:-"-Xmx512m -Xms256m"}"

if [ ! -f "$KUBECONFIG_PATH" ]; then
  echo "WARNING: kubeconfig not found at $KUBECONFIG_PATH"
fi

exec java $JVM_OPTS -jar /app/app.jar "$@"
