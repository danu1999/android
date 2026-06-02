const { execSync } = require('child_process');
const fs = require('fs');

const runQuery = (db, query) => {
  try {
    fs.writeFileSync('temp_query.sql', query);
    const out = execSync(`PGPASSWORD="posbah_vps_pass_2026" psql -U postgres -h localhost -d ${db} -t -f temp_query.sql`);
    fs.unlinkSync('temp_query.sql');
    return out.toString().trim();
  } catch (e) {
    return 'Error: ' + e.message;
  }
};

try {
  console.log('=== Calculating Dashboard Metrics (is_demo = false) ===');
  const totalKasIn = runQuery('bmp_db', "SELECT COALESCE(SUM(amount), 0) FROM cash_flows WHERE transaction_type = 'MASUK' AND is_demo = false AND deleted_at IS NULL;");
  const totalKasOut = runQuery('bmp_db', "SELECT COALESCE(SUM(amount), 0) FROM cash_flows WHERE transaction_type = 'KELUAR' AND is_demo = false AND deleted_at IS NULL AND description NOT LIKE '%Nono%';");
  const nonoTotalBayar = runQuery('bmp_db', "SELECT COALESCE(SUM(nominal), 0) FROM bahan_nonos WHERE is_demo = false AND deleted_at IS NULL;");
  const nonoTotalBahan = runQuery('bmp_db', "SELECT COALESCE(SUM(total_harga), 0) FROM bahan_nonos WHERE is_demo = false AND deleted_at IS NULL;");
  
  const kasIn = parseFloat(totalKasIn || 0);
  const kasOut = parseFloat(totalKasOut || 0);
  const nonoBayar = parseFloat(nonoTotalBayar || 0);
  const nonoBahan = parseFloat(nonoTotalBahan || 0);
  
  const saldoKas = kasIn - kasOut - nonoBayar;
  
  console.log('Total Kas In:', kasIn);
  console.log('Total Kas Out (no Nono):', kasOut);
  console.log('Nono Total Bayar:', nonoBayar);
  console.log('Nono Total Bahan:', nonoBahan);
  console.log('Calculated Saldo Kas Riil:', saldoKas);
} catch (e) {
  console.error(e);
}
