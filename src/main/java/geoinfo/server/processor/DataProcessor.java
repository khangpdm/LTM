package geoinfo.server.processor;

import geoinfo.server.service.CityService;
import geoinfo.server.service.CountryService;

public class DataProcessor {
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
            return CountryService.getCountryInfo(country);
        }

        if (lowerInput.startsWith("city:")) {
            String city = normalizedInput.substring("city:".length()).trim();
            return CityService.getCityInfo(city);
        }

        String countryResult = CountryService.getCountryInfo(normalizedInput);
        if (!countryResult.startsWith("Lỗi khi lấy dữ liệu quốc gia: không tìm thấy quốc gia phù hợp.")) {
            return countryResult;
        }

        return CityService.getCityInfo(normalizedInput);
    }
}
