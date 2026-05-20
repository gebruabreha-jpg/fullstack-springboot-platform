#!/bin/bash

set -e

echo "Deleting KWOK cluster..."
kwokctl delete cluster --name=kwok 2>/dev/null || kwokctl delete cluster

echo "✔ Cleanup done"