SELECT table_name, column_name
FROM information_schema.columns
WHERE table_name IN ('bmp_products', 'bmp_master_products', 'bmp_production_logs', 'bmp_product_stocks', 'bmp_stock_ledger')
  AND column_name = 'isDeleted';
