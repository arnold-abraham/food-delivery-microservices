#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# This script supports two runtimes:
#   - docker: Kafka is started via docker-compose in this repo (docker compose up)
#   - k8s:    Kafka is running in Kubernetes (default namespace: food, pod: kafka-0)
#
# Auto-detect: if docker-compose "kafka" service is running, use docker mode; otherwise k8s.
MODE="${MODE:-}"
K8S_NAMESPACE="${K8S_NAMESPACE:-food}"
K8S_KAFKA_POD="${K8S_KAFKA_POD:-kafka-0}"

docker_kafka_running() {
  docker compose ps --status running --services 2>/dev/null | grep -qx 'kafka'
}

if [[ -z "${MODE}" ]]; then
  if docker_kafka_running; then
    MODE="docker"
  else
    MODE="k8s"
  fi
fi

exec_kafka() {
  if [[ "${MODE}" == "docker" ]]; then
    docker compose exec -T kafka "$@"
  else
    kubectl -n "${K8S_NAMESPACE}" exec "${K8S_KAFKA_POD}" -- "$@"
  fi
}

# Bootstrap:
# - docker mode: host listener defaults to localhost:19092
# - k8s mode: in-cluster DNS defaults to kafka:9092 (unless overridden)
if [[ "${MODE}" == "k8s" ]]; then
  BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
else
  BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-localhost:19092}"
fi

TOPICS=(
  "order.placed.v1"
  "payment.requested.v1"
  "payment.completed.v1"
  "rider.assigned.v1"
  "delivery.status.changed.v1"
)

echo "== Ensuring topics exist (mode: ${MODE}, bootstrap: ${BOOTSTRAP}) =="
for t in "${TOPICS[@]}"; do
  exec_kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$BOOTSTRAP" \
    --create --if-not-exists \
    --topic "$t" \
    --partitions 1 \
    --replication-factor 1 >/dev/null 2>&1 || true
done

echo "== Topics =="
exec_kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --list | sort

echo
echo "== Starting consumers (Ctrl+C to stop) =="
for t in "${TOPICS[@]}"; do
  echo "- $t"
done

echo
echo "Tip: run ./scripts/demo-flow.sh in another terminal to generate events."

# Important: use a single-quoted heredoc so $1 isn't expanded by the *outer* shell (set -u would fail).
if [[ "${MODE}" == "docker" ]]; then
  docker compose exec -T kafka bash -s <<'KAFKA_SH'
else
  kubectl -n "${K8S_NAMESPACE}" exec "${K8S_KAFKA_POD}" -- bash -s <<'KAFKA_SH'
fi
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"

consumer() {
  local topic="$1"
  /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server "$BOOTSTRAP" \
    --topic "$topic" \
    --from-beginning \
    --property print.timestamp=true \
    --property print.key=true \
    --timeout-ms 600000 \
    | cat
}

consumer 'order.placed.v1' &
consumer 'payment.requested.v1' &
consumer 'payment.completed.v1' &
consumer 'rider.assigned.v1' &
consumer 'delivery.status.changed.v1' &

wait
KAFKA_SH
