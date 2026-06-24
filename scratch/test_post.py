import requests

url = "https://www.zedmz.cloud/api/rt/bmp/invoices"
headers = {
    "Authorization": "Bearer bahteramulyap@gmail.com"
}
body = {
    "clientId": 17,
    "number": "TEST-POST-12345",
    "status": "UNPAID",
    "totalAmount": 100000.0,
    "paidAmount": 0.0,
    "paymentTerms": "14 days",
    "notes": "Test insert",
    "createdAt": 1782237488418
}

resp = requests.post(url, json=body, headers=headers)
print(f"STATUS CODE: {resp.status_code}")
print(resp.text)
