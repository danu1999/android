#!/bin/bash
echo "=== Customers per tenant ==="
sudo -u postgres psql posbah -c "SELECT \"tenantId\", COUNT(*) FROM customers GROUP BY \"tenantId\";"

echo ""
echo "=== print_settings per tenant ==="
sudo -u postgres psql posbah -c "SELECT \"tenantId\", COUNT(*) FROM print_settings GROUP BY \"tenantId\";"

echo ""
echo "=== activity_logs per tenant ==="
sudo -u postgres psql posbah -c "SELECT \"tenantId\", COUNT(*) FROM activity_logs GROUP BY \"tenantId\";"

echo ""
echo "=== transaction_items ==="
sudo -u postgres psql posbah -c "SELECT COUNT(*) FROM transaction_items;"

echo ""
echo "=== products per tenant ==="
sudo -u postgres psql posbah -c "SELECT \"tenantId\", COUNT(*) FROM products WHERE NOT \"isDeleted\" GROUP BY \"tenantId\";"

echo ""
echo "=== Cek endpoint /api/rt/customers ==="
curl -s -H 'Authorization: Bearer hanafiariful@gmail.com' https://www.zedmz.cloud/api/rt/customers | head -c 200

echo ""
echo "=== Cek endpoint /api/rt/transaction-items ==="
curl -s -H 'Authorization: Bearer hanafiariful@gmail.com' 'https://www.zedmz.cloud/api/rt/transaction-items?transactionId=1' | head -c 200
