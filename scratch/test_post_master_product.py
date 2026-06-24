import requests

url = "https://www.zedmz.cloud/api/rt/bmp/master-products/53"
headers = {
    "Authorization": "Bearer bahteramulyap@gmail.com",
}

print("Deleting test product 53...")
resp = requests.delete(url, headers=headers)
print(f"STATUS CODE: {resp.status_code}")
print(resp.text)
