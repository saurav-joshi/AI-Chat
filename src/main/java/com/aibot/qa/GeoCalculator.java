package com.aibot.qa;

import com.aibot.entityextraction.LocationFromAddress;
import com.google.common.io.Resources;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;

public class GeoCalculator {

    public static String[] getLatLongPositions(String address) throws Exception {
        int responseCode = 0;
        //String api = "http://maps.googleapis.com/maps/api/geocode/xml?address=" + URLEncoder.encode(address, "UTF-8") + "&sensor=true";
        String api = "http://maps.googleapis.com/maps/api/geocode/xml?address=" + address + "&sensor=true";
        //String api = "http://nominatim.openstreetmap.org/search.php?q=" + URLEncoder.encode(address, "UTF-8") + "&format=json";
        //URL url = new URL(api);
        //HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
       // httpConnection.connect();
        //responseCode = httpConnection.getResponseCode();
        try{
            URL url = new URL(api);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (responseCode == 200) {
                DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document document = builder.parse(conn.getInputStream());
                XPathFactory xPathfactory = XPathFactory.newInstance();
                XPath xpath = xPathfactory.newXPath();
                XPathExpression expr = xpath.compile("/GeocodeResponse/status");
                String status = (String) expr.evaluate(document, XPathConstants.STRING);
                if (status.equals("OK")) {
                    expr = xpath.compile("//geometry/location/lat");
                    String latitude = (String) expr.evaluate(document, XPathConstants.STRING);
                    expr = xpath.compile("//geometry/location/lng");
                    String longitude = (String) expr.evaluate(document, XPathConstants.STRING);
                    return new String[]{latitude, longitude};
                } else {
                    throw new Exception("Error from the API - response status: " + status);

                }
            }else {
                return getLatLongPositionsOpenMap(address);
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
            throw new Exception("Error from the API - response status: " + e.getMessage());
        }
        //return null;
    }

    public static String[] getLatLongPositionsOpenMap(String address) throws Exception {
        String query = "http://nominatim.openstreetmap.org/search.php?q=" + URLEncoder.encode(address, "UTF-8") + "&format=json";
        try {
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output = "", full = "";
            while ((output = br.readLine()) != null) {
                full += output;
            }
            //full += "}";
            //System.out.println(full);
            conn.disconnect();
            //JSONObject json = new JSONObject(full);
            JSONArray jarr = new JSONArray(full);//json.getJSONArray("results");
            JSONObject json = jarr.getJSONObject(0);
            String latitude = json.getString("lat");
            String longitude = json.getString("lon");
            return new String[]{latitude, longitude};
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getCityFromLatLongOpenMap(String lat, String lon){
        String query = "http://nominatim.openstreetmap.org/reverse?format=json&lat=" + lat + "&lon=" + lon + "&zoom=18&addressdetails=1?";
        String city = "";
        try {
            URL url = new URL(query);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output = "", full = "";
            while ((output = br.readLine()) != null) {
                full += output;
            }
            //System.out.println(full);
            conn.disconnect();
            JSONObject json = new JSONObject(full);
            JSONObject addObj = json.getJSONObject("address");
            Set<String> keys = addObj.keySet();
            if(keys.contains("city")){
                city = addObj.getString("city");
            }else {
                if (keys.contains("state"))
                    city = addObj.getString("state");
                else if (keys.contains("country")) {
                    city = addObj.getString("country");
                }
            }
            if(city.equalsIgnoreCase("new delhi")) city= "delhi";
        } catch (Exception e) {
            city = LocationFromAddress.getTimeZoneFromLatLon(lat,lon);
            if(city.equalsIgnoreCase("utc")) city= "";
        }
        return city.toLowerCase();
    }

    private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == "K") {
            dist = dist * 1.609344;
        } else if (unit == "N") {
            dist = dist * 0.8684;
        }

        return (dist);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
        /*::	This function converts decimal degrees to radians						 :*/
        /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
        /*::	This function converts radians to decimal degrees						 :*/
        /*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }


    public static void getLatLongFomFile(String inputFile, String ouputFile) throws Exception {
        try{
            List<String> lines = S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, inputFile);
            List<String> outputLines = S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, ouputFile);

            for (String line : lines) {
                try {
                    if(LocationFromAddress.getLatLong(line,"")==null) {
                        String[] latlong = getLatLongPositions(new StringBuilder(line).append(", singapore").toString());
                        outputLines.add(new StringBuilder(line).append("\t").append(latlong[0]).append("\t").append(latlong[1]).toString());
                    }
                }catch (Exception e){
                    System.out.println(line + "\t" + e.getMessage());
                    continue;
                }
            }
            S3Handler.writeLinesToFile(GlobalConstants.taxonomiesBucketNameS3,ouputFile,outputLines);
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * File format: location name (example: jame) \t address
     * @param inputFile
     * @param ouputFile
     * @throws Exception
     */
    public static void getLatLongFomAddress(String inputFile, String ouputFile) throws Exception {
        try{
            //List<String> lines = S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, inputFile);
            List<String> lines = Resources.readLines(Resources.getResource(inputFile), Charset.defaultCharset());
//            List<String> outputLines = S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, ouputFile);

            for (String line : lines) {
                try {
                    String[] parts = line.split("\t");
                    //if(LocationFromAddress.getLatLong(parts[0])==null) {
                    if(!line.trim().isEmpty()){
                        String[] latlong = getLatLongPositions(line.replace("\"","") + ", Singapore");
                        //outputLines.add(new StringBuilder(parts[0]).append("\t").append(latlong[0]).append("\t").append(latlong[1]).toString());
                        System.out.println(new StringBuilder(parts[0]).append("\t").append(latlong[0]).append("\t").append(latlong[1]).toString());
                    }
                }catch (Exception e){
                    System.out.println(line+ "\t" + e.getMessage());
                    continue;
                }
            }
           // S3Handler.writeLinesToFile(GlobalConstants.taxonomiesBucketNameS3,ouputFile,outputLines);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
