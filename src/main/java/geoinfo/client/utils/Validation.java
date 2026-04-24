package geoinfo.client.utils;

public class Validation {
    private static final String LOCATION_PATTERN = "[\\p{L}\\p{M}0-9 .,'()\\-]+";

    public static boolean isValidLocationName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        return input.matches(LOCATION_PATTERN);
    }

    public static String sanitizeInput(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }

    public static boolean isEmpty(String input) {
        return input == null || input.trim().isEmpty();
    }
}

