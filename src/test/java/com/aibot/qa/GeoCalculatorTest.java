package com.aibot.qa;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.aibot.entityextraction.LocationFromAddress;
import org.slf4j.LoggerFactory;
import scala.Tuple2;

public class GeoCalculatorTest {
    public static void setup() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);
    }

    public void geoTest() throws Exception {
        String locationFile = GlobalConstants.qaFolderNameS3 + "/" + "address_latlong.txt";
        String locationLatLongFile = GlobalConstants.qaFolderNameS3 + "/" + "qa_recheck_location_latlong.tsv";
        GeoCalculator.getLatLongFomFile(locationFile, locationLatLongFile);
    }

    public void geoAddressTest() throws Exception {
//        String locationFile = GlobalConstants.qaFolderNameS3 + "/" + "address_latlong_batch2.txt";
        String locationFile = "latlong_district_roads.txt";
        String locationLatLongFile = GlobalConstants.qaFolderNameS3 + "/" + "qa_latlong_district_roads.txt";
        GeoCalculator.getLatLongFomAddress(locationFile, locationLatLongFile);
    }

    public void getGeoOfLocation(){
        Tuple2 t = LocationFromAddress.getLatLong("bedok central","singapore");
        if(t!=null) System.out.println("Lat: " + t._1() + " Long: " + t._2());
    }

    public void getCityFromGeo(){
        String lat = "28.61257702551"; String lon = "77.231171635249";
        //System.out.println(LocationFromAddress.getTimeZoneFromLatLon(lat,lon));
        try {
            //delhi
            System.out.println(GeoCalculator.getCityFromLatLongOpenMap(lat,lon));
            //singapore
            System.out.println(GeoCalculator.getCityFromLatLongOpenMap("1.2984826","103.7871546"));
            //dubai
            System.out.println(GeoCalculator.getCityFromLatLongOpenMap("25.2048","55.2708"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}