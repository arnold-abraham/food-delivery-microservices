#!/usr/bin/env bash
# Tear down the EKS stack completely.
# Deletes the namespace first so Kubernetes releases EBS volumes before
# Terraform tries to destroy them (avoids stuck destroys).

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "================================================================"
echo " EKS tear-down"
echo "================================================================"

# Delete k8s namespace first — this triggers PVC deletion which releases
# EBS volumes. Without this, TF destroy can hang waiting for volumes.
echo ""
echo "==> [1/2] Deleting Kubernetes namespace 'food'"
if kubectl get namespace food --request-timeout=10s &>/dev/null; then
  kubectl delete namespace food --timeout=3m
  echo "     namespace deleted"
else
  echo "     namespace not found or cluster unreachable — skipping"
fi

echo ""
echo "==> [2/2] Terraform destroy (~10 min)"
terraform -chdir="${ROOT}/terraform" destroy -auto-approve -input=false

echo ""
echo "================================================================"
echo " All AWS resources destroyed. Billing stopped."
echo "================================================================"