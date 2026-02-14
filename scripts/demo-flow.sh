#!/usr/bin/env bash
set -euo pipefail

# Minimal end-to-end demo via the API Gateway.
# Prereq: stack running (docker compose up --build)

GATEWAY_URL="${GATEWAY_URL:-http://localhost:8079}"
CUSTOMER_EMAIL="${CUSTOMER_EMAIL:-alice@example.com}"
CUSTOMER_PASS="${CUSTOMER_PASS:-pass1234}"
DRIVER_EMAIL="${DRIVER_EMAIL:-driver@example.com}"
DRIVER_PASS="${DRIVER_PASS:-pass1234}"

PY_BIN=""
# Prefer python3 on macOS ("python" may be a shim that triggers xcode-select install prompts)
if command -v python3 >/dev/null 2>&1; then
  PY_BIN="python3"
elif command -v python >/dev/null 2>&1; then
  PY_BIN="python"
else
  echo "Error: python3 is required for this demo script (used only for JSON parsing)." >&2
  echo "Install it (or adjust the script to use jq) and re-run." >&2
  exit 1
fi

get_json_field() {
  local field="$1"
  "${PY_BIN}" -c 'import sys,json
obj=json.load(sys.stdin)
field=sys.argv[1]
if field not in obj:
  raise SystemExit(f"missing field: {field} (response={obj})")
print(obj[field])' "$field"
}

echo "Gateway: ${GATEWAY_URL}"

# Make curl calls fail fast and avoid hanging forever
CURL=(curl -sS --retry 3 --retry-delay 1 --connect-timeout 5 --max-time 20)

echo "1) Register customer + driver (idempotent)"
${CURL[@]} -X POST "${GATEWAY_URL}/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Alice\",\"email\":\"${CUSTOMER_EMAIL}\",\"password\":\"${CUSTOMER_PASS}\",\"roles\":\"CUSTOMER\"}" \
  | cat

echo
${CURL[@]} -X POST "${GATEWAY_URL}/auth/register" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Driver Dan\",\"email\":\"${DRIVER_EMAIL}\",\"password\":\"${DRIVER_PASS}\",\"roles\":\"DRIVER\"}" \
  | cat

echo

echo "2) Login as customer (get JWT)"
TOKEN=$(${CURL[@]} -X POST "${GATEWAY_URL}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${CUSTOMER_EMAIL}\",\"password\":\"${CUSTOMER_PASS}\"}" \
  | get_json_field accessToken
)

echo "Got token (length=${#TOKEN})"
AUTH_HEADER=( -H "Authorization: Bearer ${TOKEN}" )

echo "3) Create a restaurant"
RESTAURANT_ID=$(${CURL[@]} -X POST "${GATEWAY_URL}/restaurants" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Mario\u2019s Pizza","cuisine":"ITALIAN"}' \
  | get_json_field id
)

echo "restaurantId=${RESTAURANT_ID}"

echo "4) Create menu items"
MENU_1_ID=$(${CURL[@]} -X POST "${GATEWAY_URL}/restaurants/${RESTAURANT_ID}/menu" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Margherita Pizza","price":10.0,"cuisine":"ITALIAN"}' \
  | get_json_field id
)

echo "menuItemId1=${MENU_1_ID}"

MENU_2_ID=$(${CURL[@]} -X POST "${GATEWAY_URL}/restaurants/${RESTAURANT_ID}/menu" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Pasta","price":12.0,"cuisine":"ITALIAN"}' \
  | get_json_field id
)

echo "menuItemId2=${MENU_2_ID}"

echo "5) Create an order with items"
ORDER_ID=$(${CURL[@]} -X POST "${GATEWAY_URL}/orders" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":1,\"restaurantId\":${RESTAURANT_ID},\"items\":[{\"menuItemId\":${MENU_1_ID},\"quantity\":1},{\"menuItemId\":${MENU_2_ID},\"quantity\":1}]}" \
  | get_json_field id
)

echo "orderId=${ORDER_ID}"

echo "6) Pay with exact amount (and assign driverId=2)"
${CURL[@]} -X POST "${GATEWAY_URL}/orders/${ORDER_ID}/pay" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"amount":22.0,"driverId":2}' | cat

echo

echo "7) Delivery lifecycle"
DELIVERY_ID=$(${CURL[@]} "${GATEWAY_URL}/deliveries/by-order/${ORDER_ID}" \
  "${AUTH_HEADER[@]}" \
  | get_json_field id
)

echo "deliveryId=${DELIVERY_ID}"

${CURL[@]} -X PATCH "${GATEWAY_URL}/deliveries/${DELIVERY_ID}/status" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"status":"PICKED_UP"}' | cat

echo
${CURL[@]} -X PATCH "${GATEWAY_URL}/deliveries/${DELIVERY_ID}/status" \
  "${AUTH_HEADER[@]}" \
  -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}' | cat

echo

echo "8) Fetch order (includes delivery status)"
${CURL[@]} "${GATEWAY_URL}/orders/${ORDER_ID}" \
  "${AUTH_HEADER[@]}" | cat

echo

echo "Done."
