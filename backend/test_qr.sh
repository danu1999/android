#!/bin/bash
echo "=== Test 1: qr-session ==="
SESSION_JSON=$(curl -s http://localhost:3000/api/auth/qr-session)
echo "$SESSION_JSON"

SESSION=$(echo "$SESSION_JSON" | grep -o '"sess_[^"]*"' | tr -d '"')
echo "Session ID: $SESSION"

echo ""
echo "=== Test 2: qr-check dummy (harus 'expired') ==="
curl -s 'http://localhost:3000/api/auth/qr-check?sessionId=dummy123'
echo ""

echo ""
echo "=== Test 3: full flow qr-confirm + qr-check ==="
curl -s -X POST http://localhost:3000/api/auth/qr-confirm \
  -H "Content-Type: application/json" \
  -d "{\"sessionId\":\"$SESSION\",\"user\":{\"id\":\"test123\",\"email\":\"owner@posbah.com\",\"name\":\"Owner Test\",\"role\":\"OWNER\",\"tenantId\":\"bahteramulyap_gmail_com\",\"isDemo\":false,\"businessMode\":\"BMP\"}}"
echo ""

echo ""
echo "=== Test 4: poll setelah konfirmasi (harus 'authorized') ==="
curl -s "http://localhost:3000/api/auth/qr-check?sessionId=$SESSION"
echo ""
echo "=== DONE ==="
