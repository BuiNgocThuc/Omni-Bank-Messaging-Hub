# 📋 API Specification — Sell Foreign Currency (SF) System

> **Version:** 1.0.0
> **Base URL:** `http://localhost:8081`
> **Cập nhật:** 11/05/2026 — Defined từ confirmed architecture flow

---

## 📌 OVERVIEW

| Service | Port | Base Path |
|---------|------|-----------|
| SF Service (entry point) | 8081 | `/api/v1/fx` |
| Core Service (internal) | 8083 | `/api/v1/core` |
| Treasury Service (internal) | 8085 | `/api/v1/treasury` |

> **Lưu ý:** Treasury Service và Core Service là **internal APIs** — chỉ được gọi bởi SF Processor, không expose ra ngoài.

---

## 🔷 API 1: Execute FX Transaction — Initiate Sell Foreign

### 1.1. API Specification

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/fx/exchange` |
| **Caller** | Client (Mobile App) |
| **Auth** | Bearer Token (JWT) |
| **Idempotency** | `idempotency_key` trong Request Body (UUID v4, client generate) |
| **Content-Type** | `application/json` |
| **Response Type** | `application/json` |
| **Behavior** | Async — trả về ngay (202) sau khi validate và push vào Queue |

### 1.2. Request

#### Headers

| Header | Required | Format | Mô tả |
|--------|----------|--------|-------|
| `Authorization` | ✅ Required | `Bearer {jwt_token}` | JWT token xác thực client |
| `Content-Type` | ✅ Required | `application/json` | — |

#### Request Body

```json
{
  "idempotency_key": "550e8400-e29b-41d4-a716-446655440000",
  "owner_id": 882291,
  "account_number_id": "0900231231",
  "base_currency": "USD",
  "target_currency": "VND",
  "amount": 500.00
}
```

#### Request Fields

| Field | Type | Required | Mô tả |
|-------|------|----------|-------|
| `idempotency_key` | `String (UUID v4)` | ✅ | Client tự generate. SF Service dùng để check duplicate request. Phải là UUID v4 hợp lệ. |
| `owner_id` | `Long` | ✅ | ID số của chủ tài khoản |
| `account_number_id` | `String` | ✅ | Số tài khoản nguồn (chứa ngoại tệ cần bán) |
| `base_currency` | `String` | ✅ | Tiền tệ đang bán (ngoại tệ). Enum: `USD`, `EUR`, `JPY`, `GBP`, `SGD` |
| `target_currency` | `String` | ✅ | Tiền tệ nhận về. Bắt buộc phải là `VND` |
| `amount` | `BigDecimal` | ✅ | Số lượng ngoại tệ muốn bán |

### 1.3. Validate Rules

#### Body Validation

| Field | Rule | Error Code | HTTP |
|-------|------|-----------|------|
| `idempotency_key` | NotBlank | `MISSING_IDEMPOTENCY_KEY` | 400 |
| `idempotency_key` | Đúng định dạng UUID v4 | `INVALID_IDEMPOTENCY_KEY_FORMAT` | 400 |
| `idempotency_key` | **Đã tồn tại** trong system (duplicate request) | `DUPLICATE_REQUEST` | 409 |
| `owner_id` | NotNull, > 0 | `MISSING_FIELD` | 400 |
| `account_number_id` | NotBlank | `MISSING_FIELD` | 400 |
| `base_currency` | NotBlank, valid enum | `INVALID_CURRENCY` | 400 |
| `target_currency` | NotBlank, valid enum | `INVALID_CURRENCY` | 400 |
| `amount` | NotNull | `MISSING_FIELD` | 400 |
| `amount` | > 0 (Positive) | `AMOUNT_NOT_POSITIVE` | 400 |
| `amount` | >= 0.01 (min) | `AMOUNT_TOO_SMALL` | 400 |
| `amount` | <= 1,000,000 (max) | `AMOUNT_EXCEEDS_LIMIT` | 400 |
| `amount` | Scale <= 2 chữ số thập phân | `AMOUNT_INVALID_SCALE` | 400 |
| `base_currency != target_currency` | Không được cùng loại tiền | `SAME_CURRENCY` | 400 |
| `target_currency == VND` | target bắt buộc phải là VND | `UNSUPPORTED_TARGET_CURRENCY` | 400 |
| `base_currency` in supported list | Chỉ hỗ trợ: USD, EUR, JPY, GBP, SGD | `UNSUPPORTED_CURRENCY_PAIR` | 400 |

### 1.4. Business Rules

| Rule # | Rule | Mô tả |
|--------|------|-------|
| BR-001 | **Idempotency Check** | SF Service đọc `idempotency_key` từ request body. Nếu key đã tồn tại trong idempotency store → reject 409. KHÔNG tạo record mới. |
| BR-002 | **Supported Currency Pair** | `base_currency` phải thuộc: `USD`, `EUR`, `JPY`, `GBP`, `SGD`. `target_currency` bắt buộc là `VND`. |
| BR-003 | **Async Processing** | SF Service KHÔNG xử lý business logic. Validate xong → push message vào Queue → trả 202 Accepted ngay lập tức. |
| BR-004 | **No DB Write** | SF Service KHÔNG tạo record trong DB. Chỉ check idempotency store để reject duplicate. |
| BR-005 | **Message Content** | Message đẩy vào Queue phải chứa đủ: `idempotency_key`, `tx_id` (gen bởi SF Service), `owner_id`, `account_number_id`, `base_currency`, `target_currency`, `amount`, `timestamp`. |
| BR-006 | **tx_id generation** | SF Service gen `tx_id` dạng `FX-{UUID}` để trả về client tracking. `tx_id` này được đưa vào message Queue — SF Processor dùng để tạo TRANSACTION record. |

### 1.5. Response

#### ✅ Success — 202 Accepted

```json
{
  "timestamp": "2026-05-11T10:00:00Z",
  "status": "PROCESSING",
  "data": {
    "tx_id": "FX-UUID-12345",
    "message": "Transaction is being processed"
  }
}
```

#### ❌ Error — 400 Bad Request (Validation)

```json
{
  "success": false,
  "code": "INVALID_CURRENCY",
  "message": "base_currency 'XYZ' is not supported",
  "timestamp": "2026-05-11T10:00:00Z",
  "path": "/api/v1/fx/exchange"
}
```

#### ❌ Error — 409 Conflict (Duplicate)

```json
{
  "success": false,
  "code": "DUPLICATE_REQUEST",
  "message": "This request has already been submitted",
  "timestamp": "2026-05-11T10:00:00Z",
  "path": "/api/v1/fx/exchange"
}
```

### 1.6. HTTP Status Codes

| HTTP Code | Scenario | Error Code |
|-----------|---------|------------|
| `202 Accepted` | Request hợp lệ, đã push vào Queue | `SUCCESS` |
| `400 Bad Request` | Field thiếu hoặc null | `MISSING_FIELD` |
| `400 Bad Request` | `idempotency_key` không đúng UUID format | `INVALID_IDEMPOTENCY_KEY_FORMAT` |
| `400 Bad Request` | `idempotency_key` bị thiếu | `MISSING_IDEMPOTENCY_KEY` |
| `400 Bad Request` | Currency không hợp lệ | `INVALID_CURRENCY` |
| `400 Bad Request` | Currency pair không được hỗ trợ | `UNSUPPORTED_CURRENCY_PAIR` |
| `400 Bad Request` | target_currency không phải VND | `UNSUPPORTED_TARGET_CURRENCY` |
| `400 Bad Request` | base_currency == target_currency | `SAME_CURRENCY` |
| `400 Bad Request` | amount vi phạm constraint | `AMOUNT_TOO_SMALL` / `AMOUNT_EXCEEDS_LIMIT` / `AMOUNT_INVALID_SCALE` / `AMOUNT_NOT_POSITIVE` |
| `400 Bad Request` | Request body malformed JSON | `INVALID_REQUEST_BODY` |
| `401 Unauthorized` | JWT token thiếu hoặc không hợp lệ | `UNAUTHORIZED` |
| `409 Conflict` | `idempotency_key` đã tồn tại (duplicate) | `DUPLICATE_REQUEST` |
| `503 Service Unavailable` | RabbitMQ không available | `BROKER_UNAVAILABLE` |
| `500 Internal Server Error` | Lỗi không lường trước | `INTERNAL_ERROR` |

### 1.7. Exception Failure Matrix

| Exception | Nguyên nhân | HTTP | Code | Xử lý |
|-----------|------------|------|------|-------|
| `MethodArgumentNotValidException` | Jakarta Bean Validation fail | 400 | `VALIDATION_FAILED` | GlobalExceptionHandler |
| `HttpMessageNotReadableException` | JSON malformed, enum invalid | 400 | `INVALID_REQUEST_BODY` | GlobalExceptionHandler |
| `BusinessException(MISSING_IDEMPOTENCY_KEY)` | `idempotency_key` null/blank trong body | 400 | `MISSING_IDEMPOTENCY_KEY` | GlobalExceptionHandler |
| `BusinessException(INVALID_IDEMPOTENCY_KEY_FORMAT)` | Không đúng UUID v4 format | 400 | `INVALID_IDEMPOTENCY_KEY_FORMAT` | GlobalExceptionHandler |
| `BusinessException(DUPLICATE_REQUEST)` | `idempotency_key` đã tồn tại | 409 | `DUPLICATE_REQUEST` | GlobalExceptionHandler |
| `BusinessException(INVALID_CURRENCY)` | base/target currency không hợp lệ | 400 | `INVALID_CURRENCY` | GlobalExceptionHandler |
| `BusinessException(UNSUPPORTED_CURRENCY_PAIR)` | base_currency không thuộc whitelist | 400 | `UNSUPPORTED_CURRENCY_PAIR` | GlobalExceptionHandler |
| `BusinessException(UNSUPPORTED_TARGET_CURRENCY)` | target_currency không phải VND | 400 | `UNSUPPORTED_TARGET_CURRENCY` | GlobalExceptionHandler |
| `BusinessException(SAME_CURRENCY)` | base == target | 400 | `SAME_CURRENCY` | GlobalExceptionHandler |
| `BusinessException(AMOUNT_TOO_SMALL)` | amount < 0.01 | 400 | `AMOUNT_TOO_SMALL` | GlobalExceptionHandler |
| `BusinessException(AMOUNT_EXCEEDS_LIMIT)` | amount > 1,000,000 | 400 | `AMOUNT_EXCEEDS_LIMIT` | GlobalExceptionHandler |
| `BusinessException(AMOUNT_INVALID_SCALE)` | amount có > 2 chữ số thập phân | 400 | `AMOUNT_INVALID_SCALE` | GlobalExceptionHandler |
| `AmqpException` | RabbitMQ unavailable | 503 | `BROKER_UNAVAILABLE` | Try-catch trong Service |
| `Exception` (catch-all) | Unexpected error | 500 | `INTERNAL_ERROR` | GlobalExceptionHandler |


---

## 🔷 API 2: Get Transaction Status (Notifications / Poll)

### 2.1. API Specification

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/fx/{tx_id}` |
| **Caller** | Client (Mobile App) |
| **Auth** | Bearer Token (JWT) |
| **Description** | Lấy trạng thái và kết quả giao dịch FX theo `tx_id` |

### 2.2. Request

#### Headers

| Header | Required | Format | Mô tả |
|--------|----------|--------|-------|
| `Authorization` | ✅ Required | `Bearer {jwt_token}` | JWT token |

#### Path Variable

| Parameter | Location | Type | Required | Mô tả |
|-----------|----------|------|----------|-------|
| `tx_id` | Path Variable | `String` | ✅ | Transaction ID được trả về từ API 1 (dạng `FX-UUID-12345`) |

### 2.3. Validate Rules

| Rule | Mô tả | Error Code | HTTP |
|------|-------|-----------|------|
| `tx_id` not blank | Path variable bắt buộc | `MISSING_FIELD` | 400 |
| `tx_id` exists | tx_id phải tồn tại trong hệ thống | `TRANSACTION_NOT_FOUND` | 404 |
| `owner_id` matches | Client chỉ được xem transaction của chính mình | `FORBIDDEN` | 403 |

### 2.4. Business Rules

| Rule # | Rule | Mô tả |
|--------|------|-------|
| BR-S001 | **Ownership check** | Lấy `owner_id` từ JWT claim, so sánh với `owner_id` trong transaction. Không khớp → 403. |
| BR-S002 | **Status mapping** | Trả về đúng status hiện tại: `PROCESSING`, `SUCCESS`, `FAILED`. |
| BR-S003 | **Data completeness** | Khi status = `SUCCESS`: bắt buộc có `rate_exchange`, `converted_amount`, `completed_at`. Khi status = `FAILED`: có `failure_reason`. |

### 2.5. Response

#### ✅ Success — 200 OK (status: SUCCESS)

```json
{
  "tx_id": "FX-UUID-12345",
  "status": "SUCCESS",
  "base_currency": "USD",
  "source_amount": 500.00,
  "rate_exchange": 25450.0,
  "target_currency": "VND",
  "converted_amount": 12725000.00,
  "created_at": "2026-05-11T10:00:00Z",
  "completed_at": "2026-05-11T10:00:05Z"
}
```

#### ⏳ In Progress — 200 OK (status: PROCESSING)

```json
{
  "tx_id": "FX-UUID-12345",
  "status": "PROCESSING",
  "base_currency": "USD",
  "source_amount": 500.00,
  "target_currency": "VND",
  "created_at": "2026-05-11T10:00:00Z",
  "completed_at": null
}
```

#### ❌ Failed — 200 OK (status: FAILED)

```json
{
  "tx_id": "FX-UUID-12345",
  "status": "FAILED",
  "base_currency": "USD",
  "source_amount": 500.00,
  "target_currency": "VND",
  "failure_reason": "INSUFFICIENT_BALANCE",
  "created_at": "2026-05-11T10:00:00Z",
  "completed_at": "2026-05-11T10:00:03Z"
}
```

#### 📐 Response Fields

| Field | Type | Required | Mô tả |
|-------|------|----------|-------|
| `tx_id` | `String (UUID)` | ✅ Always | Mã giao dịch hệ thống — do SF Service generate theo format `FX-{UUID}` |
| `status` | `String (Enum)` | ✅ Always | Trạng thái giao dịch hiện tại: `PROCESSING` \| `SUCCESS` \| `FAILED` |
| `base_currency` | `String` | ✅ Always | Tiền tệ đang bán (ngoại tệ nguồn). Enum: `USD`, `EUR`, `JPY`, `GBP`, `SGD` |
| `source_amount` | `BigDecimal` | ✅ Always | Số lượng ngoại tệ đã bán (từ request ban đầu) |
| `rate_exchange` | `BigDecimal` | ⚠️ Khi `SUCCESS` | Tỷ giá áp dụng tại thời điểm giao dịch (từ Treasury Service) |
| `target_currency` | `String` | ✅ Always | Tiền tệ nhận về. Luôn là `VND` |
| `converted_amount` | `BigDecimal` | ⚠️ Khi `SUCCESS` | Số tiền VND nhận được = `source_amount × rate_exchange` |
| `failure_reason` | `String` | ⚠️ Khi `FAILED` | Lý do thất bại: `INSUFFICIENT_BALANCE` \| `TREASURY_TIMEOUT` \| `CORE_ERROR` |
| `created_at` | `String (ISO 8601)` | ✅ Always | Thời điểm tạo giao dịch (UTC) |
| `completed_at` | `String (ISO 8601)` | ⚠️ Nullable | Thời điểm hoàn tất giao dịch (UTC). `null` khi status = `PROCESSING` |

> **Quy tắc completeness (BR-S003):**
> - `status = SUCCESS` → bắt buộc có: `rate_exchange`, `converted_amount`, `completed_at`
> - `status = FAILED` → bắt buộc có: `failure_reason`, `completed_at`
> - `status = PROCESSING` → `rate_exchange`, `converted_amount`, `failure_reason` đều `null`; `completed_at = null`

### 2.6. HTTP Status Codes

| HTTP Code | Scenario | Error Code |
|-----------|---------|------------|
| `200 OK` | tx_id tồn tại, trả về status hiện tại | `SUCCESS` |
| `400 Bad Request` | tx_id blank | `MISSING_FIELD` |
| `401 Unauthorized` | JWT thiếu/invalid | `UNAUTHORIZED` |
| `403 Forbidden` | tx_id không thuộc owner này | `FORBIDDEN` |
| `404 Not Found` | tx_id không tồn tại | `TRANSACTION_NOT_FOUND` |
| `500 Internal Server Error` | Unexpected error | `INTERNAL_ERROR` |

### 2.7. Exception Failure Matrix

| Exception | Nguyên nhân | HTTP | Code |
|-----------|------------|------|------|
| `BusinessException(TRANSACTION_NOT_FOUND)` | tx_id không có trong DB | 404 | `TRANSACTION_NOT_FOUND` |
| `BusinessException(FORBIDDEN)` | owner_id từ JWT không khớp transaction | 403 | `FORBIDDEN` |
| `Exception` (catch-all) | Unexpected error | 500 | `INTERNAL_ERROR` |

---

## 🔷 API 3: Treasury — Get Rate Exchange (Internal)

### 3.1. API Specification

| Field | Value |
|-------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/treasury/rate` |
| **Caller** | SF Processor Service (internal only) |
| **Auth** | Internal service token / mTLS |

### 3.2. Request

| Parameter | Location | Type | Required | Mô tả |
|-----------|----------|------|----------|-------|
| `base` | Query Param | `String` | ✅ | Tiền tệ nguồn (e.g. `USD`) |
| `target` | Query Param | `String` | ✅ | Tiền tệ đích (e.g. `VND`) |
| `amount` | Query Param | `BigDecimal` | ✅ | Số lượng cần convert |

### 3.3. Business Rules

| Rule | Mô tả |
|------|-------|
| BR-T001 | Treasury chỉ gọi FxRatesAPI để lấy rate. Không gọi service khác. |
| BR-T002 | Assumption: tất cả currency pairs đều hợp lệ và không có hạn mức. |
| BR-T003 | Rate được snapshot tại thời điểm gọi — không cache. |
| BR-T004 | Timeout: 5 giây. Quá thời gian → throw exception về Processor. |

### 3.4. Response — 200 OK

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "base": "USD",
    "target": "VND",
    "rateExchange": 25480.0,
    "amount": 1000.00,
    "convertedAmount": 25480000.00,
    "timestamp": "2026-05-11T17:00:01"
  }
}
```

### 3.5. HTTP Status Codes

| HTTP Code | Scenario | Code |
|-----------|---------|------|
| `200 OK` | Lấy rate thành công | `SUCCESS` |
| `400 Bad Request` | Currency không hợp lệ | `INVALID_CURRENCY` |
| `503 Service Unavailable` | FxRatesAPI timeout hoặc fail | `EXTERNAL_API_UNAVAILABLE` |
| `500 Internal Server Error` | Lỗi không lường trước | `INTERNAL_ERROR` |

---

## 🔷 API 4: Core Service — Hold and Entry (Internal)

### 4.1. API Specification

| Field | Value |
|-------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/core/hold-and-entry` |
| **Caller** | SF Processor Service (internal only, via OpenFeign) |
| **Auth** | Internal service token / mTLS |

### 4.2. Request Body

```json
{
  "txId": "TXN-20260511-A1B2C3D4",
  "accountNumberId": "ACC-USD-001",
  "ownerId": "USER-001",
  "currency": "USD",
  "amount": 1000.00,
  "rateExchange": 25480.0
}
```

| Field | Type | Required | Mô tả |
|-------|------|----------|-------|
| `txId` | `String` | ✅ | Transaction ID — dùng cho idempotency check |
| `accountNumberId` | `String` | ✅ | Tài khoản cần hold |
| `ownerId` | `String` | ✅ | Owner của tài khoản |
| `currency` | `String` | ✅ | Tiền tệ cần hold |
| `amount` | `BigDecimal` | ✅ | Số tiền cần hold |
| `rateExchange` | `BigDecimal` | ✅ | Tỷ giá áp dụng — từ Treasury |

### 4.3. Validate Rules

| Rule | Mô tả | Error Code | HTTP |
|------|-------|-----------|------|
| `txId` not blank | Bắt buộc | `MISSING_FIELD` | 400 |
| `accountNumberId` exists | Tài khoản phải tồn tại | `ACCOUNT_NOT_FOUND` | 404 |
| `currency` matches account | Currency của account phải khớp với `currency` request | `CURRENCY_MISMATCH` | 400 |
| `amount > 0` | Số tiền phải dương | `AMOUNT_NOT_POSITIVE` | 400 |
| `available_balance >= amount` | Số dư khả dụng phải đủ để hold | `INSUFFICIENT_BALANCE` | 422 |
| `txId` not duplicate | Check idempotency — nếu txId đã xử lý → skip | `DUPLICATE_TRANSACTION` | 409 |

### 4.4. Business Rules

| Rule # | Rule | Mô tả |
|--------|------|-------|
| BR-C001 | **Idempotency** | Core Service check `txId` trong HOLD table. Nếu đã tồn tại → return success (idempotent, không tạo lại). |
| BR-C002 | **Balance Check** | `available_balance >= amount`. Nếu không đủ → throw `INSUFFICIENT_BALANCE`, không thực hiện hold. |
| BR-C003 | **Atomic Operation** | HOLD + ENTRY + Update ACCOUNT phải trong 1 DB transaction (`@Transactional`). Nếu bất kỳ bước nào fail → rollback toàn bộ. |
| BR-C004 | **Account Update** | Khi hold: `available_balance = available_balance - amount`, `held_balance = held_balance + amount`. |
| BR-C005 | **Entry Type** | ENTRY record có `type = DEBIT` (ghi nhật ký tiền đi ra khỏi available). |
| BR-C006 | **HOLD Status** | HOLD record được tạo với `status = ACTIVE`. |

### 4.5. Response — 200 OK (Success)

```json
{
  "success": true,
  "code": "SUCCESS",
  "data": {
    "holdId": "HOLD-20260511-X9Y8Z7",
    "txId": "TXN-20260511-A1B2C3D4",
    "accountNumberId": "ACC-USD-001",
    "heldAmount": 1000.00,
    "currency": "USD",
    "holdStatus": "ACTIVE",
    "entryId": "ENTRY-20260511-P1Q2R3",
    "createdAt": "2026-05-11T17:00:03"
  }
}
```

### 4.6. HTTP Status Codes

| HTTP Code | Scenario | Code |
|-----------|---------|------|
| `200 OK` | Hold + Entry thành công | `SUCCESS` |
| `400 Bad Request` | Field validation fail | `MISSING_FIELD` / `AMOUNT_NOT_POSITIVE` / `CURRENCY_MISMATCH` |
| `404 Not Found` | Account không tồn tại | `ACCOUNT_NOT_FOUND` |
| `409 Conflict` | txId đã được xử lý (idempotent) | `DUPLICATE_TRANSACTION` |
| `422 Unprocessable Entity` | Số dư không đủ | `INSUFFICIENT_BALANCE` |
| `500 Internal Server Error` | Lỗi DB hoặc unexpected | `INTERNAL_ERROR` |

### 4.7. Exception Failure Matrix — Core Service

| Exception | Nguyên nhân | HTTP | Code |
|-----------|------------|------|------|
| `BusinessException(ACCOUNT_NOT_FOUND)` | accountNumberId không có trong DB | 404 | `ACCOUNT_NOT_FOUND` |
| `BusinessException(CURRENCY_MISMATCH)` | Currency request không khớp account | 400 | `CURRENCY_MISMATCH` |
| `BusinessException(INSUFFICIENT_BALANCE)` | available_balance < amount | 422 | `INSUFFICIENT_BALANCE` |
| `BusinessException(DUPLICATE_TRANSACTION)` | txId đã tồn tại trong HOLD table | 409 | `DUPLICATE_TRANSACTION` |
| `DataIntegrityViolationException` | DB constraint violation | 500 | `INTERNAL_ERROR` |
| `TransactionSystemException` | DB transaction rollback | 500 | `INTERNAL_ERROR` |
| `Exception` (catch-all) | Unexpected | 500 | `INTERNAL_ERROR` |

---

## 🔷 Common: Error Response Format

Tất cả error response đều dùng chung format `ApiResponse`:

```json
{
  "success": false,
  "code": "ERROR_CODE",
  "message": "Human readable error message",
  "data": null,
  "timestamp": "2026-05-11T17:00:00",
  "path": "/api/v1/sell-foreign"
}
```

---

## 🔷 Transaction Status Definitions

| Status | Mô tả | Owner |
|--------|-------|-------|
| `PENDING` | SF Service đã tiếp nhận, chưa vào Processor | SF Processor DB |
| `PROCESSING` | SF Processor đang xử lý (đang gọi Treasury/Core) | SF Processor DB |
| `COMPLETED` | Core Service hold + entry thành công | SF Processor DB |
| `FAILED` | Xử lý thất bại (balance không đủ / technical error) | SF Processor DB |

---

## 🔷 Supported Currency Pairs

| `base_currency` | `target_currency` | Được hỗ trợ |
|----------------|-------------------|-------------|
| `USD` | `VND` | ✅ |
| `EUR` | `VND` | ✅ |
| `JPY` | `VND` | ✅ |
| `GBP` | `VND` | ✅ |
| `SGD` | `VND` | ✅ |
| Bất kỳ cặp khác | — | ❌ `UNSUPPORTED_CURRENCY_PAIR` |

---

## 🔷 Amount Constraints

| Constraint | Value | Error Code |
|------------|-------|-----------|
| Minimum | `0.01` | `AMOUNT_TOO_SMALL` |
| Maximum | `1,000,000.00` | `AMOUNT_EXCEEDS_LIMIT` |
| Decimal places | Max `2` chữ số sau dấu phẩy | `AMOUNT_INVALID_SCALE` |
| Type | `BigDecimal` (không dùng `double`/`float`) | — |

---

## 🔷 Các lỗi cần fix trong codebase hiện tại

| # | File | Vấn đề hiện tại | Cần sửa |
|---|------|-----------------|---------|
| 1 | `PaymentController.java` | Endpoint `/api/v1/payments` sai | Đổi thành `/api/v1/fx/exchange` |
| 2 | `PaymentController.java` | `ResponseEntity.ok()` → luôn trả 200 | Đổi thành `ResponseEntity.accepted()` (202) |
| 3 | `PaymentRequest.java` | Fields sai: `fromAccount`, `toAccount`, `sourceCurrency`, `targetCurrency` | Đổi thành `account_number_id`, `base_currency`, `target_currency`, thêm `owner_id`, `idempotency_key` |
| 4 | `PaymentService.java` | Gen `txId` kiểu `TXN-...` sai format | Format mới: `FX-{UUID}` |
| 5 | `PaymentService.java` | KHÔNG check idempotency key từ body | Thêm logic đọc `idempotency_key` từ request body và check duplicate |
| 6 | `AccountLedgerService.java` | `debit()` chỉ check `balance > 0` | Fix: check `available_balance >= amount` |
| 7 | `AccountLedgerService.java` | Không có HOLD + ENTRY logic | Tạo mới: Hold record + Entry record + Update ACCOUNT atomic |
| 8 | `TransactionStatus.java` | Có `PROCESSED`, `REFUNDED`, `UNKNOWN` (không dùng) | Giữ lại: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| 9 | `GlobalExceptionHandler.java` | Thiếu handler cho `MethodArgumentNotValidException` | Thêm handler trả về field-level validation errors |
| 10 | `PaymentService.java` | `sendAuditLog()` dùng tiếng Việt `"exchange thanh cong"` | Đổi sang English |

---

> **⚠️ IMPORTANT:** Document này là nguồn sự thật duy nhất (Single Source of Truth) cho API contract của SF System. Mọi implementation phải tuân theo spec này.
