#!/bin/bash

###############################################################################
# Rate Limiting Validation Script
# Tests rate limiting activation on login endpoint
# Usage: ./test-rate-limiting.sh [host] [port] [attempts] [delay_ms]
###############################################################################

set -e

# Configuration
HOST="${1:-localhost}"
PORT="${2:-8080}"
ATTEMPTS="${3:-15}"
DELAY_MS="${4:-100}"

ENDPOINT="http://$HOST:$PORT/api/auth/login"
LOG_FILE="/tmp/rate-limit-test-$(date +%s).log"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║              RATE LIMITING VALIDATION TEST                     ║"
echo "╚════════════════════════════════════════════════════════════════╝"

echo ""
echo "Configuration:"
echo "  Endpoint: $ENDPOINT"
echo "  Attempts: $ATTEMPTS"
echo "  Delay: ${DELAY_MS}ms"
echo "  Log: $LOG_FILE"
echo ""

# Test payload
PAYLOAD=$(cat <<'EOF'
{
  "username": "test_user",
  "password": "invalid_password_for_rate_limit_test"
}
EOF
)

echo "Testing rate limiting..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

blocked_count=0
success_count=0
error_count=0

for i in $(seq 1 $ATTEMPTS); do
  echo -n "Attempt $i/$ATTEMPTS... "

  response=$(curl -s -w "\n%{http_code}" -X POST "$ENDPOINT" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD" 2>&1)

  # Split response and status code
  http_code=$(echo "$response" | tail -n1)
  body=$(echo "$response" | head -n-1)

  echo "$response" >> "$LOG_FILE"
  echo "---" >> "$LOG_FILE"

  case $http_code in
    200)
      echo "✓ 200 (Login successful - unexpected)"
      ((success_count++))
      ;;
    401)
      echo "✓ 401 (Authentication failed - expected)"
      ((success_count++))
      ;;
    429)
      echo "✓ 429 (Rate limit blocked - EXPECTED!)"
      ((blocked_count++))
      ;;
    *)
      echo "✗ $http_code (Unexpected response)"
      ((error_count++))
      ;;
  esac

  # Delay between attempts
  if [ $i -lt $ATTEMPTS ]; then
    sleep "$(echo "scale=3; $DELAY_MS / 1000" | bc)"
  fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Results:"
echo "  ✓ Successful (200/401): $success_count"
echo "  ⚠ Blocked (429): $blocked_count"
echo "  ✗ Errors: $error_count"
echo ""

if [ $blocked_count -gt 0 ]; then
  echo "✅ RATE LIMITING IS WORKING!"
  echo ""
  echo "First 429 response:"
  grep -A 20 '"status":429' "$LOG_FILE" | head -25
  exit 0
else
  echo "❌ RATE LIMITING NOT WORKING!"
  echo ""
  echo "Expected: 429 responses after reaching limit (default: 5 per 300s)"
  echo "Got: All responses were $success_count × 200/401 + $error_count × errors"
  echo ""
  echo "Debug: Check logs at $LOG_FILE"
  echo ""
  echo "Possible causes:"
  echo "  1. LOGIN_RATE_LIMIT_USERNAME_LIMIT is too high (default: 5)"
  echo "  2. LOGIN_RATE_LIMIT_USERNAME_WINDOW_SECONDS is too high (default: 300)"
  echo "  3. Redis is not connected (check REDIS_HOST, REDIS_PASSWORD)"
  echo "  4. KRONOS_REDIS_ENABLED=false"
  echo "  5. Rate limiting is disabled in this environment"
  exit 1
fi
