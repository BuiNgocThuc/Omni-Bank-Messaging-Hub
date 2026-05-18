# Progress

## What Works
- Cấu trúc Maven multi-module đã thiết lập.
- Tài liệu API và Business Flow đã cập nhật trong `README.md`.
- **SF Service (port 8088):** Nhận request, validate, check idempotency, save Transaction (PENDING), publish MQ, trả 202.
- **SF Processor (port 8082):** Consume MQ, orchestrate FX workflow (Hold → Rate → Release+Entry), update trạng thái, publish NotificationEvent, **expose API GET `/api/v1/fx/transactions/{txId}`**.
- **Notification Service:** Consume NotificationEvent, gửi thông báo.
- **Core Banking:** Hold balance, release + ledger entry.
- **Treasury:** Cung cấp tỷ giá.
- Toàn bộ code compile thành công (BUILD SUCCESS).

## What's Left to Build
- Test full flow end-to-end: POST → MQ → Process → Notification → Client GET.
- Business rules chưa implement: Cut-off Time (08:00-16:30), Daily Limit.
- Tách DB schema riêng cho mỗi service (hiện cùng dùng `SELL_FOREIGN_USER`).
- Cơ chế retry / DLQ cho MQ failures.

## Current Status
- Đã hoàn thành refactor di chuyển API GET query sang Processor Service theo góp ý mentor.

## Known Issues
- 2 service (SF Service + Processor) cùng dùng shared DB schema `SELL_FOREIGN_USER` → vi phạm Database per Service nhưng tạm chấp nhận.
