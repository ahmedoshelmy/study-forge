#!/usr/bin/env bash
# End-to-end smoke test against a running compose stack (docker compose up -d --build --wait).
# Creates a roadmap with 3 cards, reviews 2 of them through the web proxy, then polls
# insights-service until the Kafka-fed read model shows both reviews.
set -euo pipefail

BASE="${BASE_URL:-http://localhost:${WEB_PORT:-8095}}"
LEARNER="smoke-$(date +%s)"

json() { python3 -c "import json,sys; d=json.load(sys.stdin); print($1)"; }
post() { curl -fsS -X POST -H 'Content-Type: application/json' -d "$2" "$BASE$1"; }

echo "web health:      $(curl -fsS "$BASE/healthz")"
echo "learning ready:  $(curl -fsS "$BASE/api/learners/$LEARNER/due" >/dev/null && echo ok)"
echo "insights ready:  $(curl -fsS "$BASE/api/insights/learners/$LEARNER/stats" >/dev/null && echo ok)"

roadmap=$(post /api/roadmaps "{\"learnerId\":\"$LEARNER\",\"zoneId\":\"Africa/Cairo\",\"title\":\"Kafka basics\",
  \"stages\":[{\"title\":\"Foundations\",\"items\":[{\"title\":\"Topics and partitions\",\"minutes\":20}]}]}")
roadmap_id=$(echo "$roadmap" | json 'd["id"]')
echo "roadmap:         $roadmap_id"

cards=()
for front in "What orders events in Kafka?" "What is a consumer group?" "What is a DLT?"; do
  card=$(post "/api/roadmaps/$roadmap_id/cards" "{\"front\":\"$front\",\"back\":\"See the docs\"}")
  cards+=("$(echo "$card" | json 'd["id"]')")
done
echo "cards:           ${#cards[@]}"

post "/api/cards/${cards[0]}/reviews" '{"grade":5,"durationSeconds":30}' >/dev/null
post "/api/cards/${cards[1]}/reviews" '{"grade":3,"durationSeconds":45}' >/dev/null
echo "reviews posted:  2"

for attempt in $(seq 1 30); do
  total=$(curl -fsS "$BASE/api/insights/learners/$LEARNER/stats" | json 'd["totalReviews"]')
  if [ "$total" = "2" ]; then
    echo "insights:        totalReviews=2 after $attempt poll(s)"
    echo "SMOKE PASS"
    exit 0
  fi
  sleep 2
done
echo "insights never reached totalReviews=2 (last: $total)" >&2
echo "SMOKE FAIL" >&2
exit 1
