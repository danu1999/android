#!/bin/bash
echo "=== Email User Premium Aktif ==="
psql -d posbah -c "SELECT email, \"displayName\", \"isPremium\", \"isActive\" FROM local_users WHERE \"isActive\" = TRUE AND email != '' ORDER BY \"isPremium\" DESC, \"registeredAt\" ASC;" 2>/dev/null || \
sudo -u postgres psql -d posbah -c "SELECT email, \"displayName\", \"isPremium\", \"isActive\" FROM local_users WHERE \"isActive\" = TRUE AND email != '' ORDER BY \"isPremium\" DESC, \"registeredAt\" ASC;"
