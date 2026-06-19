PREPARE test_stmt(text) AS SELECT * FROM products WHERE id = $1;
