#!/usr/bin/env bash
set -euo pipefail

# Lightweight helper for local Kafka verification.
# - Lists topics
# - Starts two consumers (order placed + delivery status changed)
#
# Usage:
#   ./scripts/kafka-sanity.sh
#
# Stop:
#   Ctrl+C

BOOTSTRAP_SERVER=${BOOTSTRAP_SERVER:-kafka:9092}

echo "== Topics (bootstrap: ${BOOTSTRAP_SERVER}) =="
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP_SERVER}" --list

echo
echo "== Starting consumers (Ctrl+C to stop) =="
echo "- order.placed.v1"
echo "- delivery.status.changed.v1"
echo

# Run consumers in background and wait.
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --topic order.placed.v1 \
  --from-beginning \
  --property print.timestamp=true &
PID1=$!

docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --topic delivery.status.changed.v1 \
  --from-beginning \
  --property print.timestamp=true &
PID2=$!

cleanup() {
  kill ${PID1} ${PID2} 2>/dev/null || true
}
trap cleanup EXIT

wait

