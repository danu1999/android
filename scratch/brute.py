import hashlib
import binascii

salt = b"posbah_default_salt_secret"
iterations = 1000
key_len = 64 # 512 bits
target_hash = "4fcdcf963504e99736d47e01937d8c7cc0bde8f6845c797eaf0d1a0589797132f41d00501ee568539589eefb2aec6c05296c60c5b53ccbed4a5b119bb54d4b1d"

def check(pw):
    h = hashlib.pbkdf2_hmac('sha512', pw.encode('utf-8'), salt, iterations, key_len)
    return binascii.hexlify(h).decode('utf-8') == target_hash

# Test common passwords
candidates = [
    "admin", "admin123", "password", "password123", "123456", "12345678", "1234",
    "posbah", "posbah123", "ramayana", "pisang", "keju", "jonio", "jonio123", "jonio9012",
    "PISANG KEJU RAMAYANA", "pisangkejuramayana"
]

for c in candidates:
    if check(c):
        print(f"FOUND: {c}")
        exit()

# Try all 4-digit and 6-digit PINs
print("Trying 4-digit pins...")
for i in range(10000):
    pin = f"{i:04d}"
    if check(pin):
        print(f"FOUND: {pin}")
        exit()

print("Trying 6-digit pins...")
for i in range(1000000):
    pin = f"{i:06d}"
    if check(pin):
        print(f"FOUND: {pin}")
        exit()

print("Not found in common candidates or digits.")
