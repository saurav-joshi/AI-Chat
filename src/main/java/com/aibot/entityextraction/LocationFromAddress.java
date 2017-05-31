package com.aibot.entityextraction;


import com.aibot.entity.Location;
import com.aibot.qa.GlobalConstants;
import com.aibot.qa.Parser;
import com.aibot.qa.S3Handler;
import com.aibot.qa.SynonymMappingAndLemmatization;
import com.aibot.qa.*;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import gapchenko.llttz.Converter;
import gapchenko.llttz.IConverter;
import gapchenko.llttz.stores.TimeZoneListStore;
import scala.Tuple2;

import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LocationFromAddress {
    private static ListMultimap mapLatLong = ArrayListMultimap.create();
    static {
        Pattern regex = Pattern.compile("[$&+,:;=?@#|]");

        try{
            List<String> lines = S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, GlobalConstants.locationLatLongFile);
            lines.subList(1,lines.size()).stream().forEach(line ->{
                String[] parts = line.split("\t");
                if(!(parts[3].isEmpty() && parts[4].isEmpty())){
                    Matcher matcher = regex.matcher(parts[0]);
                    parts[0] = matcher.find() ? SynonymMappingAndLemmatization.run(parts[0]) : Parser.stem(parts[0].toLowerCase());
                    mapLatLong.put(parts[0], new Location(parts[0], parts[1], parts[2], Double.valueOf(parts[3]), Double.valueOf(parts[4]),parts[5]));
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static List<String> citiesCovered = new ArrayList<>();
    static {
        try{
            List<String> lines = S3Handler.readLinesFromFile(GlobalConstants.taxonomiesBucketNameS3, GlobalConstants.citiesCoveredFile);
            lines.subList(1,lines.size()).stream().forEach(line ->{
                citiesCovered.add(line.toLowerCase());
            });
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static List<String> getCitiesCovered(){
        return citiesCovered;
    }

    public static List<Location> getLocationDetailsByLocation(String location){
        if(location == null || location.trim().length() == 0)
            return null;
        return mapLatLong.get(location.toLowerCase());
    }

    public static List<Location> getLocationDetailsByLocation(String location, String city){
        List<Location> locations = getLocationDetailsByLocation(location);
        if(city.isEmpty()) return locations;
        return locations.stream().filter(x -> x.getCity().equalsIgnoreCase(city)).collect(Collectors.toList());
    }

    public static List<Tuple2<String,String>> getCityAndCountryByLocation(String location){
        if(location == null || location.trim().length() == 0)
            return null;
        List<Location> locations = getLocationDetailsByLocation(location);
        if(locations == null || locations.size() == 0){
            System.out.println("NO such location found in our file");
            return null;
        }
        List<Tuple2<String, String>> cityCountryList = new ArrayList<>();
        locations.forEach(x->{
            Tuple2<String,String> cityCountry = new Tuple2<String, String>(x.getCity(),x.getCountry());
            cityCountryList.add(cityCountry);
        });
        return cityCountryList;
    }

    public static List<Tuple2<Double, Double>> getLatLonsByLocation(String location, String city){
        if(location == null || location.trim().length() == 0)
            return null;
        List<Location> locations = getLocationDetailsByLocation(location, city);
        if(locations == null || locations.size() == 0){
            System.out.println("No such location is found in our geo location dictionary");
            return null;
        }
        List<Tuple2<Double, Double>> resultsLatLons = new ArrayList<>();
        locations.forEach(x->{
            resultsLatLons.add(new Tuple2<>(x.getLat(),x.getLon()));
        });
        return resultsLatLons;
    }

    public static boolean isLocationBuildingOrStreet(Location location){
        if(location == null)
            return false;
        Set<String> locationTypesDoNotExtend = Sets.newHashSet("building", "building-mall", "building-hotel", "building-office", "building-others","street");
        if(locationTypesDoNotExtend.contains(location.getType())){
            return true;
        }else{
            return false;
        }
    }

    public static Tuple2<Double,Double> getLatLong(String location, String city) {
        List<Tuple2<Double, Double>> results = getLatLonsByLocation(location, city);
        if(results == null || results.size() == 0){
            return null;
        }else{
            return results.get(0);
        }
    }

    public static boolean isBuildingOrStreet(String location, String city){
        if(location == null || location.trim().length() == 0){
            return false;
        }
        List<Location> locations = getLocationDetailsByLocation(location, city);
        if(locations == null || locations.size() == 0){
            System.out.println("NO such location found in our file");
            return false;
        }
        return isLocationBuildingOrStreet(locations.get(0));
    }

    public static boolean isBuildingOrStreet(String location){
        if(location == null || location.trim().length() == 0){
            return false;
        }
        List<Location> locations = getLocationDetailsByLocation(location);
        if(locations == null || locations.size() == 0){
            System.out.println("NO such location found in our file");
            return false;
        }
        return isLocationBuildingOrStreet(locations.get(0));
    }

    String apprFile = "dictionary_address_appr.tsv";
    public void locationFromAddress(String path){
        List<String> location = new ArrayList<>();

        try{
            List<String> lines = Resources.readLines(Resources.getResource(apprFile), Charset.defaultCharset());
            final Map<String, String> appr = new HashMap();
            List<String[]> patterns = new ArrayList<>();
            for(int i=1; i < lines.size(); i++){
                String line = lines.get(i);
                line = line.toLowerCase();
                String[] words = line.split("\t");
                appr.put(words[0], words[1]);
                patterns.add(words[0].split("\\s+"));
            }
            /*lines.subList(1,lines.size()).forEach(line ->{
                line = line.toLowerCase();
                String[] words = line.split("\t");
                appr.put(words[0], words[1]);
                patternsForState.add(words[0].split("\\s+"));
            });*/
            ACTrie trie = new ACTrie<>(patterns,"");

            List<String> addresses = Resources.readLines(Resources.getResource(path), Charset.defaultCharset());
            addresses.stream().forEach(line -> {
                line = line.toLowerCase();
                String[] components = line.split("[,|\\d]");

                for(String component: components){
                    component = component.replaceAll("\\p{Punct}", "").trim();
                    List<String> apprWords = trie.searchPatternInString(component.split("\\s+"));

                    for(String word: apprWords){
                        component = component.replace(word,appr.get(word));
                    }
                    //&& !component.replaceAll("\\W", "").matches("\\d+")
                    if(!component.isEmpty()  && !location.contains(component)){
                        component = component.replaceAll("^\\d+", "").trim();
                        if(component.length()>2){
                            System.out.println(component);
                            location.add(component);
                        }
                    }
                }
            });
            System.out.println("Write update to S3");
            S3Handler.writeLinesToFile(GlobalConstants.taxonomiesBucketNameS3,"KR-Dictionary/sg_locations.tsv", location.stream().collect(Collectors.toList()));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String getTimeZoneFromLatLon(String latStr, String lonStr){
        //if city available, from restaurant or from QA, just return city
        String timezone = "UTC";
        try {
            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            //try google API first

            IConverter iconv = Converter.getInstance(TimeZoneListStore.class);
            TimeZone tz = iconv.getTimeZone(lat, lon);
            timezone = tz.getDisplayName().replace("Time","").trim();
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
        return timezone;
    }

}
