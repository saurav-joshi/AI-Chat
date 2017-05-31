package com.aibot.entityextraction;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.aibot.qa.Parser;
import com.wanasit.chrono.Chrono;
import com.wanasit.chrono.ChronoOption;
import com.wanasit.chrono.ParsedResult;
import scala.Tuple2;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DateExtraction implements EntityExtraction {
    String name;
    List<String[]> patterns;
    static SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
    static DateExtraction dateExtraction = null;
    static Chrono chrono;
    public DateExtraction(){
        ChronoOption options = ChronoOption.casualOptions();
        options.parsers.remove(1);
        //options.parsers.add(new ENCasualDateParser());
        options.parsers.add(new ENCustomizeTimeExpressionParser());
        options.parsers.add(new ENCustomizeMonthNameLittleEndianParser());
        options.parsers.add(new ENCustomizeDayOfWeekDateFormatParser());
        chrono = new Chrono(options);
    }

    public static DateExtraction getInstance(){
        if(dateExtraction ==null){
            dateExtraction = new DateExtraction();
        }
        return dateExtraction;
    }

    public static Date getDate(String text){
        Date date = chrono.parseDate(text.replaceAll("\\s+(?=\\p{Punct})", ""));
        return date;
    }

    public static String getTime(String text){
        Date date = Chrono.casual.parseDate(text);
        return new StringBuilder(String.valueOf(date.getHours())).append(":").append(date.getMinutes()).toString();
    }

    public static String formatDate(String text){
        return sdf.format(getDate(text));
    }

    public static String formatDate(Date date){
        return sdf.format(date);
    }

    public static String formatDateOnly(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }

    public static String formatTimeOnly(Date date){
        String time = "";
        if(date.getMinutes()<10){
            time = date.getHours() + ":0" + date.getMinutes();
        }else{
            time = date.getHours() + ":" + date.getMinutes();
        }
        return time;
    }

    @Override
    public EntityExtraction setPatterns(List<String[]> patterns) {
        this.patterns = patterns;
        return this;
    }

    @Override
    public boolean isPartialMatch() {
        return false;
    }

    @Override
    public SetMultimap<String[], Integer> searchPatternToPosIndex(String[] text) {
        SetMultimap<String[], Integer> result = HashMultimap.create();
        String sentence = String.join(" ", Arrays.asList(text)).replaceAll("\\s+(?=\\p{Punct})", "");

        List<ParsedResult> dates = chrono.parse(sentence);

        for(ParsedResult date: dates){
            String[] words = Parser.wordTokenize(date.text).stream().toArray(String[]::new);
            if(words!=null && words.length>0){
                int index = EntityExtractionUtil.getPosition(text, words);
                if (index > -1)
                    result.put(words,index);
            }
        }
        return result;
    }

    @Override
    public SetMultimap<String[], Tuple2<Integer, String[]>> searchPartial(String[] text) {
        return null;
    }

    @Override
    public EntityExtraction setEntityName(String entityName) {
        this.name = entityName;
        return this;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public List<String[]> getPatterns() {
        return this.patterns;
    }

}
