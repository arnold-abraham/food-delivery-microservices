#!/usr/bin/env bash
# Bring the full stack up on EKS.
# Run from the repo root or from anywhere — it uses absolute paths.
#
# Total time: ~20-25 min (TF infra ~15 min, images ~5 min, pods ~5 min)
# Cost while running: ~$0.25/hour
# When done testing: run scripts/eks-down.sh

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REGION="${AWS_DEFAULT_REGION:-eu-central-1}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_REGISTRY="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
CLUSTER_NAME="food-delivery"

echo "================================================================"
echo " EKS bring-up  |  account=${ACCOUNT_ID}  region=${REGION}"
echo "================================================================"

# ── 1. Terraform ─────────────────────────────────────────────────────────────
echo ""
echo "==> [1/6] Terraform init + apply (~15 min)"
terraform -chdir="${ROOT}/terraform" init -upgrade -input=false
terraform -chdir="${ROOT}/terraform" apply -auto-approve -input=false

# ── 2. kubectl context ───────────────────────────────────────────────────────
echo ""
echo "==> [2/6] Configuring kubectl"
aws eks update-kubeconfig --region "${REGION}" --name "${CLUSTER_NAME}"

# ── 3. Maven build ───────────────────────────────────────────────────────────
echo ""
echo "==> [3/6] Maven build (skip tests)"
mvn -f "${ROOT}/pom.xml" -DskipTests clean package -q

# ── 4. Docker build + push to ECR ────────────────────────────────────────────
echo ""
echo "==> [4/6] Building images for linux/amd64 and pushing to ECR"
aws ecr get-login-password --region "${REGION}" | \
  docker login --username AWS --password-stdin "${ECR_REGISTRY}"

SERVICES=(api-gateway user-service restaurant-service order-service payment-service delivery-service)
for svc in "${SERVICES[@]}"; do
  echo "  -> ${svc}"
  docker build \
    --platform linux/amd64 \
    -t "${ECR_REGISTRY}/${svc}:latest" \
    "${ROOT}/${svc}/"
  docker push "${ECR_REGISTRY}/${svc}:latest"
done

# ── 5. Apply k8s manifests ───────────────────────────────────────────────────
echo ""
echo "==> [5/6] Applying Kubernetes manifests"

# Infrastructure manifests are cluster-agnostic — apply as-is
kubectl apply -f "${ROOT}/k8s/00-namespace.yaml"
kubectl apply -f "${ROOT}/k8s/01-configmap.yaml"
kubectl apply -f "${ROOT}/k8s/02-secrets.yaml"
kubectl apply -f "${ROOT}/k8s/10-postgres-statefulset.yaml"
kubectl apply -f "${ROOT}/k8s/11-kafka-statefulset.yaml"

# Service deployments: replace local image tags with ECR URIs and set pull policy
sed \
  -e "s|image: api-gateway:0\.0\.1-SNAPSHOT|image: ${ECR_REGISTRY}/api-gateway:latest|g" \
  -e "s|image: user-service:0\.0\.1-SNAPSHOT|image: ${ECR_REGISTRY}/user-service:latest|g" \
  -e "s|image: restaurant-service:0\.0\.1-SNAPSHOT|image: ${ECR_REGISTRY}/restaurant-service:latest|g" \
  -e "s|image: order-service:0\.0\.1-SNAPSHOT|image: ${ECR_REGISTRY}/order-service:latest|g" \
  -e "s|image: payment-service:0\.0\.1-SNAPSHOT|image: ${ECR_REGISTRY}/payment-service:latest|g" \
  -e "s|image: delivery-service:0\.0\.1-SNAPSHOT|image: ${ECR_REGISTRY}/delivery-service:latest|g" \
  -e "s|imagePullPolicy: IfNotPresent|imagePullPolicy: Always|g" \
  "${ROOT}/k8s/20-services-deployments.yaml" | kubectl apply -f -

# ── 6. Wait for everything to be ready ───────────────────────────────────────
echo ""
echo "==> [6/6] Waiting for pods to be ready (3-5 min)"

kubectl rollout status statefulset/postgres -n food --timeout=5m
kubectl rollout status statefulset/kafka    -n food --timeout=5m

for svc in "${SERVICES[@]}"; do
  kubectl rollout status deployment/"${svc}" -n food --timeout=5m
done

# ── Done ─────────────────────────────────────────────────────────────────────
echo ""
# Expose api-gateway publicly via AWS Load Balancer (port 80)
kubectl patch service api-gateway -n food --type=json \
  -p='[{"op":"replace","path":"/spec/type","value":"LoadBalancer"},{"op":"replace","path":"/spec/ports/0/port","value":80}]' \
  2>/dev/null || true

echo "Waiting for Load Balancer DNS..."
until kubectl get svc api-gateway -n food -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null | grep -q "elb.amazonaws.com"; do sleep 5; done
LB_URL="http://$(kubectl get svc api-gateway -n food -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"

echo "================================================================"
echo " Stack is ready!"
echo ""
echo " Public URL (usable from browser / Postman / curl):"
echo "   ${LB_URL}"
echo ""
echo " Quick test:"
echo "   curl ${LB_URL}/health"
echo ""
echo " Run end-to-end test:"
echo "   GATEWAY_URL=${LB_URL} bash scripts/demo-flow.sh"
echo ""
echo " IMPORTANT: destroy when done to stop billing:"
echo "   bash scripts/eks-down.sh"
echo "================================================================"