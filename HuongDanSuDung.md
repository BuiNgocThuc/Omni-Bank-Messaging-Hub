# Omni Bank Messaging Hub

Hệ thống mô phỏng kiến trúc microservices banking sử dụng:

- Spring Boot
- RabbitMQ
- Docker Compose
- Oracle Database
- Event-Driven Architecture

---

# Services

| Service | Port | Description |
|---|---|---|
| payment-gateway-service | 8081 | Nhận payment request và publish message |
| currency-exchange-service | 8082 | Lắng nghe queue và xử lý exchange |
| account-ledger-service | 8083 | Chưa triển khai DB logic |
| audit-log-service | 8084 | Chưa triển khai DB logic |
| RabbitMQ Management | 15672 | UI quản lý RabbitMQ |
| RabbitMQ Broker | 5672 | RabbitMQ AMQP Port |
| Oracle Database | 1521 | Oracle DB |

---

# 1. Start Infrastructure

Chạy Docker Compose:

```bash
docker compose up -d
```

Kiểm tra container:

```bash
docker ps
```

---

# 2. RabbitMQ Management UI

Truy cập:

```txt
http://localhost:15672
```

Thông tin đăng nhập:

```txt
username: guest
password: guest
```

---

# 3. Run Services

Chạy lần lượt các service:

```txt
common
payment-gateway-service
currency-exchange-service
```

---

# 4. Test Payment API

## Endpoint

```http
POST http://localhost:8081/api/v1/payments
```

## Request Body

```json
{
  "fromAccount": "ACC10001",
  "toAccount": "ACC20002",
  "amount": 3,
  "sourceCurrency": "USD",
  "targetCurrency": "VND"
}
```

---

# 5. Expected Result

Sau khi gọi API:

- `payment-gateway-service`
    - publish message vào RabbitMQ exchange

- `currency-exchange-service`
    - consume message từ queue
    - log transaction received

---

# 6. Check Logs

## payment-gateway-service

Bạn sẽ thấy log dạng:

```txt
Published payment [TXN-XXXXXXX]
```

---

## currency-exchange-service

Bạn sẽ thấy log dạng:

```txt
CURRENCY-SERVICE Received TX: TXN-XXXXXXX
```

---

# 7. Check RabbitMQ Messages

Vào:

```txt
RabbitMQ UI
→ Queues and Streams
→ q.exchange.process
```

Tại đây có thể:

- xem message
- xem Ready / Unacked count
- get message manually

---

# 8. Oracle Database

## Connection Info

```txt
Database: FREEPDB1
Username: system
Password: Oracle123
```

## Oracle Port

```txt
localhost:1521
```

---

# 9. Current Database Status

Hiện tại:

- `account-ledger-service`
- `audit-log-service`

chưa triển khai lưu dữ liệu xuống database.

Các service hiện chỉ mô phỏng:

- message publishing
- queue consuming
- event-driven flow

---

# 10. Current Architecture Flow

```txt
POST /payments
        ↓
payment-gateway-service
        ↓
RabbitMQ Exchange
        ↓
q.exchange.process
        ↓
currency-exchange-service
```

---

# 11. RabbitMQ Credentials

| Property | Value |
|---|---|
| Host | localhost |
| Port | 5672 |
| Username | guest |
| Password | guest |

---

# 12. Notes

- RabbitMQ sử dụng Topic Exchange
- Message được serialize bằng Jackson JSON
- Consumer sử dụng manual ACK/NACK
- Event-driven asynchronous processing

---

# 13. Future Enhancements

Planned:

- Account Ledger Persistence
- Audit Logging Persistence
- Oracle Integration
- Retry Queue / DLQ
- Outbox Pattern
- Idempotency
- FX Rate API Integration
- Distributed Tracing