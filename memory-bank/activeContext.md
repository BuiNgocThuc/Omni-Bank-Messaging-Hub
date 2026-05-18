# Active Context

## Current Work Focus
- Refactor kiến trúc: di chuyển API GET transaction query từ `sell-foreign-service` sang `sell-foreign-processor-service`.
- Đảm bảo mỗi service có trách nhiệm đúng: SF Service = Gateway, Processor = Orchestrator + Query.

## Recent Changes
- **Thêm vào Processor Service:**
  - `controller/TransactionQueryController.java` — API GET `/api/v1/fx/transactions/{txId}`
  - `service/TransactionQueryService.java` + `impl/TransactionQueryServiceImpl.java`
  - `dto/TransactionQueryResponse.java`
- **Xoá khỏi SF Service:**
  - Endpoint GET trong `ExchangeController` (chỉ còn POST /exchange)
  - `TransactionQueryService` + `TransactionQueryServiceImpl`
  - `TransactionQueryResponse.java`
  - `TransactionDetailRepository.java`
  - `TransactionDetail.java` (entity)
- **Cập nhật README:** API spec phản ánh đúng service ownership.

## Next Steps
- Cân nhắc tách DB schema riêng cho mỗi service (hiện cùng dùng `SELL_FOREIGN_USER`).
- Test full flow: POST → MQ → Process → Notification → Client GET.
- Tiếp tục implement các business rules còn thiếu (Cut-off Time, Daily Limit).
