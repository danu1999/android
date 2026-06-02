const fs = require('fs');
const path = require('path');

const dumpPath = 'C:\\Users\\danus\\Documents\\antigravity\\railway_shuttle_proxy_rlwy_net-2026_05_31_16_27_55-dump.sql';

try {
  const content = fs.readFileSync(dumpPath, 'utf8');
  const lines = content.split('\n');
  
  let masukTotal = 0;
  let keluarTotal = 0;
  let cashFlowsCount = 0;
  
  // Look for COPY public.cash_flows or INSERT INTO public.cash_flows
  // Typically pg_dump uses COPY unless --column-inserts is specified.
  // Let's check both patterns.
  
  let inCopy = false;
  for (const line of lines) {
    if (line.startsWith('COPY public.cash_flows')) {
      inCopy = true;
      continue;
    }
    if (inCopy) {
      if (line.startsWith('\\.')) {
        inCopy = false;
        continue;
      }
      // Parse COPY format (tab separated values)
      const parts = line.split('\t');
      if (parts.length >= 7) {
        cashFlowsCount++;
        const type = parts[4]; // transaction_type
        const amount = parseFloat(parts[6]); // amount
        if (type === 'MASUK') {
          masukTotal += amount;
        } else if (type === 'KELUAR') {
          keluarTotal += amount;
        }
      }
    }
    
    // Also support INSERT format just in case
    if (line.startsWith('INSERT INTO public.cash_flows')) {
      // Parse insert statements
      // e.g. INSERT INTO public.cash_flows VALUES (..., 'MASUK', ..., 100000, ...);
      // Let's extract values
      const matches = line.match(/\(([^)]+)\)/g);
      if (matches) {
        for (const match of matches) {
          const vals = match.slice(1, -1).split(',');
          // We can parse properly if needed, but COPY is standard for pg_dump
        }
      }
    }
  }
  
  console.log('--- Results from parsing SQL dump ---');
  console.log('Total Cash Flow rows parsed:', cashFlowsCount);
  console.log('Total MASUK:', masukTotal);
  console.log('Total KELUAR:', keluarTotal);
  console.log('Net Balance:', masukTotal - keluarTotal);
  
} catch (e) {
  console.error('Error parsing dump:', e.message);
}
