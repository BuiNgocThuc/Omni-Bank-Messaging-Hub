# 🏦 Omni-Bank Messaging Hub

Hệ thống chuyển tiền ngân hàng sử dụng kiến trúc **Event-Driven Microservices** với **RabbitMQ** làm Message Broker, **Oracle Database** làm cơ sở dữ liệu, và **Spring Boot 4.0.6** (Java 21).

---

## 📑 Mục Lục

- [Tổng quan kiến trúc](#-tổng-quan-kiến-trúc)
- [Tech Stack](#-tech-stack)
- [Cấu trúc Project](#-cấu-trúc-project)
- [Chi tiết từng Module](#-chi-tiết-từng-module)
- [Luồng xử lý giao dịch (Payment Flow)](#-luồng-xử-lý-giao-dịch-payment-flow)
- [RabbitMQ Topology](#-rabbitmq-topology)
- [Database Schema](#-database-schema)
- [Hướng dẫn chạy project](#-hướng-dẫn-chạy-project)
- [API Endpoints](#-api-endpoints)
- [Dữ liệu mẫu](#-dữ-liệu-mẫu)

---

## 🏗 Tổng quan kiến trúc

```
┌──────────────────┐     REST (POST)     ┌─────────────────────┐
│   Client / User  │ ──────────────────► │  Transaction Service│ (port 8085)
└──────────────────┘                     │  - Validate request │
                                         │  - Call Ledger (Feign)
                                         │  - Save Transaction │
                                         │  - Publish message  │
                                         └────────┬────────────┘
                                                  │
                                         routing: pay.convert
                                                  │
                                                  ▼
                                    ┌──────────────────────────┐
                                    │     RabbitMQ Broker       │
                                    │  Exchange: omni.banking.  │
                                    │           topic           │
                                    └──────┬───────────────┬───┘
                                           │               │
                              q.exchange.  │               │  q.transaction.
                              process      │               │  update
                                           ▼               │
                              ┌────────────────────────┐   │
                              │ Currency Exchange Svc  │   │
                              │ (port 8082)            │   │
                              │ - Gọi FxRatesAPI       │   │
                              │ - Tính convertedAmount │   │
                              └────────┬───────────────┘   │
                                       │                   │
                              routing:  │ pay.ledger        │
                                       ▼                   │
                              ┌────────────────────────┐   │
                              │ Account Ledger Service │   │
                              │ (port 8083)            │   │
                              │ - Debit sender         │   │
                              │ - Credit receiver      │   │
                              │ - Save history         │   │
                              │ - Publish result       │───┘
                              └────────────────────────┘
                                       │
                              routing: pay.transaction.update
                                       │
                                       ▼
                              ┌────────────────────────┐
                              │ Transaction Service    │
                              │ (Listener)             │
                              │ - Cập nhật status TX   │
                              │   COMPLETED / FAILED   │
                              └────────────────────────┘
```

---

## 🛠 Tech Stack

| Thành phần | Công nghệ |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Message Broker | RabbitMQ 3.13 (với Management UI) |
| Database | Oracle 23 Free (FREEPDB1) |
| ORM | Spring Data JPA + Hibernate |
| Inter-service Call | OpenFeign (Transaction → Ledger) |
| External API | FxRatesAPI (`https://api.fxratesapi.com`) |
| HTTP Client | Spring RestClient |
| Serialization | Jackson JSON |
| Build Tool | Maven (multi-module) |
| Container | Docker Compose |
| Annotation Processor | Lombok 1.18.36 |

---

## 📁 Cấu trúc Project

```
Omni-Bank-Messaging-Hub/
├── pom.xml                          # Parent POM (multi-module)
├── docker-compose.yml               # RabbitMQ + Oracle DB
├── oracle-init/
│   └── 01_create_schemas.sql        # Tạo user/schema Oracle
│
├── common/                          # Module dùng chung
│   └── src/main/java/com/example/common/
│       ├── config/api/
│       │   ├── ApiCode.java         # Mã trạng thái API
│       │   └── ApiResponse.java     # Response wrapper chuẩn
│       ├── constant/
│       │   └── RabbitMQConstants.java  # Tên exchange, queue, routing key
│       ├── dto/
│       │   ├── message/
│       │   │   ├── PaymentMessage.java # Message truyền qua RabbitMQ
│       │   │   └── AuditEvent.java     # Audit event (chuẩn bị mở rộng)
│       │   └── request/
│       │       ├── PaymentRequest.java   # Request body từ client
│       │       ├── AccountsRequest.java  # Request gửi sang Ledger
│       │       └── AccountResponseDTO.java # Response từ Ledger
│       ├── enums/
│       │   ├── Currency.java         # Enum tất cả loại tiền tệ
│       │   └── TransactionStatus.java # PENDING → EXCHANGED → COMPLETED/FAILED
│       └── exception/
│           ├── BusinessException.java      # Exception nghiệp vụ
│           └── GlobalExceptionHandler.java # Xử lý exception toàn cục
│
├── transaction-service/             # Service khởi tạo giao dịch
│   └── src/main/java/org/example/transactionservice/
│       ├── controller/
│       │   └── TransactionController.java  # POST /api/v1/transaction
│       ├── service/
│       │   ├── ITransactionService.java
│       │   └── Impl/TransactionService.java  # Logic chính
│       ├── entity/
│       │   └── Transaction.java       # Entity JPA
│       ├── repository/
│       │   └── TransactionRepository.java
│       ├── client/
│       │   └── LedgerClient.java      # OpenFeign gọi sang Ledger
│       ├── dto/
│       │   └── AccountValidateResponse.java
│       ├── config/
│       │   └── RabbitMQConfig.java    # Khai báo Exchange, Queue, Binding
│       └── listener/
│           └── TransactionUpdateListener.java  # Nhận kết quả cuối
│
├── currency-exchange-service/       # Service đổi ngoại tệ
│   └── src/main/java/com/example/currencyexchangeservice/
│       ├── listener/
│       │   └── CurrencyExchangeListener.java  # Nhận từ q.exchange.process
│       ├── service/
│       │   ├── ICurrencyExchangeService.java
│       │   └── Impl/CurrencyExchangeService.java  # Gọi API + tính toán
│       ├── client/
│       │   └── FxRatesClient.java     # Gọi FxRatesAPI external
│       ├── dto/
│       │   └── FxRatesResponse.java   # Response từ FxRatesAPI
│       └── config/
│           ├── RabbitMQConfig.java     # Khai báo tất cả queue/binding
│           └── RestClientConfig.java   # Config RestClient cho FxRatesAPI
│
└── account-ledger-service/          # Service quản lý sổ cái & số dư
    └── src/main/java/com/example/accountledgerservice/
        ├── controller/
        │   └── AccountController.java  # POST /api/ledger/accounts
        ├── service/
        │   ├── IAccountLedgerService.java
        │   └── Impl/AccountLedgerService.java  # Debit/Credit + History
        ├── entity/
        │   ├── Account.java            # Tài khoản ngân hàng
        │   └── TransactionHistory.java  # Lịch sử ghi sổ (DEBIT/CREDIT)
        ├── repository/
        │   ├── AccountRepository.java   # debit(), credit(), findAll
        │   └── TransactionHistoryRepository.java
        ├── enums/
        │   └── EntryType.java           # DEBIT, CREDIT
        ├── config/
        │   ├── RabbitMQConfig.java
        │   └── AccountDataInitializer.java  # Seed 3 tài khoản mẫu
        └── listener/
            └── AccountLedgerListener.java  # Nhận từ q.account.update
```

---

## 📦 Chi tiết từng Module

### 1. `common` — Module dùng chung

Module này **không chạy độc lập**, được các service khác dependency vào. Chứa:

| Package | Mô tả |
|---|---|
| `config.api` | `ApiResponse<T>` — wrapper response chuẩn (success, code, message, data, timestamp, path). `ApiCode` — mã lỗi/thành công. |
| `constant` | `RabbitMQConstants` — tên Exchange (`omni.banking.topic`), Queue và Routing Key. |
| `dto.message` | `PaymentMessage` — object truyền qua RabbitMQ giữa các service. `AuditEvent` — chuẩn bị cho audit trail. |
| `dto.request` | `PaymentRequest` (input từ client), `AccountsRequest`, `AccountResponseDTO`. |
| `enums` | `Currency` (160+ loại tiền), `TransactionStatus` (PENDING → EXCHANGED → COMPLETED_LEDGER / FAILED_*). |
| `exception` | `BusinessException` + `GlobalExceptionHandler` — xử lý lỗi tập trung bằng `@RestControllerAdvice`. |

### 2. `transaction-service` — Port 8085

**Vai trò:** Điểm vào (entry point) của hệ thống, nhận request chuyển tiền từ client.

**Luồng xử lý khi nhận POST `/api/v1/transaction`:**

1. **Validate request:** Kiểm tra `fromAccount ≠ toAccount`.
2. **Gọi Ledger qua OpenFeign:** `LedgerClient.getAccounts()` → lấy thông tin 2 tài khoản (sender & receiver).
3. **Validate nghiệp vụ:** Kiểm tra sender/receiver tồn tại, kiểm tra số dư đủ.
4. **Lưu Transaction** vào Oracle DB với status `PENDING`.
5. **Publish `PaymentMessage`** lên RabbitMQ với routing key `pay.convert`.
6. **Trả về `transactionId`** cho client ngay lập tức (async processing).

**Listener (`TransactionUpdateListener`):**
- Lắng nghe queue `q.transaction.update`.
- Nhận kết quả cuối cùng từ Ledger (COMPLETED_LEDGER hoặc FAILED_LEDGER / FAILED_EXCHANGE).
- Cập nhật status của Transaction trong DB.

### 3. `currency-exchange-service` — Port 8082

**Vai trò:** Xử lý đổi ngoại tệ giữa 2 loại tiền khác nhau.

**Luồng xử lý:**

1. **Listener (`CurrencyExchangeListener`)** lắng nghe queue `q.exchange.process`.
2. **Validate message:** Kiểm tra transactionId, amount > 0, currency không null, source ≠ target.
3. **Gọi API external** `FxRatesAPI` (`https://api.fxratesapi.com/latest`) qua `FxRatesClient` (RestClient) để lấy tỷ giá thực.
4. **Tính `convertedAmount`** = tỷ giá × amount.
5. **Cập nhật status** thành `EXCHANGED`.
6. **Forward message** sang queue `q.account.update` với routing key `pay.ledger`.

**Xử lý lỗi:**
- `RestClientException` (lỗi API) → **REQUEUE** (thử lại).
- `IllegalArgumentException` (dữ liệu sai) → publish `FAILED_EXCHANGE` → **DROP**.
- Exception khác → publish `FAILED_EXCHANGE` → **DROP**.

### 4. `account-ledger-service` — Port 8083

**Vai trò:** Quản lý số dư tài khoản, thực hiện ghi sổ (debit/credit), lưu lịch sử giao dịch.

**REST API (`AccountController`):**
- `POST /api/ledger/accounts` — Trả về thông tin tài khoản theo danh sách accountNumber (được gọi bởi Transaction Service qua Feign).

**Luồng xử lý message (`AccountLedgerListener`):**

1. Lắng nghe queue `q.account.update`.
2. Gọi `executeLedgerAndUpdateBalance()`:
   - **Validate** status phải là `EXCHANGED`.
   - **Debit** sender: `balance = balance - amount` (native JPQL update).
   - **Credit** receiver: `balance = balance + convertedAmount`.
   - **Lưu 2 bản ghi TransactionHistory** (1 DEBIT, 1 CREDIT).
3. **Thành công** → publish `COMPLETED_LEDGER` lên `q.transaction.update`.
4. **Thất bại** → publish `FAILED_LEDGER` lên `q.transaction.update`.

**Seed data (`AccountDataInitializer`):** Tự động tạo 3 tài khoản mẫu khi DB trống.

---

## 🔄 Luồng xử lý giao dịch (Payment Flow)

```
Người dùng gửi: POST /api/v1/transaction
{
  "fromAccount": "ACC10001",
  "toAccount": "ACC20002",
  "amount": 100
}
```

### ✅ Luồng Thành Công (Happy Path)

```
Step 1: [Transaction Service]
        │ ← Nhận POST request
        │ → Gọi Feign đến Ledger để validate 2 tài khoản
        │ → Check sender balance ≥ amount
        │ → Lưu Transaction (status = PENDING)
        │ → Publish PaymentMessage (routing: pay.convert)
        │ → Trả client: { transactionId: "TXN-A1B2C3D4" }
        ▼
Step 2: [Currency Exchange Service]
        │ ← Nhận message từ q.exchange.process
        │ → Validate payload
        │ → Gọi FxRatesAPI: USD → VND, amount=100
        │ → Nhận convertedAmount (ví dụ: 2,547,500 VND)
        │ → Set status = EXCHANGED
        │ → Forward message (routing: pay.ledger)
        │ → ACK message
        ▼
Step 3: [Account Ledger Service]
        │ ← Nhận message từ q.account.update
        │ → Debit ACC10001: -100 USD
        │ → Credit ACC20002: +2,547,500 VND
        │ → Lưu 2 bản ghi TransactionHistory
        │ → Publish COMPLETED_LEDGER (routing: pay.transaction.update)
        │ → ACK message
        ▼
Step 4: [Transaction Service - Listener]
        │ ← Nhận message từ q.transaction.update
        │ → Cập nhật Transaction status = COMPLETED_LEDGER
        │ → ACK message
        ▼
        ✅ HOÀN TẤT
```

### ❌ Luồng Thất Bại

| Lỗi tại | Xử lý |
|---|---|
| Transaction Service (validate) | Trả 400 ngay lập tức, không publish message |
| Currency Exchange (API timeout) | NACK + REQUEUE → RabbitMQ retry |
| Currency Exchange (dữ liệu sai) | Publish FAILED_EXCHANGE → Transaction cập nhật |
| Account Ledger (debit/credit fail) | Publish FAILED_LEDGER → Transaction cập nhật |

---

## 🐰 RabbitMQ Topology

### Exchange

| Tên | Loại | Durable |
|---|---|---|
| `omni.banking.topic` | Topic Exchange | ✅ |

### Queues & Bindings

| Queue | Routing Key | Consumer |
|---|---|---|
| `q.exchange.process` | `pay.convert` | Currency Exchange Service |
| `q.account.update` | `pay.ledger` | Account Ledger Service |
| `q.transaction.update` | `pay.transaction.update` | Transaction Service |

### Message Format

Tất cả message đều dùng **`PaymentMessage`** (JSON):

```json
{
  "transactionId": "TXN-A1B2C3D4",
  "fromAccount": "ACC10001",
  "toAccount": "ACC20002",
  "amount": 100,
  "sourceCurrency": "USD",
  "targetCurrency": "VND",
  "convertedAmount": 2547500.000000,
  "transactionStatus": "EXCHANGED",
  "createdAt": "2026-05-09T17:30:00",
  "failureReason": null
}
```

### ACK Mode

Tất cả service đều sử dụng **Manual ACK** (`acknowledge-mode: manual`):
- `basicAck` — xử lý thành công, xóa message khỏi queue.
- `basicNack(requeue=true)` — lỗi tạm thời (API timeout), đưa lại vào queue.
- `basicNack(requeue=false)` — lỗi nghiêm trọng, bỏ message (có publish thông báo lỗi).

---

## 💾 Database Schema

### Oracle — `FREEPDB1`

Hệ thống sử dụng 2 schema riêng biệt:

#### Schema: `LEDGER_USER` (Account Ledger Service)

**Bảng `ACCOUNT`**

| Cột | Kiểu | Mô tả |
|---|---|---|
| `ID` | NUMBER (PK, auto) | ID tự tăng |
| `ACCOUNT_NUMBER` | VARCHAR(50) | Số tài khoản |
| `OWNER_NAME` | VARCHAR(100) | Tên chủ tài khoản |
| `BALANCE` | NUMBER(19,4) | Số dư hiện tại |
| `CURRENCY` | VARCHAR(3) | Loại tiền (USD, VND, EUR...) |
| `COUNTRY_CODE` | VARCHAR(3) | Mã quốc gia |
| `UPDATED_AT` | TIMESTAMP | Lần cập nhật cuối |

**Bảng `TRANSACTION_HISTORY`**

| Cột | Kiểu | Mô tả |
|---|---|---|
| `ID` | NUMBER (PK, auto) | ID tự tăng |
| `TRANSACTION_ID` | VARCHAR(50) | Mã giao dịch (TXN-xxx) |
| `ACCOUNT_NUMBER` | VARCHAR(50) | Tài khoản liên quan |
| `AMOUNT` | NUMBER(19,4) | Số tiền (âm = DEBIT, dương = CREDIT) |
| `ENTRY_TYPE` | VARCHAR | DEBIT hoặc CREDIT |
| `DESCRIPTION` | VARCHAR(50) | Mô tả |
| `CREATED_AT` | TIMESTAMP | Thời điểm ghi sổ |

#### Schema: `TRANSACTION_USER` (Transaction Service)

**Bảng `TRANSACTION_NE`**

| Cột | Kiểu | Mô tả |
|---|---|---|
| `TRANSACTION_ID` | VARCHAR (PK) | Mã giao dịch (TXN-xxx) |
| `FROM_ACCOUNT` | VARCHAR | Tài khoản gửi |
| `TO_ACCOUNT` | VARCHAR | Tài khoản nhận |
| `AMOUNT` | NUMBER | Số tiền gốc |
| `STATUS` | VARCHAR | PENDING → EXCHANGED → COMPLETED_LEDGER / FAILED_* |
| `CREATED_AT` | TIMESTAMP | Thời điểm tạo |
| `UPDATED_AT` | TIMESTAMP | Lần cập nhật cuối |

---

## 🚀 Hướng dẫn chạy project

### Yêu cầu hệ thống

- **Java 21** (JDK)
- **Maven 3.8+**
- **Docker & Docker Compose**
- (Tuỳ chọn) IDE: IntelliJ IDEA

### Bước 1: Pull Docker Image Oracle

```bash
# Pull Oracle 23 Free image từ Oracle Container Registry
docker pull container-registry.oracle.com/database/free:latest

# Tag lại cho dễ dùng
docker tag container-registry.oracle.com/database/free:latest oracle23:latest
```

### Bước 2: Khởi động Infrastructure (RabbitMQ + Oracle)

```bash
cd Omni-Bank-Messaging-Hub

# Khởi động RabbitMQ + Oracle
docker-compose up -d
```

**Kiểm tra trạng thái:**

```bash
docker-compose ps
```

Chờ Oracle DB healthy (có thể mất 2-5 phút lần đầu):

```bash
# Xem logs Oracle
docker logs omnibank-oracle -f

# Khi thấy "DATABASE IS READY TO USE!" là OK
```

**Truy cập RabbitMQ Management UI:**
- URL: `http://localhost:15672`
- Username: `guest`
- Password: `guest`

### Bước 3: Build toàn bộ project

```bash
# Từ thư mục root
mvn clean install -DskipTests
```

> ⚠️ **Lưu ý:** Phải build `common` module trước vì các service khác phụ thuộc vào nó. Lệnh `mvn clean install` từ root sẽ tự build đúng thứ tự.

### Bước 4: Chạy từng Service

Mở **3 terminal riêng biệt** và chạy:

**Terminal 1 — Account Ledger Service (port 8083):**
```bash
cd account-ledger-service
mvn spring-boot:run
```
> Service này nên chạy **đầu tiên** vì Transaction Service cần gọi API tới nó (Feign).

**Terminal 2 — Currency Exchange Service (port 8082):**
```bash
cd currency-exchange-service
mvn spring-boot:run
```

**Terminal 3 — Transaction Service (port 8085):**
```bash
cd transaction-service
mvn spring-boot:run
```

### Bước 5: Test giao dịch

```bash
curl -X POST http://localhost:8085/api/v1/transaction \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccount": "ACC10001",
    "toAccount": "ACC20002",
    "amount": 100
  }'
```

**Response mong đợi:**

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "Payment is being processed - initiatePayment successfully!",
  "data": "TXN-A1B2C3D4",
  "timestamp": "2026-05-09T17:30:00",
  "path": "/api/v1/transaction"
}
```

### Bước 6: Theo dõi luồng xử lý

Quan sát log trên 3 terminal:

1. **Transaction Service:** `Published payment [TXN-xxx] with key=pay.convert`
2. **Currency Exchange:** `CURRENCY-SERVICE Received TX: TXN-xxx` → `Forwarded TX TXN-xxx`
3. **Account Ledger:** `[LEDGER] Processing TX: TXN-xxx` → `[LEDGER] TX TXN-xxx COMPLETED`
4. **Transaction Service:** `[TRANSACTION] TX TXN-xxx updated to COMPLETED_LEDGER`

---

## 📡 API Endpoints

### Transaction Service (port 8085)

| Method | Path | Mô tả | Body |
|---|---|---|---|
| POST | `/api/v1/transaction` | Tạo giao dịch chuyển tiền | `PaymentRequest` |

### Account Ledger Service (port 8083)

| Method | Path | Mô tả | Body |
|---|---|---|---|
| POST | `/api/ledger/accounts` | Lấy thông tin tài khoản | `AccountsRequest` |

---

## 📋 Dữ liệu mẫu

Khi `account-ledger-service` khởi động lần đầu, tự động seed 3 tài khoản:

| Account Number | Owner | Balance | Currency | Country |
|---|---|---|---|---|
| `ACC10001` | Nguyen Van A | 10,000.0000 | USD | US |
| `ACC20002` | Tran Thi B | 5,000,000.0000 | VND | VN |
| `ACC30003` | Le Van C | 5,000.0000 | EUR | UK |

**Ví dụ test case:**

```bash
# Test 1: Chuyển USD → VND (khác tiền tệ - có đổi ngoại tệ)
curl -X POST http://localhost:8085/api/v1/transaction \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC10001","toAccount":"ACC20002","amount":50}'

# Test 2: Chuyển VND → EUR
curl -X POST http://localhost:8085/api/v1/transaction \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC20002","toAccount":"ACC30003","amount":1000000}'

# Test 3: Lỗi - cùng tài khoản
curl -X POST http://localhost:8085/api/v1/transaction \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC10001","toAccount":"ACC10001","amount":100}'

# Test 4: Lỗi - không đủ tiền
curl -X POST http://localhost:8085/api/v1/transaction \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACC10001","toAccount":"ACC20002","amount":999999}'
```

---

## 🔧 Cấu hình Service

### Port mapping

| Service | Port |
|---|---|
| Transaction Service | 8085 |
| Currency Exchange Service | 8082 |
| Account Ledger Service | 8083 |
| RabbitMQ AMQP | 5672 |
| RabbitMQ Management | 15672 |
| Oracle DB | 1521 |

### Docker Compose Services

| Container | Image | Mô tả |
|---|---|---|
| `omnibank-rabbitmq` | `rabbitmq:3.13-management` | Message Broker |
| `omnibank-oracle` | `oracle23:latest` | Oracle Database 23 Free |

### Oracle Connection

| Thuộc tính | Giá trị |
|---|---|
| URL | `jdbc:oracle:thin:@//localhost:1521/FREEPDB1` |
| Ledger User | `ledger_user` / `ledger123` |
| Transaction User | `transaction_user` / `transaction123` |
| SYS Password | `Oracle123` |

---

## ⚠️ Lưu ý quan trọng

1. **Thứ tự khởi động:** Docker Compose → Account Ledger Service → Currency Exchange Service → Transaction Service.
2. **Oracle DB cần thời gian:** Lần đầu chạy Oracle container có thể mất 3-5 phút để khởi tạo. Script `01_create_schemas.sql` sẽ tự chạy khi Oracle sẵn sàng.
3. **FxRatesAPI:** Currency Exchange Service gọi API thật từ `https://api.fxratesapi.com`. Cần kết nối Internet.
4. **Manual ACK:** Tất cả consumer đều dùng manual acknowledge. Nếu service crash giữa chừng, message sẽ được requeue.
5. **Hibernate DDL Auto:** Đặt `update` — tự tạo/cập nhật bảng khi khởi động. **Không dùng cho production.**
