//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package geoinfo.server;

import geoinfo.service.CityService;
import geoinfo.service.CountryService;
import java.io.PrintWriter;

public class DataProcessor {
    public static void processData(String input, PrintWriter writer) {
        if (input.toLowerCase().startsWith("country:")) {
            String  datnuoc = input.substring("country:".length()).trim();
            String output = CountryService.getCountryInfo(datnuoc);
            writer.println(output);
        } else if (input.toLowerCase().startsWith("city:")) {
            String thanhpho = input.substring("city:".length()).trim();
            String output = CityService.getCityInfo(thanhpho);
            writer.println(output);
        } else {
            writer.println("Không xác định được loại dữ liệu. Vui lòng nhập 'country:<tên>' hoặc 'city:<tên>'.");
        }

    }
}
