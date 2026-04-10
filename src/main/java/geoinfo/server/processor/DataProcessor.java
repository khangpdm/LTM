package geoinfo.server.processor;

import geoinfo.server.service.CityService;
import geoinfo.server.service.CountryService;

public class DataProcessor {
    private static final String ERROR_PREFIX = "Loi khi lay du lieu";
    private static final String META_PREFIX = "__META__";

    public static String processData(String input) {
        if (input == null || input.isBlank()) {
            return "Du lieu rong.";
        }

        String normalizedInput = input.trim();
        String lowerInput = normalizedInput.toLowerCase();

        if (lowerInput.equals("reloadcountry")) {
            return CountryService.reloadCountries();
        }

        if (lowerInput.startsWith("country:")) {
            String country = normalizedInput.substring("country:".length()).trim();
            if (country.isEmpty()) {
                return "Vui long nhap ten quoc gia.";
            }
            return buildCountryResponse(country);
        }

        if (lowerInput.startsWith("city:")) {
            String city = normalizedInput.substring("city:".length()).trim();
            if (city.isEmpty()) {
                return "Vui long nhap ten thanh pho.";
            }
            return buildCityResponse(city);
        }

        if (lowerInput.startsWith("country-news:")) {
            String country = normalizedInput.substring("country-news:".length()).trim();
            if (country.isEmpty()) {
                return "Vui long nhap ten quoc gia.";
            }
            return CountryService.getCountryNews(country);
        }

        if (lowerInput.startsWith("city-news:")) {
            String city = normalizedInput.substring("city-news:".length()).trim();
            if (city.isEmpty()) {
                return "Vui long nhap ten thanh pho.";
            }
            return CityService.getCityNews(city);
        }

        String countryResult = CountryService.getCountryInfo(normalizedInput);
        if (!countryResult.startsWith(ERROR_PREFIX)) {
            return countryResult;
        }

        String cityResult = CityService.getCityInfo(normalizedInput);
        if (!cityResult.startsWith(ERROR_PREFIX)) {
            return cityResult;
        }

        return "Khong tim thay thong tin.\n" + countryResult + "\n" + cityResult;
    }

    private static String buildCountryResponse(String country) {
        try {
            CountryService.CountryDetails details = CountryService.getCountryDetails(country);
            StringBuilder response = new StringBuilder();
            appendMeta(response, "flagUrl", details.flagUrl());
            appendMeta(response, "moreInfoRequest", "country-news:" + details.commonName());
            appendMeta(response, "moreInfoLabel", "Them thong tin");
            response.append("\n").append(details.summary());
            return response.toString();
        } catch (Exception ex) {
            return CountryService.getCountryInfo(country);
        }
    }

    private static String buildCityResponse(String city) {
        try {
            CityService.CityDetails details = CityService.getCityDetails(city);
            StringBuilder response = new StringBuilder();
            appendMeta(response, "moreInfoRequest", "city-news:" + details.name());
            appendMeta(response, "moreInfoLabel", "Them thong tin");
            response.append("\n").append(details.summary());
            return response.toString();
        } catch (Exception ex) {
            return CityService.getCityInfo(city);
        }
    }

    private static void appendMeta(StringBuilder response, String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        response.append(META_PREFIX).append(key).append("=").append(value).append("\n");
    }
}
