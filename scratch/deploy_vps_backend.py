import subprocess

def deploy_vps():
    ssh_cmd = [
        "ssh", "-i", r"C:\Users\danus\Documents\muizz.pem",
        "-o", "StrictHostKeyChecking=no",
        "muizz9900@zedmz.cloud",
        "bash"
    ]

    vps_script = """
    if [ -d "/home/muizz9900/android" ]; then
        cd /home/muizz9900/android
        git fetch origin main
        git reset --hard origin/main
        cd backend
        go build -o /home/muizz9900/posbah-backend .
        chmod +x /home/muizz9900/posbah-backend
        sudo systemctl restart posbah-backend
        sleep 2
        sudo systemctl status posbah-backend --no-pager
    fi
    """

    res = subprocess.run(ssh_cmd, input=vps_script, capture_output=True, text=True)
    print("Code:", res.returncode)
    print("Stdout:\n", res.stdout)
    print("Stderr:\n", res.stderr)

if __name__ == "__main__":
    deploy_vps()
