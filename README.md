
# Omni-Bank Message Hub

**Omni-Bank Message Hub** là một dự án mini-project mô phỏng hệ thống xử lý giao dịch ngân hàng đa kênh tích hợp (Omnichannel Banking) dựa trên kiến trúc Microservices. Dự án tập trung trình diễn sức mạnh của **RabbitMQ** trong việc giải quyết các bài toán về xử lý bất đồng bộ, tính toàn vẹn dữ liệu và khả năng mở rộng hệ thống.



## Kiến trúc hệ thống

Dự án bao gồm 3 dịch vụ chính giao tiếp với nhau thông qua cơ chế Event Chaining (Chuỗi sự kiện):

1.  **Payment Gateway Service (Service A):**
    * **Vai trò:** Cổng tiếp nhận (Producer).
    * **Nhiệm vụ:** Nhận yêu cầu thanh toán từ Client qua REST API, tạo Transaction ID, dán nhãn định tuyến (Routing Key) và đẩy vào hệ thống Broker.
2.  **Currency Exchange Service (Service B):**
    * **Vai trò:** Trung gian xử lý (Consumer & Producer).
    * **Nhiệm vụ:** Lấy tin nhắn từ hàng đợi, giả lập tác vụ nặng (tính toán tỷ giá ngoại tệ với `Thread.sleep`), cập nhật dữ liệu và đẩy tiếp sang dịch vụ kế tiếp.
3.  **Account Ledger Service (Service C):**
    * **Vai trò:** Trạm cuối (Consumer).
    * **Nhiệm vụ:** Nhận kết quả cuối cùng, thực hiện cập nhật số dư tài khoản trong Database và lưu trữ lịch sử giao dịch (Transaction History).

---

## Tính năng nổi bật của Message Queue

Dự án này được thiết kế để minh họa các khái niệm cốt lõi của Message Broker:

* **Topic Exchange Power:** Sử dụng duy nhất một `Topic Exchange` để thay thế hoàn toàn cho `Direct` và `Fanout Exchange` thông qua việc cấu hình linh hoạt các `Binding Key` (`#`, `*`, chuỗi cố định).
* **Asynchronous Processing:** Gateway phản hồi ngay lập tức cho người dùng trong khi các tác vụ nặng (tính tỷ giá, cập nhật DB) được xử lý ngầm, giúp hệ thống không bị nghẽn (Non-blocking).
* **Load Leveling (Buffer):** Sử dụng cơ chế hàng đợi để điều tiết lưu lượng giao dịch, giúp các Service phía sau không bị quá tải khi có lượng lớn yêu cầu đổ về cùng lúc.
* **Fault Tolerance & Manual ACK:** Trình diễn khả năng tự phục hồi dữ liệu. Nếu một Service bị sập khi đang xử lý, tin nhắn sẽ được trả lại hàng đợi (Requeue) nhờ cơ chế xác nhận thủ công (Manual Acknowledgment).

---

## Công nghệ sử dụng

* **Ngôn ngữ:** Java 21
* **Framework:** Spring Boot 3.x, Spring AMQP
* **Message Broker:** RabbitMQ
* **Database:** Oracle (In-memory cho Service C)
* **Công cụ:** Docker (chạy RabbitMQ), Postman

---

## Cấu hình RabbitMQ (Topic Routing)

Hệ thống sử dụng Exchange: `x.banking.topic`

| Queue | Binding Key | Mô phỏng | Mục đích |
| :--- | :--- | :--- | :--- |
| `q.exchange.process` | `pay.usd.convert` | **Direct** | Xử lý chuyển đổi riêng cho đồng USD. |
| `q.account.update` | `pay.*.execute` | **Topic** | Nhận lệnh thực thi của tất cả các loại ngoại tệ. |
| `q.audit.log` | `#` | **Fanout** | Ghi log mọi sự kiện chảy qua hệ thống. |

---

## Luồng dữ liệu (Data Flow)

1.  **POST `/api/v1/payments`** -> Service A tạo Message (Status: `PENDING`).
2.  **Service A** -> Publish tới Exchange với key `pay.usd.convert`.
3.  **Service B** -> Consume, `sleep(5000)`, tính tỷ giá -> Publish message mới (Status: `PROCESSED`) với key `pay.usd.execute`.
4.  **Service C** -> Consume, Update Database -> Hoàn tất (Status: `COMPLETED`).

---

## Hướng dẫn cài đặt

1.  **Chạy RabbitMQ bằng Docker:**
    ```bashH
    docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
    ```
2.  **Clone dự án và chạy từng Service:**
    * Mở 3 terminal riêng biệt hoặc dùng IDE để chạy 3 ứng dụng Spring Boot.
3.  **Gửi yêu cầu thử nghiệm qua Postman:**
    ```json
    POST http://localhost:8080/Omni-bank/api/v1/payments
    {
      "fromAccount": "ACC-001",
      "toAccount": "ACC-002",
      "amount": 100,
      "currency": "USD"
    }
    ```
---
*Dự án được thực hiện bởi [Bùi Ngọc Thức và những người bạn]*
