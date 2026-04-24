# Hệ thống Tra Cứu Thông Tin Địa Lý

Ứng dụng Java theo mô hình client-server, cho phép tra cứu thông tin thành phố và quốc gia qua giao diện JavaFX. Dữ liệu được tổng hợp từ nhiều nguồn bên ngoài như thời tiết, quốc gia, tin tức, khách sạn và địa danh nổi bật.

## Mục tiêu dự án

Dự án phục vụ môn Lập trình mạng, tập trung vào các nội dung:

- Giao tiếp `TCP Socket` giữa client và server
- Xử lý nhiều client đồng thời bằng `ThreadPoolExecutor`
- Mã hóa dữ liệu trao đổi bằng `RSA` và `AES`
- Tích hợp API bên ngoài để xây dựng ứng dụng thực tế
- Xây dựng giao diện desktop bằng `JavaFX`

##  Demo

[![Watch the video](https://img.youtube.com/vi/3sdEZ9LCRaM/0.jpg)](https://www.youtube.com/watch?v=3sdEZ9LCRaM)

## Chức năng chính

### 1. Tra cứu thành phố

Khi người dùng nhập tên thành phố, hệ thống có thể trả về:

- Tên thành phố và quốc gia
- Tọa độ địa lý
- Dân số
- Giờ địa phương
- Thời tiết hiện tại
- Danh sách tin tức liên quan
- Danh sách khách sạn gợi ý

### 2. Tra cứu quốc gia

Khi người dùng nhập tên quốc gia, hệ thống có thể trả về:

- Tên quốc gia
- Tọa độ
- Dân số
- Tiền tệ
- Ngôn ngữ
- Các quốc gia láng giềng
- Thời tiết tại thủ đô
- Quốc kỳ
- Tin tức liên quan
- Một số địa danh nổi bật

### 3. Giao diện người dùng

Client có hai màn hình chính:

- `Search Engine`: tìm kiếm theo từ khóa
- `Map Search`: hỗ trợ tra cứu qua giao diện bản đồ

## Kiến trúc hệ thống

### Server

Server chịu trách nhiệm:

- Nhận request từ client qua socket
- Giải mã dữ liệu nhận vào
- Phân loại request thành tra cứu thành phố, quốc gia, hoặc xem thêm chi tiết
- Gọi các service tương ứng
- Trả response JSON đã mã hóa về client

### Client

Client chịu trách nhiệm:

- Hiển thị giao diện JavaFX
- Kết nối tới server theo `ip:port`
- Tạo khóa AES theo từng request
- Mã hóa nội dung gửi đi
- Nhận và giải mã dữ liệu trả về
- Render kết quả tìm kiếm lên GUI

### Bảo mật

Luồng bảo mật hiện tại:

1. Client sinh khóa phiên `AES`
2. Client mã hóa khóa AES bằng `RSA public key`
3. Server giải mã khóa bằng `RSA private key`
4. Nội dung request/response được mã hóa bằng AES

Khóa đang được lưu trong `src/main/resources/security.properties`.

## Công nghệ sử dụng

- `Java 21`
- `Maven`
- `JavaFX 21`
- `org.json`
- `jsoup`
- `JUnit 5`

## Nguồn dữ liệu bên ngoài

Dự án hiện đang gọi tới các dịch vụ sau:

- `WeatherAPI` cho thời tiết
- `REST Countries` cho dữ liệu quốc gia
- `GeoNames` cho dữ liệu địa lý và điểm tham quan
- `Google News RSS` cho tin tức
- `Booking.com RapidAPI` cho khách sạn
- `Retool API` để publish/fetch endpoint của server

## Cấu trúc thư mục

```text
src/
├── main/
│   ├── java/geoinfo/
│   │   ├── client/
│   │   │   ├── gui/
│   │   │   └── network/
│   │   ├── common/
│   │   └── server/
│   │       ├── handler/
│   │       ├── network/
│   │       ├── processor/
│   │       ├── service/
│   │       └── utils/
│   └── resources/
│       ├── data/
│       ├── images/
│       ├── utils/
│       └── security.properties
└── test/
    └── java/geoinfo/server/service/
```

## Luồng xử lý request

1. Người dùng nhập từ khóa trên client
2. Client gửi request tới server qua socket
3. `ClientHandler` nhận và giải mã dữ liệu
4. `DataProcessor` xác định loại request
5. `CityService` hoặc `CountryService` xử lý nghiệp vụ
6. Server trả kết quả JSON về client
7. Client hiển thị kết quả lên giao diện

## Yêu cầu môi trường

- `JDK 21`
- `Maven 3.9+`
- Kết nối Internet ổn định
- Các API key còn hiệu lực

## Cách chạy dự án

### Chạy bằng IntelliJ IDEA

Đây là cách đơn giản nhất với dự án hiện tại:

1. Mở project bằng IntelliJ IDEA
2. Chạy class `geoinfo.server.Server` để khởi động server
3. Chạy class `geoinfo.client.Client` để mở giao diện client

### Chạy bằng Maven

Khởi động server:

```bash
mvn -DskipTests exec:java -Dexec.mainClass=geoinfo.server.Server
```

Khởi động client:

```bash
mvn -DskipTests javafx:run
```

## Cấu hình kết nối server

Server có thể tự publish địa chỉ đang chạy thông qua `ServerRegistryApi`.

Client sẽ:

- Ưu tiên lấy `ip:port` từ registry
- Nếu thất bại, fallback về `localhost:12345`

Bạn có thể override bằng system property:

```bash
-Dgeoinfo.server.host=127.0.0.1
-Dgeoinfo.server.port=12345
-Dgeoinfo.default.host=localhost
-Dgeoinfo.default.port=12345
```

Ngoài ra có thể đổi URL registry bằng:

```bash
-Dgeoinfo.registry.url=<url>
```

## Test

Dự án hiện có test cho `CountryService` tại:

- `src/test/java/geoinfo/server/service/CountryServiceTest.java`

Chạy test:

```bash
mvn test
```

Lưu ý: một số chức năng phụ thuộc API bên ngoài nên kết quả có thể bị ảnh hưởng bởi mạng, hạn mức API hoặc thay đổi dữ liệu từ phía nhà cung cấp.

## Hạn chế hiện tại

- Dữ liệu phụ thuộc mạnh vào dịch vụ bên ngoài
- Một số endpoint bên ngoài có thể thay đổi hoặc giới hạn request

## Hướng phát triển

- Đưa API key sang biến môi trường hoặc file cấu hình nội bộ
- Bổ sung cache cho các truy vấn tốn thời gian
- Tách DTO/model rõ ràng hơn thay vì xử lý trực tiếp bằng `JSONObject`
- Viết thêm test cho `CityService`, `HotelService`, `ClientHandler`
- Chuẩn hóa logging và xử lý lỗi mạng

## Ghi chú

Nếu chạy trên nhiều máy trong cùng mạng LAN, cần bảo đảm:

- Máy client truy cập được IP của máy server
- Cổng server không bị chặn bởi firewall
- Registry endpoint hoặc cấu hình fallback trỏ đúng địa chỉ server

## License

Dự án được thực hiện cho mục đích học tập.
