#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

# Use the host listener so the script works from your laptop terminal.
# (Inside containers, services use kafka:9092 via KAFKA_BOOTSTRAP_SERVERS.)
BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-localhost:19092}"

TOPICS=(
  "order.placed.v1"
  "payment.requested.v1"
  "payment.completed.v1"
  "rider.assigned.v1"
  "delivery.status.changed.v1"
)

echo "== Ensuring topics exist (bootstrap: $BOOTSTRAP) =="
for t in "${TOPICS[@]}"; do
  docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$BOOTSTRAP" \
    --create --if-not-exists \
    --topic "$t" \
    --partitions 1 \
    --replication-factor 1 >/dev/null 2>&1 || true
done

echo "== Topics =="
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP" --list | sort

echo
echo "== Starting consumers (Ctrl+C to stop) =="
for t in "${TOPICS[@]}"; do
  echo "- $t"
done

echo
echo "Tip: run ./scripts/demo-flow.sh in another terminal to generate events."

# Important: use a single-quoted heredoc so $1 isn't expanded by the *outer* shell (set -u would fail).
docker compose exec -T kafka bash -s <<'KAFKA_SH'
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-localhost:19092}"

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
