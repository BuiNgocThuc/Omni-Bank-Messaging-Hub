# 🏦 Architecture Redesign — Sell Foreign Currency Flow

> **Nguồn:** Requirement từ Sếp — 10/05/2026
> **Bối cảnh:** Redesign lại kiến trúc Omni-Bank Messaging Hub theo luồng nghiệp vụ thực tế Banking — **Bán ngoại tệ (Sell Foreign Currency)**
> **Cập nhật lần cuối:** 11/05/2026 — Confirmed 100% từ sơ đồ kiến trúc thực tế

---

## 1. Bối cảnh nghiệp vụ (Business Context)

### Giả định (Assumptions)

- Khách hàng (KH) đã **đăng nhập và xác thực** hoàn tất trên Channel (Mobile App)
- Trên màn hình, KH thao tác:
  - Chọn **cặp tiền** (Currency Pair): ví dụ `USD/VND`
  - Nhập **số lượng**: ví dụ `1.000 USD`
  - Nhấn **Xác nhận bán ngoại tệ**

### Luồng tổng quan

```
Channel (Mobile App)
    → SF Service              (Validate cơ bản, check idempotency — KHÔNG tạo record)
    → SF Processor Service    (Orchestrator — tạo record, xử lý logic nghiệp vụ)
        ├→ Treasury Service   (Lấy tỷ giá — CHỈ trả rate_exchange, assumption luôn OK)
        └→ Core Service       (Hold + Entry atomic — nghiệp vụ corebank)
    → SF Service              (Nhận callback qua MQ — update status)
    → Notification Service    (Notify client: success / failed)
```

---

## 2. Kiến trúc Service (Service Map)

| #   | Service                  | Vai trò                                              | Ghi chú                                                         |
| --- | ------------------------ | ---------------------------------------------------- | --------------------------------------------------------------- |
| 1   | **SF Service**           | Đầu nhận request, validate cơ bản, check idempotency | **KHÔNG tạo record** — đọc idempotency key từ **request header** để check duplicate |
| 2   | **SF Processor Service** | **Orchestrator** — tạo record, xử lý toàn bộ flow    | Tạo `transaction` + `transaction_detail`. Gọi Treasury rồi Core |
| 3   | **Treasury Service**     | Pricing engine — trả `rate_exchange`                 | Chỉ xử lý đổi ngoại tệ. Assumption: limit/status luôn hợp lệ    |
| 4   | **Core Service**         | Thực hiện Hold + Entry (atomic)                      | Hold và Entry là 1 nghiệp vụ nguyên tử của core banking         |
| 5   | **Notification Service** | Notify client khi flow hoàn tất                      | Gọi sau khi SF Service nhận callback và update status xong      |

### Nguyên tắc thiết kế (Confirmed)

> ✅ **SF Service** = Gatekeeper — validate + check idempotency từ **request** (đọc idempotency key trong request header). **KHÔNG tạo record, KHÔNG query DB transaction**
> ✅ **SF Processor** = Orchestrator — tạo record, điều phối toàn bộ flow
> ✅ **Treasury** = Stateless pricing — CHỈ trả về `rate_exchange`, không gọi service khác
> ✅ **Core Service** = Atomic core banking — Hold + Entry là 1 nghiệp vụ duy nhất, không tách rời
> ✅ **Notification** = Cuối flow — SF Service trigger sau khi update status

---

## 3. Luồng xử lý chi tiết (Detailed Flow — Confirmed 100%)

### 3.1. Complete Sequence

```
Client
  │  (request kèm X-Idempotency-Key: {uuid} trong header)
  ▼
SF Service
  ├─ validate request (format, required fields)
  └─ Đọc idempotency key từ REQUEST HEADER → check duplicate
       ├─ key đã tồn tại (đã xử lý rồi) → ❌ reject (duplicate request)
       └─ key chưa tồn tại              → ✅ push message vào Message Queue
                                      │
                          [Message Queue — async]
                                      │
                                      ▼
                          SF Processor Service
                            │
                            ├─ Step 1: Tạo record TRANSACTION
                            │         { tx_id, request_id, owner_id }
                            │
                            ├─ Step 2: Tạo record TRANSACTION_DETAIL
                            │         { tx_detail_id, currency_based,
                            │           currency_target, amount,
                            │           status, rate_exchange = null }
                            │
                            ├─ Step 3: HTTP → Treasury Service
                            │     ├─ Chỉ xử lý đổi ngoại tệ
                            │     ├─ Assumption: limit, status tiền tệ luôn hợp lệ
                            │     └─ Trả về rate_exchange
                            │     └─ Update TRANSACTION_DETAIL.rate_exchange
                            │
                            ├─ Step 4: HTTP (OpenFeign) → Core Service
                            │       ├─ validate account
                            │       ├─ check balance
                            │       │
                            │       ├─ ✅ Đủ tiền
                            │       │   └─ Hold + Entry (1 nghiệp vụ ATOMIC)
                            │       │       ├─ ACCOUNT: cập nhật available_balance, held_balance
                            │       │       ├─ HOLD:    { hold_id, account_number_id, hold, status, created_at }
                            │       │       └─ ENTRY:   { entry_id, account_number_id, owner_id, currency, amount, type, created_at }
                            │       │
                            │       └─ ❌ Không đủ tiền
                            │           └─ Trả lỗi về SF Processor
                            │
                            │ [Message Queue — callback về SF Service]
                            ▼
                          SF Service
                            └─ Nhận callback → update status transaction
                                      │
                                      ▼
                          Notification Service   ⚠️ (chưa có trong sơ đồ gốc — cần bổ sung)
                            └─ Notify Client (success / failed)
```

### 3.2. Step-by-step bảng

| Step | Service                  | Hành động                                                                                                        | Giao tiếp               |
| ---- | ------------------------ | ---------------------------------------------------------------------------------------------------------------- | ----------------------- |
| 1    | **Client**               | Chọn cặp tiền, nhập số lượng, xác nhận                                                                           | —                       |
| 2    | **SF Service**           | Basic validate (format, required fields)                                                                         | ← REST từ Client        |
| 3    | **SF Service**           | **Đọc idempotency key từ REQUEST HEADER → check duplicate** (KHÔNG query DB transaction)                         | Từ request header       |
| 4    | **SF Service**           | Push message vào Queue                                                                                           | → **Message Queue**     |
| 5    | **SF Processor**         | Nhận message, **tạo TRANSACTION** `{tx_id, request_id, owner_id}`                                                | ← Queue                 |
| 6    | **SF Processor**         | **Tạo TRANSACTION_DETAIL** `{tx_detail_id, currency_based, currency_target, amount, status, rate_exchange=null}` | DB write                |
| 7    | **SF Processor**         | Call **Treasury Service** lấy tỷ giá                                                                             | → **REST**              |
| 8    | **Treasury Service**     | Xử lý đổi ngoại tệ, trả `rate_exchange`                                                                          | ← REST, trả rate        |
| 9    | **SF Processor**         | Nhận rate, update `transaction_detail.rate_exchange`                                                             | DB write                |
| 10   | **SF Processor**         | Call **Core Service** (OpenFeign) validate account + check balance                                               | → **REST (OpenFeign)**  |
| 11   | **Core Service**         | Check balance → **Hold + Entry atomic** nếu đủ tiền                                                              | ← REST từ Processor     |
| 12   | **Core Service**         | Nếu không đủ tiền → trả lỗi về SF Processor                                                                      | → REST response         |
| 13   | **SF Processor**         | Nhận kết quả → gửi callback qua **Message Queue** về SF Service                                                  | → **Message Queue**     |
| 14   | **SF Service**           | Nhận callback → **update status** transaction                                                                    | DB write                |
| 15   | **Notification Service** | Notify client (success / failed)                                                                                 | ← trigger từ SF Service |

---

## 4. Ownership Data — Ai sở hữu gì?

### 4.1. Service Ownership (Confirmed)

| Service                  | Tạo / Quản lý                           | KHÔNG làm                     |
| ------------------------ | --------------------------------------- | ----------------------------- |
| **SF Service**           | Đọc idempotency key từ **request header** để check duplicate | ❌ KHÔNG tạo record, ❌ KHÔNG query DB |
| **SF Processor**         | Tạo `TRANSACTION`, `TRANSACTION_DETAIL` | ❌ KHÔNG gọi client trực tiếp |
| **Treasury Service**     | Trả `rate_exchange`                     | ❌ KHÔNG gọi service khác     |
| **Core Service**         | Tạo `HOLD`, `ENTRY`, update `ACCOUNT`   | ❌ KHÔNG biết về Treasury     |
| **Notification Service** | Notify client                           | ❌ KHÔNG có business logic    |

### 4.2. Database Ownership

| Bảng                 | Owned by         | Mô tả                                                 |
| -------------------- | ---------------- | ----------------------------------------------------- |
| `TRANSACTION`        | **SF Processor** | Tạo khi nhận message từ Queue                         |
| `TRANSACTION_DETAIL` | **SF Processor** | Tạo cùng lúc với TRANSACTION, update rate sau         |
| `ACCOUNT`            | **Core Service** | Cập nhật `available_balance`, `held_balance` khi Hold |
| `HOLD`               | **Core Service** | Tạo khi hold tiền thành công                          |
| `ENTRY`              | **Core Service** | Tạo cùng lúc với HOLD (atomic)                        |

---

## 5. Quyết định kiến trúc (Architecture Decisions)

### 5.1. Communication Map

| Giao tiếp                       | Kiểu                                 | Lý do                                  |
| ------------------------------- | ------------------------------------ | -------------------------------------- |
| **Client → SF Service**         | REST (sync)                          | Entry point cần phản hồi ngay          |
| **SF Service → SF Processor**   | **Message Queue (async)**            | Decouple, buffer, fault tolerance      |
| **SF Processor → Treasury**     | REST (sync)                          | Cần rate ngay để xử lý tiếp            |
| **SF Processor → Core Service** | REST/OpenFeign (sync)                | Cần kết quả hold ngay để update status |
| **SF Processor → SF Service**   | **Message Queue (async — callback)** | Async callback update status           |
| **SF Service → Notification**   | Trigger (sau update status)          | Notify sau khi có kết quả cuối         |

### 5.2. Tại sao Treasury trước Core?

> **Treasury phải được gọi TRƯỚC Core Service** vì:
>
> - Cần `rate_exchange` để biết chính xác số VND sẽ receive
> - Core Service cần số tiền đã tính toán theo rate để tạo ENTRY đúng
> - Nếu Treasury fail → không cần call Core Service (tiết kiệm transaction)

### 5.3. Tại sao Hold + Entry là 1 nghiệp vụ atomic?

> Đây là **nghiệp vụ chuẩn của Core Banking**:
>
> - `HOLD`: Khóa số tiền (chưa trừ thực sự) — `held_balance` tăng, `available_balance` giảm
> - `ENTRY`: Ghi nhật ký kế toán cho transaction này
> - Hai thao tác này **KHÔNG tách rời** — nếu tách sẽ mất tính toàn vẹn dữ liệu kế toán

---

## 6. Database Schema (Confirmed)

### Schema: SF Processor Service DB

**Table: TRANSACTION**

```sql
TX_ID           VARCHAR2(50)    PK    -- Gen bởi SF Service (idempotency key)
REQUEST_ID      VARCHAR2(50)          -- ID request từ client
OWNER_ID        VARCHAR2(50)    NOT NULL
CREATED_AT      TIMESTAMP
```

**Table: TRANSACTION_DETAIL**

```sql
TX_DETAIL_ID    VARCHAR2(50)    PK
TX_ID           VARCHAR2(50)    FK → TRANSACTION.TX_ID
CURRENCY_BASED  VARCHAR2(3)     NOT NULL    -- Tiền bán: "USD"
CURRENCY_TARGET VARCHAR2(3)     NOT NULL    -- Tiền nhận: "VND"
AMOUNT          NUMBER(19,4)    NOT NULL    -- Số lượng bán
STATUS          VARCHAR2(20)    NOT NULL    -- PENDING / PROCESSING / COMPLETED / FAILED
RATE_EXCHANGE   NUMBER(19,6)    NULL        -- NULL lúc tạo, update sau khi có rate từ Treasury
CREATED_AT      TIMESTAMP
UPDATED_AT      TIMESTAMP
```

### Schema: Core Service DB

**Table: ACCOUNT**

```sql
ACCOUNT_NUMBER_ID   VARCHAR2(50)    PK
OWNER_ID            VARCHAR2(50)    NOT NULL
CURRENCY            VARCHAR2(3)     NOT NULL
AVAILABLE_BALANCE   NUMBER(19,4)    NOT NULL
HELD_BALANCE        NUMBER(19,4)    NOT NULL DEFAULT 0
```

**Table: HOLD**

```sql
HOLD_ID             VARCHAR2(50)    PK
ACCOUNT_NUMBER_ID   VARCHAR2(50)    FK → ACCOUNT
HOLD                NUMBER(19,4)    NOT NULL    -- Số tiền đang hold
STATUS              VARCHAR2(20)    NOT NULL    -- ACTIVE / RELEASED / EXPIRED
CREATED_AT          TIMESTAMP
```

**Table: ENTRY**

```sql
ENTRY_ID            VARCHAR2(50)    PK
ACCOUNT_NUMBER_ID   VARCHAR2(50)    FK → ACCOUNT
OWNER_ID            VARCHAR2(50)    NOT NULL
CURRENCY            VARCHAR2(3)     NOT NULL
AMOUNT              NUMBER(19,4)    NOT NULL
TYPE                VARCHAR2(10)    NOT NULL    -- DEBIT / CREDIT
CREATED_AT          TIMESTAMP
```

> **Lưu ý quan trọng:** HOLD và ENTRY được tạo trong cùng 1 DB transaction (atomic). Core Service là owner duy nhất của 3 bảng này.

---

## 7. RabbitMQ Topology

### Exchanges

| Exchange          | Type   | Mục đích               |
| ----------------- | ------ | ---------------------- |
| `x.banking.topic` | Topic  | Business event routing |
| `x.banking.audit` | Fanout | Broadcast audit events |

### Queues & Bindings

| Queue                     | Bound To          | Binding Key     | Consumer          | Chiều                     |
| ------------------------- | ----------------- | --------------- | ----------------- | ------------------------- |
| `q.sell-foreign.process`  | `x.banking.topic` | `sell.process`  | SF Processor      | SF Service → SF Processor |
| `q.sell-foreign.callback` | `x.banking.topic` | `sell.callback` | SF Service        | SF Processor → SF Service |
| `q.audit.log`             | `x.banking.audit` | (fanout)        | Audit Log Service | Mọi service → Audit       |

### Consumer Config

| Config         | Giá trị    | Áp dụng cho                   |
| -------------- | ---------- | ----------------------------- |
| ACK mode       | **Manual** | SF Processor, Audit           |
| Prefetch       | 1          | SF Processor                  |
| Prefetch       | 5          | Audit                         |
| Message format | JSON       | `JacksonJsonMessageConverter` |

---

## 8. Failure Handling

### 8.1. Transaction State Machine

```
PENDING → PROCESSING → COMPLETED
                     → FAILED
```

### 8.2. Failure Scenarios

| Scenario                             | Xử lý                                                                                   |
| ------------------------------------ | --------------------------------------------------------------------------------------- |
| Treasury timeout / fail              | Processor retry 3 lần (exponential backoff). Vẫn fail → FAILED, callback về SF Service  |
| Core Service fail (balance không đủ) | Processor nhận lỗi → FAILED, callback qua MQ về SF Service                              |
| Core Service fail (lỗi kỹ thuật)     | Processor mark FAILED. HOLD không được tạo → không cần rollback                         |
| Hold thành công nhưng Entry fail     | **Core Service tự rollback** trong cùng 1 DB transaction (atomic)                       |
| SF Processor crash giữa chừng        | Message vẫn trong Queue (manual ACK chưa gửi). RabbitMQ redeliver khi Processor restart |
| Duplicate message (redelivery)       | Processor check `tx_id` trong TRANSACTION table — nếu đã tồn tại → skip (idempotent)    |

### 8.3. Resilience Patterns

| Pattern                            | Áp dụng tại                                         | Mục đích                                     |
| ---------------------------------- | --------------------------------------------------- | -------------------------------------------- |
| **Timeout**                        | Processor → Treasury (5s), Processor → Core (10s)   | Tránh block vô hạn                           |
| **Circuit Breaker** (Resilience4j) | Processor → Treasury, Processor → Core              | Ngừng gọi khi downstream liên tục fail       |
| **Manual ACK**                     | Queue → Processor, Queue → SF Service               | Chỉ ACK khi xử lý thành công                 |
| **DLQ** (Dead Letter Queue)        | `q.sell-foreign.process`, `q.sell-foreign.callback` | Messages fail quá N lần → DLQ để investigate |

---

## 9. API Definition

### SF Service — Entry Point

```
POST /api/v1/sell-foreign
Header: X-Idempotency-Key: {tx_id}   ← client generate, SF Service dùng để check idempotency
```

**Request Body:**

```json
{
  "ownerId": "USER-001",
  "fromAccountId": "ACC-USD-001",
  "toAccountId": "ACC-VND-001",
  "currencyBased": "USD",
  "currencyTarget": "VND",
  "amount": 1000.0
}
```

**Response (ngay lập tức — sau khi push queue):**

```json
{
  "txId": "TXN-20260511-001",
  "status": "PENDING",
  "message": "Giao dịch đã được tiếp nhận, đang xử lý"
}
```

### Core Service — Hold + Entry (Internal API, gọi bởi SF Processor)

```
POST /api/v1/core/hold-and-entry
```

**Request Body:**

```json
{
  "txId": "TXN-20260511-001",
  "accountNumberId": "ACC-USD-001",
  "ownerId": "USER-001",
  "currency": "USD",
  "amount": 1000.0,
  "rateExchange": 25480.0
}
```

---

## 10. Tech Stack

| Layer            | Technology                    | Version         | Ghi chú                        |
| ---------------- | ----------------------------- | --------------- | ------------------------------ |
| Language         | Java                          | 21              |                                |
| Framework        | Spring Boot                   | 4.0.6           |                                |
| Messaging        | Spring AMQP + RabbitMQ        | 3.13-management | Docker image                   |
| Database (Prod)  | Oracle Database               | 23 Free         | Docker image `oracle23:latest` |
| Database (Dev)   | H2 In-memory                  | embedded        |                                |
| ORM              | Hibernate/JPA                 | via Spring Boot | `ddl-auto: update`             |
| HTTP Client      | Spring RestClient + OpenFeign | —               | OpenFeign cho Processor → Core |
| Build            | Maven multi-module            | parent POM      | groupId: `com.banking`         |
| Containerization | Docker Compose                | 3.8             | RabbitMQ + Oracle              |
| Code Gen         | Lombok                        | 1.18.36         |                                |
| Serialization    | Jackson JSON                  | via Spring Boot | `JacksonJsonMessageConverter`  |
| Validation       | Jakarta Bean Validation       | via Spring Boot |                                |

---

## 11. Service Ports & Infrastructure

| Service                | Port  | Ghi chú                                 |
| ---------------------- | ----- | --------------------------------------- |
| SF Service             | 8081  | Đầu nhận request từ client              |
| SF Processor Service   | 8082  | Orchestrator                            |
| Treasury Service       | 8085  | Service mới — tách từ currency-exchange |
| Core Service           | 8083  | Rename từ account-ledger-service        |
| Notification Service   | 8086  | **Mới** — chưa có trong codebase        |
| Audit Log Service      | 8084  | Giữ nguyên                              |
| RabbitMQ AMQP          | 5672  | `omnibank-rabbitmq`                     |
| RabbitMQ Management UI | 15672 | guest/guest                             |
| Oracle Database        | 1521  | `omnibank-oracle` (SYS pwd: Oracle123)  |

### External APIs

- **FxRatesAPI:** `https://api.fxratesapi.com/latest`
  - Free tier, params: `base`, `currencies`, `amount`
  - Timeout: 5 seconds
  - Rate update frequency: ~1 hour

---

## 12. Mapping với codebase hiện tại

| Service mới              | Code hiện tại                                    | Cần thay đổi                                                           |
| ------------------------ | ------------------------------------------------ | ---------------------------------------------------------------------- |
| **SF Service**           | `payment-gateway-service`                        | Rename + Sửa lại: KHÔNG INSERT record, chỉ GET để check idempotency    |
| **SF Processor**         | `currency-exchange-service` (listener part)      | Tách ra, thêm logic tạo TRANSACTION + TRANSACTION_DETAIL               |
| **Treasury Service**     | `currency-exchange-service` (FxRatesClient part) | Tách ra service riêng                                                  |
| **Core Service**         | `account-ledger-service`                         | Rename + refactor: listener → REST controller, thêm HOLD + ENTRY logic |
| **Notification Service** | ❌ Chưa có                                       | **Tạo mới**                                                            |
| **Audit Log Service**    | `audit-log-service`                              | **Giữ nguyên**                                                         |

---

## 13. Known Issues (cần fix trong quá trình redesign)

| #   | Issue                                                                                               | Vị trí                  | Severity                    |
| --- | --------------------------------------------------------------------------------------------------- | ----------------------- | --------------------------- |
| 1   | `AccountRepository.debit()` chỉ check `balance > 0`, không check `balance >= amount`                | account-ledger-service  | 🔴 Critical                 |
| 2   | `transactions.java` entity là **class rỗng**                                                        | payment-gateway-service | 🟡 Cần implement            |
| 3   | `AccountLedgerListener.sendAuditLog()` gọi `convertAndSend()` với 2 params (thiếu routing key `""`) | account-ledger-service  | 🟡 Bug                      |
| 4   | Audit event description dùng tiếng Việt informal                                                    | common/DTOs             | 🟢 Cleanup                  |
| 5   | Không có DLQ — messages bị NACK+DROP mất vĩnh viễn                                                  | RabbitMQ config         | 🟡 Cần thêm                 |
| 6   | SF Service đang INSERT record PENDING (SAI) → cần sửa thành chỉ GET check idempotency               | payment-gateway-service | 🔴 Critical — Sai nghiệp vụ |

---

## 14. Implementation Checklist

### Phase 1: Chuẩn bị (Không code nghiệp vụ)

- [ ] Update `RabbitMQConstants.java`: thêm `q.sell-foreign.process`, `q.sell-foreign.callback`
- [ ] Update `TransactionStatus.java`: PENDING / PROCESSING / COMPLETED / FAILED
- [ ] Tạo DTO mới: `SellForeignMessage`, `RateResponse`, `HoldAndEntryRequest`
- [ ] Tạo entity mới cho SF Processor: `Transaction`, `TransactionDetail`
- [ ] Tạo entity mới cho Core Service: `Hold`, `Entry`, update `Account`

### Phase 2: Rename & Restructure

- [ ] Rename `payment-gateway-service` → `sell-foreign-service`
- [ ] Rename `account-ledger-service` → `core-banking-service`
- [ ] Tạo module mới: `treasury-service`
- [ ] Tạo module mới: `sell-foreign-processor`
- [ ] Tạo module mới: `notification-service`
- [ ] Update parent `pom.xml` modules
- [ ] Update `docker-compose.yml`

### Phase 3: Implement Services

- [ ] **SF Service:** Controller + validate + GET check idempotency (**KHÔNG INSERT**) + publish queue
- [ ] **SF Processor:** Queue consumer + tạo TRANSACTION + TRANSACTION_DETAIL + call Treasury + call Core + callback qua MQ
- [ ] **Treasury Service:** REST controller + FxRatesClient + trả `rate_exchange`
- [ ] **Core Service:** REST controller (thay listener) + validate account + check balance + Hold + Entry (atomic)
- [ ] **Core Service:** Fix `debit()` bug — check `balance >= amount`
- [ ] **Notification Service:** Nhận trigger từ SF Service + notify client
- [ ] **Audit:** Mỗi service publish audit event vào `x.banking.audit`

### Phase 4: Resilience & Polish

- [ ] Timeout config: Treasury 5s, Core Service 10s
- [ ] Circuit Breaker (Resilience4j): Processor → Treasury, Processor → Core
- [ ] DLQ cho `q.sell-foreign.process` và `q.sell-foreign.callback`
- [ ] Idempotency check ở Processor (check tx_id tồn tại chưa)
- [ ] Test end-to-end: happy path + insufficient balance + timeout scenarios
- [ ] Chuẩn hóa audit event descriptions (English)

---

## 15. Tổng kết — Communication Map

```
┌────────────────────────────────────────────────────────────────────────┐
│                     CONFIRMED COMMUNICATION MAP                        │
│                                                                        │
│  Client ──REST──▶ SF Service ──MQ──▶ SF Processor                     │
│                       ▲                  │                             │
│                       │                  ├──REST──▶ Treasury Service   │
│                  MQ callback             │          (rate_exchange)    │
│                       │                  │                             │
│                       └──────────────────┤──REST(OpenFeign)──▶ Core   │
│                                          │                    Service  │
│                                          │              ┌─────────────┤│
│                                 SF Service              │ ACCOUNT      ││
│                                  update status          │ HOLD         ││
│                                      │                  │ ENTRY        ││
│                                      ▼                  └─────────────┘│
│                            Notification Service                         │
│                              └─▶ Notify Client                         │
│                                                                        │
│  MQ:          SF Service → SF Processor (process)                      │
│               SF Processor → SF Service (callback)                     │
│  REST:        Processor → Treasury (get rate)                          │
│               Processor → Core (hold + entry)                          │
│  AUDIT:       Fanout Exchange — mọi service publish → Audit consume    │
└────────────────────────────────────────────────────────────────────────┘
```

---

> **⚠️ ĐIỂM ĐÃ CONFIRMED — KHÔNG THAY ĐỔI:**
>
> 1. SF Service **KHÔNG tạo record** — đọc idempotency key từ **REQUEST HEADER** để check duplicate (KHÔNG query DB transaction)
> 2. SF Processor **tạo cả 2 bảng**: TRANSACTION + TRANSACTION_DETAIL
> 3. Treasury được gọi **TRƯỚC** Core Service
> 4. Hold + Entry là **1 nghiệp vụ atomic** của Core Banking — không tách
> 5. SF Processor callback về SF Service qua **Message Queue** (không phải REST)
> 6. Notification Service trigger **từ SF Service** sau khi update status xong

---

> **Với document này, bất kỳ AI hoặc developer nào cũng có đủ context để implement toàn bộ redesign mà không cần file nào khác.**
