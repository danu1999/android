import requests

url = "https://www.zedmz.cloud/api/rt/bmp/invoices"
headers = {
    "Authorization": "Bearer bahteramulyap@gmail.com"
}

resp = requests.get(url, headers=headers)
print(f"STATUS CODE: {resp.status_code}")
if resp.status_code == 200:
    invoices = resp.json()
    print(f"TOTAL INVOICES RETURNED: {len(invoices)}")
    
    # Check for id = 0
    id_zero_invoices = [inv for inv in invoices if inv.get("id") == 0]
    print(f"INVOICES WITH id=0: {id_zero_invoices}")
else:
    print(resp.text)
