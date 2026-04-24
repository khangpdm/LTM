package geoinfo.server.utils;

import java.text.Normalizer;

public class ValidationUtils {

    // Pattern for location names (countries, cities)
    private static final String LOCATION_PATTERN = "[\\p{L}\\p{M}0-9 .,'()\\-]+";
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;

    // Kiểm tra input đã cho có phải là tên vị trí hợp lệ hay không
    public static boolean isValidLocationName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        return input.matches(LOCATION_PATTERN);
    }

    // Xử lí khoảng trắng
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }

    // Kiểm tra input có rỗng không
    public static boolean isEmpty(String input) {
        return input == null || input.trim().isEmpty();
    }

    // Chuẩn hóa một chuỗi bằng cách loại bỏ dấu phụ tiếng Việt, chuyển sang chữ thường và xóa các ký tự đặc biệt
    public static String normalizeName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.replace("đ", "d")
                .replace("Đ", "D");

        return normalized.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    // Xác thực chuỗi đầu vào vị trí, theo mặc định không cho phép tên ngắn.
    public static String validateLocationInput(String input) {
        return validateLocationInput(input, false);
    }

    // Xác thực chuỗi đầu vào vị trí, với tùy chọn cho phép tên ngắn.
    public static String validateLocationInput(String input, boolean allowShortNames) {
        if (input == null) {
            throw new IllegalArgumentException("Input data is empty.");
        }
        input = normalizeName(input);
        if (input.isEmpty()) {
            throw new IllegalArgumentException("Input data is empty.");
        }
        if (!allowShortNames && input.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("Input name is too short (min " + MIN_LENGTH + " characters).");
        }
        if (input.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Input name is too long (max " + MAX_LENGTH + " characters).");
        }
        if (!isValidLocationName(input)) {
            throw new IllegalArgumentException("Input name is invalid.");
        }
        return input;
    }
}
