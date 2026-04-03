package geoinfo.server.processor;

import geoinfo.server.service.CityService;
import geoinfo.server.service.CountryService;

public class DataProcessor {

    public static String processData(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "No results found. Try a different search term.";
        }
        input = input.trim();

        if (input.toLowerCase().startsWith("country ")) {
            String datnuoc = input.substring("country ".length()).trim();
            return CountryService.getCountryInfo(datnuoc);

        } else if (input.toLowerCase().startsWith("city ")) {
            String thanhpho = input.substring("city ".length()).trim();
            return CityService.getCityInfo(thanhpho);

        } else {
            return "No results found. Try a different search term.";
        }
    }
}


//import geoinfo.server.service.CityService;
//import geoinfo.server.service.CountryService;
//import java.io.PrintWriter;
//
//public class DataProcessor {
//    public static void processData(String input, PrintWriter writer) {
//        if (input.toLowerCase().startsWith("country:")) {
//            String datnuoc = input.substring("country:".length()).trim();
//            String output = CountryService.getCountryInfo(datnuoc);
//            writer.println(output);
//        } else if (input.toLowerCase().startsWith("city:")) {
//            String thanhpho = input.substring("city:".length()).trim();
//            String output = CityService.getCityInfo(thanhpho);
//            writer.println(output);
//        } else {
//            writer.println("Không xác định được loại dữ liệu. Vui lòng nhập 'country:<tên>' hoặc 'city:<tên>'.");
//        }
//
//    }
//}
