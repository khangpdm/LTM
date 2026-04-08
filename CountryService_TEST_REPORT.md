# CountryService Test Report

## Mục tiêu
- Kiểm tra logic tìm quốc gia sau khi chuyển sang mô hình tải dữ liệu một lần và tìm local từ cache.
- Phân loại rõ các trường hợp đã xử lý tốt và các trường hợp còn giới hạn.

## Các trường hợp đã test tự động
1. Tìm đúng theo `name.common`
   Input: `china`
   Kỳ vọng: trả về China.

2. Không phân biệt hoa thường
   Input: `VIETNAM`
   Kỳ vọng: trả về Vietnam.

3. Tìm theo `name.official`
   Input: `People's Republic of China`
   Kỳ vọng: trả về China.

4. Tìm theo `altSpellings`
   Input: `PRC`
   Kỳ vọng: trả về China.

5. Fallback theo khớp một phần
   Input: `viet`
   Kỳ vọng: trả về Vietnam.

6. Dữ liệu đầu vào rỗng
   Input: `""`, `"   "`
   Kỳ vọng: trả thông báo lỗi đầu vào rỗng.

7. Không có kết quả
   Input: `atlantis`
   Kỳ vọng: trả thông báo không tìm thấy quốc gia phù hợp.

## Các trường hợp đã fix được
- Không còn gọi API nhiều lần cho mỗi lần tìm kiếm.
- Không còn bị chọn sai do lấy luôn phần tử đầu tiên từ API.
- Hỗ trợ tìm theo:
  - tên thường (`common`)
  - tên chính thức (`official`)
  - tên thay thế (`altSpellings`)
  - không phân biệt hoa thường
- Có fallback khớp một phần khi người dùng không nhập đủ tên.

## Các trường hợp chưa fix triệt để
- Từ khóa mơ hồ như `congo`, `korea`, `guinea`
  Lý do: có nhiều quốc gia hợp lệ cùng gần với từ khóa, service hiện chỉ chọn kết quả phù hợp đầu tiên trong cache.

- Từ khóa viết sai chính tả như `chian`, `vitnam`
  Lý do: hiện chưa có fuzzy matching kiểu Levenshtein.

- Tên bản dịch không nằm trong `altSpellings`
  Ví dụ một số tên tiếng Việt hoặc cách gọi địa phương hiếm có thể không khớp nếu dữ liệu nguồn không chứa.

- Cache bị cũ khi dữ liệu nguồn thay đổi
  Cách xử lý: gọi `reloadCountries()` khi muốn cập nhật.

- Lỗi mạng ở lần tải đầu tiên hoặc lúc reload
  Lý do: service vẫn cần mạng để tải bộ dữ liệu `/all` ít nhất một lần.

## Gợi ý trình bày với giảng viên
- Phần đã tối ưu:
  - từ nhiều request cho mỗi lần tìm kiếm sang một lần tải toàn bộ dữ liệu
  - giảm độ trễ cho các lần tìm sau
  - giảm phụ thuộc mạng trong quá trình tra cứu thường xuyên

- Phần còn giới hạn:
  - chưa xử lý tốt tên mơ hồ hoặc sai chính tả
  - cần reload thủ công nếu muốn đồng bộ dữ liệu mới nhất
