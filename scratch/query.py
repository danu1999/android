import subprocess

def run_query():
    ssh_cmd = [
        "ssh", "-i", "C:\\Users\\danus\\Documents\\muizz.pem",
        "-o", "StrictHostKeyChecking=no",
        "muizz9900@zedmz.cloud",
        'psql postgres://postgres:Bahtera1!@localhost:5432/posbah?sslmode=disable -c "SELECT * FROM tenants WHERE id = \'ten_premium_bahteramulyap_gmail_com\';"'
    ]
    res = subprocess.run(ssh_cmd, capture_output=True, text=True)
    print("STDOUT:")
    print(res.stdout)
    print("STDERR:")
    print(res.stderr)

if __name__ == "__main__":
    run_query()
