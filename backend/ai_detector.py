import sys
import math
import re

# Training data
TRAINING_DATA = {
    "SAVE_SIGNATURE_VPS": [
        "simpan di vps aja ketika ada yang ttd untuk penerima invoice",
        "simpan tanda tangan di vps",
        "buat folder ttd di server",
        "jangan pakai cloudinary lagi, simpan lokal di vps",
        "taruh file ttd di folder vps",
        "simpan ttd di vps lokal",
        "simpan di vps aja",
        "buat folder TTD di vps",
        "simpan gambar ttd lokal di server"
    ],
    "SAVE_SIGNATURE_CLOUDINARY": [
        "simpan tanda tangan di cloudinary",
        "upload ttd ke cloudinary",
        "pakai cloudinary untuk ttd",
        "simpan gambar ttd ke cloud",
        "upload ke cloudinary"
    ],
    "CHANGE_DOMAIN": [
        "ubah domain web ke zedmz.cloud",
        "pakai domain www.posbah.com",
        "ganti domain server",
        "ubah settingan dns atau domain",
        "pakai https://www.zedmz.cloud"
    ]
}

def tokenize(text):
    text = text.lower()
    text = re.sub(r'[^a-z0-9\s]', '', text)
    return text.split()

def get_tf(tokens):
    tf = {}
    for token in tokens:
        tf[token] = tf.get(token, 0) + 1
    return tf

# Build vocabulary and document frequencies
vocab = set()
docs = []
doc_labels = []

for label, texts in TRAINING_DATA.items():
    for text in texts:
        tokens = tokenize(text)
        vocab.update(tokens)
        docs.append(tokens)
        doc_labels.append(label)

num_docs = len(docs)
idf = {}
for term in vocab:
    doc_count = sum(1 for doc in docs if term in doc)
    idf[term] = math.log(num_docs / (1 + doc_count))

def get_tfidf_vector(text):
    tokens = tokenize(text)
    tf = get_tf(tokens)
    vector = {}
    for term in vocab:
        if term in tf:
            vector[term] = tf[term] * idf[term]
        else:
            vector[term] = 0.0
    return vector

def cosine_similarity(v1, v2):
    dot_product = sum(v1[term] * v2[term] for term in vocab)
    magnitude_v1 = math.sqrt(sum(v1[term] ** 2 for term in vocab))
    magnitude_v2 = math.sqrt(sum(v2[term] ** 2 for term in vocab))
    if magnitude_v1 == 0 or magnitude_v2 == 0:
        return 0.0
    return dot_product / (magnitude_v1 * magnitude_v2)

def classify(input_text):
    # Check simple keyword rules first for absolute accuracy
    input_lower = input_text.lower()
    if "simpan" in input_lower and ("vps" in input_lower or "folder" in input_lower or "lokal" in input_lower or "ttd" in input_lower):
        return "SAVE_SIGNATURE_VPS", 1.0
    if "cloudinary" in input_lower:
        return "SAVE_SIGNATURE_CLOUDINARY", 1.0
    if "domain" in input_lower or "dns" in input_lower or "zedmz" in input_lower or "posbah.com" in input_lower:
        return "CHANGE_DOMAIN", 1.0

    input_vector = get_tfidf_vector(input_text)
    best_score = 0.0
    best_label = "UNKNOWN"

    for doc, label in zip(docs, doc_labels):
        doc_vector = get_tfidf_vector(" ".join(doc))
        score = cosine_similarity(input_vector, doc_vector)
        if score > best_score:
            best_score = score
            best_label = label
            
    if best_score < 0.1:
        return "UNKNOWN", best_score
        
    return best_label, best_score

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print('{"category": "UNKNOWN", "confidence": 0.0}')
        sys.exit(0)
    
    statement = sys.argv[1]
    label, score = classify(statement)
    print(f'{{"category": "{label}", "confidence": {score:.4f}}}')
