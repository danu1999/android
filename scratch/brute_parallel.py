import hashlib
import binascii
import multiprocessing

salt = b"posbah_default_salt_secret"
iterations = 1000
key_len = 64 # 512 bits
target_hash = "4fcdcf963504e99736d47e01937d8c7cc0bde8f6845c797eaf0d1a0589797132f41d00501ee568539589eefb2aec6c05296c60c5b53ccbed4a5b119bb54d4b1d"

def check_range(start_end):
    start, end = start_end
    for i in range(start, end):
        pin = f"{i:06d}"
        h = hashlib.pbkdf2_hmac('sha512', pin.encode('utf-8'), salt, iterations, key_len)
        if binascii.hexlify(h).decode('utf-8') == target_hash:
            return pin
    return None

def main():
    # Split 000000 to 999999 into chunks for each CPU core
    num_workers = multiprocessing.cpu_count()
    print(f"Starting parallel search with {num_workers} workers...")
    
    total = 1000000
    chunk_size = total // num_workers
    ranges = []
    for i in range(num_workers):
        start = i * chunk_size
        end = total if i == num_workers - 1 else (i + 1) * chunk_size
        ranges.append((start, end))
        
    with multiprocessing.Pool(num_workers) as pool:
        results = pool.map(check_range, ranges)
        for r in results:
            if r is not None:
                print(f"FOUND PIN: {r}")
                return
    print("FINISHED: 6-digit PIN not found.")

if __name__ == '__main__':
    main()
