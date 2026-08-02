#!/bin/bash
# test-propagation.sh
#
# Pushes ONE config value through config-server and measures, in
# milliseconds, how long it takes to actually appear in the target
# service's REST-exposed config state. This is the real number: the full
# round trip of "push -> Redis write -> AES-GCM encrypt -> Kafka publish ->
# consumer poll -> decrypt -> ConcurrentHashMap put -> visible over HTTP".
#
# Usage: ./scripts/test-propagation.sh [service] [port]
#   e.g.  ./scripts/test-propagation.sh payments 8081
#         ./scripts/test-propagation.sh auth 8082
#         ./scripts/test-propagation.sh inventory 8083

SERVICE=${1:-payments}
PORT=${2:-8081}
CONFIG_SERVER="http://localhost:8084"
AUTH="admin:admin123"

KEY="propagation-test.$(date +%s)"
VALUE="probe-$RANDOM"

echo "Pushing $SERVICE/$KEY = $VALUE ..."
START_MS=$(date +%s%3N)

curl -s -u "$AUTH" -X POST "$CONFIG_SERVER/api/config/push" \
  -H "Content-Type: application/json" \
  -d "{\"service\":\"$SERVICE\",\"key\":\"$KEY\",\"value\":\"$VALUE\"}" \
  -o /dev/null

echo "Polling $SERVICE (port $PORT) until it shows up (timeout 10s) ..."

TIMEOUT_MS=10000
while true; do
  NOW_MS=$(date +%s%3N)
  ELAPSED=$((NOW_MS - START_MS))

  if [ "$ELAPSED" -gt "$TIMEOUT_MS" ]; then
    echo "TIMEOUT after ${ELAPSED}ms - value never propagated. Is the consumer running and subscribed to config.$SERVICE?"
    exit 1
  fi

  RESPONSE=$(curl -s "http://localhost:$PORT/api/$SERVICE/config")
  if echo "$RESPONSE" | grep -q "\"$KEY\":\"$VALUE\""; then
    END_MS=$(date +%s%3N)
    TOTAL=$((END_MS - START_MS))
    echo ""
    echo "Propagated in ${TOTAL}ms"
    echo "  (push -> Redis write -> AES-GCM encrypt -> Kafka publish -> consumer decrypt -> visible over HTTP)"
    exit 0
  fi

  sleep 0.1
done
