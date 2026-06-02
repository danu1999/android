const fs = require('fs');
const dumpPath = 'C:\\Users\\danus\\railway_shuttle_proxy_rlwy_net-2026_06_01_14_11_30-dump.sql';

try {
  const content = fs.readFileSync(dumpPath, 'utf8');
  const lines = content.split('\n');
  
  let masukTotal = 0;
  let keluarTotal = 0;
  let cashFlowsCount = 0;
  
  // Syntax: INSERT INTO public.cash_flows (id, created_at, updated_at, deleted_at, transaction_date, transaction_type, description, amount, payment_ref_id, date_created, tanggal, jenis_transaksi, keterangan, nominal, is_demo) VALUES (...)
  for (const line of lines) {
    if (line.includes('INSERT INTO public.cash_flows')) {
      // Find the VALUES (...) part
      const valStartIndex = line.indexOf('VALUES');
      if (valStartIndex !== -1) {
        const valuesPart = line.substring(valStartIndex);
        // Clean valuesPart to extract list of elements inside parentheses
        const match = valuesPart.match(/\((.*)\)/);
        if (match) {
          const valuesStr = match[1];
          // Split by comma, but be careful with strings containing commas
          // Let's use a simple CSV splitter or state parser
          const fields = [];
          let currentField = '';
          let inString = false;
          for (let i = 0; i < valuesStr.length; i++) {
            const char = valuesStr[i];
            if (char === "'") {
              inString = !inString;
            } else if (char === ',' && !inString) {
              fields.push(currentField.trim());
              currentField = '';
            } else {
              currentField += char;
            }
          }
          fields.push(currentField.trim());
          
          if (fields.length >= 8) {
            cashFlowsCount++;
            // Column names:
            // 0: id
            // 1: created_at
            // 2: updated_at
            // 3: deleted_at
            // 4: transaction_date
            // 5: transaction_type (e.g., 'MASUK' or 'KELUAR')
            // 6: description
            // 7: amount (number)
            const type = fields[5].replace(/'/g, '');
            const amount = parseFloat(fields[7]);
            
            if (type === 'MASUK') {
              masukTotal += amount;
            } else if (type === 'KELUAR') {
              keluarTotal += amount;
            } else {
              console.log('Unknown transaction type:', type, 'line:', line);
            }
          }
        }
      }
    }
  }
  
  console.log('--- Results from parsing latest SQL dump (INSERT format) ---');
  console.log('Total Cash Flow rows parsed:', cashFlowsCount);
  console.log('Total MASUK:', masukTotal);
  console.log('Total KELUAR:', keluarTotal);
  console.log('Net Balance:', masukTotal - keluarTotal);
  
} catch (e) {
  console.error(e);
}
