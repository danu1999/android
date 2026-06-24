import re

def parse_backup():
    path = "database_exports/posbah_db_backup_realtime_migration.sql"
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        content = f.read()

    print("Parsing posbah_db_backup_realtime_migration.sql...")
    
    # We want to see copy lines or insert lines that contain 'ten_premium_hanafiariful_gmail_com'
    lines = content.splitlines()
    
    current_table = None
    table_data = {}
    
    for line in lines:
        if line.startswith("COPY "):
            # Extract table name: COPY public.tablename (cols) FROM stdin;
            m = re.match(r"COPY public\.(\w+) ", line)
            if m:
                current_table = m.group(1)
                table_data[current_table] = []
        elif line == "\\.":
            current_table = None
        elif current_table:
            if "ten_premium_hanafiariful_gmail_com" in line or "hanafiariful@gmail.com" in line:
                table_data[current_table].append(line)
                
    for table, rows in table_data.items():
        if rows:
            print(f"Table '{table}' in backup has {len(rows)} row(s) for Hanafi:")
            for r in rows[:3]:
                print("  ", r[:120])
            if len(rows) > 3:
                print(f"   ... and {len(rows)-3} more rows.")

if __name__ == "__main__":
    parse_backup()
