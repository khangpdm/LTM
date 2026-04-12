package geoinfo.server.processor;

import geoinfo.server.service.CityService;
import geoinfo.server.service.CountryService;
import org.json.JSONObject;

public class DataProcessor {
    public static String processData(String input) {
        if (input == null || input.isBlank()) {
            return new JSONObject()
                    .put("status", "error")
                    .put("message", "Data is empty.")
                    .toString(2);
        }

        String normalizedInput = input.trim();
        String lowerInput = normalizedInput.toLowerCase();

        if (lowerInput.equals("reloadcountry")) {
            return CountryService.reloadCountries();
        }

        if (lowerInput.startsWith("country:")) {
            String country = normalizedInput.substring("country:".length()).trim();
            if (country.isEmpty()) {
                return new JSONObject()
                        .put("status", "error")
                        .put("type", "country")
                        .put("message", "PLEASE ENTER COUNTRY NAME.")
                        .toString(2);
            }
            return CountryService.getCountryInfo(country);
        }

        if (lowerInput.startsWith("city:")) {
            String city = normalizedInput.substring("city:".length()).trim();
            if (city.isEmpty()) {
                return new JSONObject()
                        .put("status", "error")
                        .put("type", "city")
                        .put("message", "PLEASE ENTER CITY NAME.")
                        .toString(2);
            }
            return CityService.getCityInfo(city);
        }

        if (lowerInput.startsWith("country-more:")) {
            String country = normalizedInput.substring("country-more:".length()).trim();
            if (country.isEmpty()) {
                return new JSONObject()
                        .put("status", "error")
                        .put("type", "countryMoreInfo")
                        .put("message", "PLEASE ENTER COUNTRY NAME.")
                        .toString(2);
            }
            return CountryService.getCountryMoreInfo(country);
        }

        if (lowerInput.startsWith("city-more:")) {
            String city = normalizedInput.substring("city-more:".length()).trim();
            if (city.isEmpty()) {
                return new JSONObject()
                        .put("status", "error")
                        .put("type", "cityMoreInfo")
                        .put("message", "PLEASE ENTER CITY NAME.")
                        .toString(2);
            }
            return CityService.getCityMoreInfo(city);
        }

        String countryResult = CountryService.getCountryInfo(normalizedInput);
        if (isSuccessResponse(countryResult)) {
            return countryResult;
        }

        String cityResult = CityService.getCityInfo(normalizedInput);
        if (isSuccessResponse(cityResult)) {
            return cityResult;
        }

        return new JSONObject()
                .put("status", "error")
                .put("message", "Not found.")
                .put("countryResponse", new JSONObject(countryResult))
                .put("cityResponse", new JSONObject(cityResult))
                .toString(2);
    }

    private static boolean isSuccessResponse(String response) {
        try {
            return "success".equalsIgnoreCase(new JSONObject(response).optString("status"));
        } catch (Exception e) {
            return false;
        }
    }
}
