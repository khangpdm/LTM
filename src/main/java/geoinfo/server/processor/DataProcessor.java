package geoinfo.server.processor;

import geoinfo.server.service.CityService;
import geoinfo.server.service.CountryService;

public class DataProcessor {
    /*private static final String COUNTRY_NOT_FOUND_PREFIX =
            "Lỗi khi lấy dữ liệu quốc gia: không tìm thấy quốc gia phù hợp.";*/
    private static final String ERROR_PREFIX = "Lỗi khi lấy dữ liệu";

    public static String processData(String input) {
        if (input == null || input.isBlank()) {
            return "Dữ liệu rỗng.";
        }

        String normalizedInput = input.trim();
        String lowerInput = normalizedInput.toLowerCase();

        if (lowerInput.equals("reloadcountry")) {
            return CountryService.reloadCountries();
        }

        if (lowerInput.startsWith("country:")) {
            String country = normalizedInput.substring("country:".length()).trim();
            if (country.isEmpty()) {
                return "Vui lòng nhập tên quốc gia.";
            }
            return CountryService.getCountryInfo(country);
        }

        if (lowerInput.startsWith("city:")) {
            String city = normalizedInput.substring("city:".length()).trim();
            if (city.isEmpty()) {
                return "Vui lòng nhập tên thành phố.";
            }
            return CityService.getCityInfo(city);
        }

        String countryResult = CountryService.getCountryInfo(normalizedInput);
        if (!countryResult.startsWith(ERROR_PREFIX)) {
            return countryResult;
        }

        String cityResult = CityService.getCityInfo(normalizedInput);
        if (!cityResult.startsWith(ERROR_PREFIX)) {
            return cityResult;
        }

        // Cả hai đều fail
        return "Không tìm thấy thông tin.\n" + countryResult + "\n" + cityResult;
    }
}