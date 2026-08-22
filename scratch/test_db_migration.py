import requests

BASE_URL = 'https://www.zedmz.cloud'
headers = {
    'Authorization': 'Bearer BahteraMigrate123!',
    'Content-Type': 'application/json'
}

queries = [
    'ALTER TABLE "bmp_job_applicants" ADD COLUMN IF NOT EXISTS "testScore" INT DEFAULT 0;',
    'ALTER TABLE "bmp_job_applicants" ADD COLUMN IF NOT EXISTS "testAnswers" TEXT DEFAULT \'\';',
    'ALTER TABLE "bmp_job_applicants" ADD COLUMN IF NOT EXISTS "wageAgreed" BOOLEAN DEFAULT TRUE;'
]

for q in queries:
    res = requests.post(f'{BASE_URL}/api/admin/run-sql', headers=headers, json={'query': q})
    print(f"Query: {q} -> Status: {res.status_code}, Response: {res.text}")
