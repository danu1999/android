FROM golang:1.25-bookworm AS builder
WORKDIR /app

# Install build dependencies untuk SQLite (CGO)
RUN apt-get update && apt-get install -y git build-essential

COPY go.mod go.sum ./
RUN go mod download
COPY . .

# SQLite butuh CGO_ENABLED=1
RUN CGO_ENABLED=1 go build -o main .

FROM debian:bookworm-slim
WORKDIR /app

# Install wkhtmltopdf dan font standar di Debian
RUN apt-get update && apt-get install -y \
    wkhtmltopdf \
    ca-certificates \
    fonts-liberation \
    libssl-dev \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/main .
COPY --from=builder /app/templates ./templates
# 👇 INI DIA KUNCI JAWABANNYA: Bawa juga folder public-nya!
COPY --from=builder /app/public ./public

# Expose port (Saya sesuaikan ke 3000 karena di main.go kamu defaultnya 3000)
EXPOSE 3000

# Jalankan aplikasi
CMD ["./main"]