#!/bin/bash

set -e

INFRA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."

echo "Creating KWOK cluster..."
kwokctl create cluster

echo "Getting kubeconfig..."
KUBECONFIG_PATH=$(kwokctl kubeconfig-path)
export KUBECONFIG="$KUBECONFIG_PATH"

echo "Waiting for cluster..."
kubectl wait --for=condition=Ready node --all --timeout=60s 2>/dev/null || true

echo "Applying namespace..."
kubectl apply -f "$INFRA_DIR/namespace.yaml" 2>/dev/null || true

echo "Applying fake resources..."
kubectl apply -f "$INFRA_DIR/fake-pods.yaml"
kubectl apply -f "$INFRA_DIR/fake-deployment.yaml"

echo "Waiting for pods..."
kubectl wait pod --all -n kwok-test --for=condition=Ready --timeout=60s 2>/dev/null || true

echo "✔ Setup complete"
echo "Kubeconfig: $KUBECONFIG_PATH"