const express = require('express');
const { createClient } = require('@supabase/supabase-js');
const cron = require('node-cron');

const app = express();
app.use(express.json());

// Load credentials from environment variables or use fallback values
const SUPABASE_URL = process.env.SUPABASE_URL || 'https://etustetneufkfilndimy.supabase.co';
const SUPABASE_SECRET_KEY = process.env.SUPABASE_SECRET_KEY || 'YOUR_SUPABASE_SECRET_KEY';

if (!SUPABASE_URL || !SUPABASE_SECRET_KEY) {
  console.error('ERROR: Supabase URL and Secret Key are required.');
  process.exit(1);
}

const supabase = createClient(SUPABASE_URL, SUPABASE_SECRET_KEY);

/**
 * 1. Cron Job: Runs every hour to lockout demo users older than 2 days.
 * 2 days in milliseconds: 2 * 24 * 60 * 60 * 1000 = 172,800,000 ms.
 */
cron.schedule('0 * * * *', async () => {
  console.log('[Cron] Checking for expired demo accounts...');
  try {
    const twoDaysAgoMillis = Date.now() - (2 * 24 * 60 * 60 * 1000);

    // Fetch active demo users who registered more than 2 days ago
    const { data: users, error } = await supabase
      .from('local_users')
      .select('googleSub, email, registeredAt, isActive')
      .eq('isPremium', false)
      .eq('isActive', true)
      .lt('registeredAt', twoDaysAgoMillis);

    if (error) {
      console.error('[Cron] Error fetching users:', error.message);
      return;
    }

    if (!users || users.length === 0) {
      console.log('[Cron] No expired demo users found.');
      return;
    }

    console.log(`[Cron] Found ${users.length} expired demo accounts. Locking out...`);

    for (const user of users) {
      const { error: updateError } = await supabase
        .from('local_users')
        .update({ isActive: false })
        .eq('googleSub', user.googleSub);

      if (updateError) {
        console.error(`[Cron] Failed to lockout ${user.email}:`, updateError.message);
      } else {
        console.log(`[Cron] Locked out demo user: ${user.email} (Registered at: ${new Date(Number(user.registeredAt)).toLocaleString()})`);
      }
    }
  } catch (err) {
    console.error('[Cron] Unexpected error in cron job:', err.message);
  }
});

// Endpoint: Health Check
app.get('/status', (req, res) => {
  res.json({
    status: 'running',
    timestamp: new Date().toISOString(),
    database: 'connected to Supabase'
  });
});

// Endpoint: Manually Trigger Demo Check (helpful for admin/testing)
app.post('/api/admin/check-demo-lockout', async (req, res) => {
  try {
    const twoDaysAgoMillis = Date.now() - (2 * 24 * 60 * 60 * 1000);

    const { data: users, error } = await supabase
      .from('local_users')
      .select('googleSub, email, registeredAt, isActive')
      .eq('isPremium', false)
      .eq('isActive', true)
      .lt('registeredAt', twoDaysAgoMillis);

    if (error) {
      return res.status(500).json({ error: error.message });
    }

    const lockedOutUsers = [];
    for (const user of users) {
      const { error: updateError } = await supabase
        .from('local_users')
        .update({ isActive: false })
        .eq('googleSub', user.googleSub);

      if (!updateError) {
        lockedOutUsers.push(user.email);
      }
    }

    res.json({
      message: 'Demo lockout check completed manually.',
      checkedCount: users.length,
      lockedOut: lockedOutUsers
    });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`PosBah Backend Server is listening on port ${PORT}`);
});
