#!/bin/bash
# seed-config.sh
# Pushes 18 realistic-looking dummy config values (6 per service) through
# config-server, so the dashboard has something to show right after startup
# instead of being empty. Run this after start-all.sh, once all 4 services
# report healthy.

CONFIG_SERVER="http://localhost:8084"
AUTH="admin:admin123"

push() {
  local service=$1
  local key=$2
  local value=$3
  echo "Pushing $service/$key = $value"
  curl -s -u "$AUTH" -X POST "$CONFIG_SERVER/api/config/push" \
    -H "Content-Type: application/json" \
    -d "{\"service\":\"$service\",\"key\":\"$key\",\"value\":\"$value\"}" \
    -o /dev/null -w "  -> HTTP %{http_code}\n"
  sleep 0.2
}

echo "Seeding payments service ..."
push payments "db.connection.timeout.ms" "30000"
push payments "gateway.retry.max-attempts" "3"
push payments "gateway.base-url" "https://sandbox.paygateway.example.com"
push payments "transaction.currency.default" "USD"
push payments "fraud-check.enabled" "true"
push payments "payout.batch-size" "250"

echo "Seeding auth service ..."
push auth "jwt.expiry.minutes" "60"
push auth "jwt.refresh-token.expiry.days" "14"
push auth "password.min-length" "10"
push auth "login.max-failed-attempts" "5"
push auth "sso.provider.default" "internal"
push auth "mfa.enabled" "true"

echo "Seeding inventory service ..."
push inventory "stock.low-threshold" "20"
push inventory "warehouse.default-region" "us-east"
push inventory "reorder.auto-trigger.enabled" "true"
push inventory "supplier.default-lead-time-days" "7"
push inventory "cache.ttl.seconds" "120"
push inventory "sync.interval.minutes" "15"

echo ""
echo "Done. Open dashboard/index.html in a browser to see the state populate."
