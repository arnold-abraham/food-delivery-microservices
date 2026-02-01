#!/usr/bin/env bash
set -euo pipefail

# Minimal end-to-end demo via the API Gateway.
# Prereq: stack running (docker compose up --build)

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8079}"
CUSTOMER_EMAIL="${CUSTOMER_EMAIL:-alice@example.com}"
CUSTOMER_PASS="${CUSTOMER_PASS:-pass1234}"
DRIVER_EMAIL="${DRIVER_EMAIL:-driver@example.com}"
DRIVER_PASS="${DRIVER_PASS:-pass1234}"

json() { python - <<'PY'
import json,sys
print(json.dumps(sys.stdin.read()))
PY
}

echo "Gateway: ${GATEWAY_URL}"

echo "1) Register customer + driver"
curl -sS -X POST "${GATEWAY_URL}/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Alice\",\"email\":\"${CUSTOMER_EMAIL}\",\"password\":\"${CUSTOMER_PASS}\",\"roles\":\"CUSTOMER\"}" \
  | cat

echo
curl -sS -X POST "${GATEWAY_URL}/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Driver Dan\",\"email\":\"${DRIVER_EMAIL}\",\"password\":\"${DRIVER_PASS}\",\"roles\":\"DRIVER\"}" \
  | cat

echo

echo "2) Login as customer (get JWT)"
TOKEN=$(curl -sS -X POST "${GATEWAY_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${CUSTOMER_EMAIL}\",\"password\":\"${CUSTOMER_PASS}\"}" \
  | python -c 'import sys,json; print(json.load(sys.stdin)["accessToken"])')

echo "Got token (length=${#TOKEN})"
AUTH_HEADER=( -H "Authorization: Bearer ${TOKEN}" )

# Note: This demo assumes DBs are empty and IDs start at 1.
# If you re-run without resetting Postgres volume, IDs will differ.

echo "3) Create menu items for restaurantId=1"
curl -sS -X POST "${GATEWAY_URL}/restaurants/1/menu" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Margherita Pizza","price":10.0,"cuisine":"ITALIAN"}' | cat

echo
curl -sS -X POST "${GATEWAY_URL}/restaurants/1/menu" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Pasta","price":12.0,"cuisine":"ITALIAN"}' | cat

echo

echo "4) Create an order with items (menuItemId=1,2)"
ORDER_ID=$(curl -sS -X POST "${GATEWAY_URL}/orders" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"restaurantId":1,"items":[{"menuItemId":1,"quantity":1},{"menuItemId":2,"quantity":1}]}' \
  | python -c 'import sys,json; print(json.load(sys.stdin)["id"])')

echo "Created orderId=${ORDER_ID}"

echo "5) Pay with exact amount (and assign driverId=2)"
curl -sS -X POST "${GATEWAY_URL}/orders/${ORDER_ID}/pay" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"amount":22.0,"driverId":2}' | cat

echo

echo "6) Delivery lifecycle"
DELIVERY_ID=$(curl -sS "${GATEWAY_URL}/deliveries/by-order/${ORDER_ID}" \
  "${AUTH_HEADER[@]}" \
  | python -c 'import sys,json; print(json.load(sys.stdin)["id"])')

echo "deliveryId=${DELIVERY_ID}"

curl -sS -X PATCH "${GATEWAY_URL}/deliveries/${DELIVERY_ID}/status" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"status":"PICKED_UP"}' | cat

echo
curl -sS -X PATCH "${GATEWAY_URL}/deliveries/${DELIVERY_ID}/status" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}' | cat

echo

echo "7) Fetch order (includes delivery status)"
curl -sS "${GATEWAY_URL}/orders/${ORDER_ID}" \
  "${AUTH_HEADER[@]}" | cat

echo

echo "Done."
