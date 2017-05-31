package com.aibot.entityextraction;

import com.wanasit.chrono.ChronoOption;
import com.wanasit.chrono.ParsedDateComponent;
import com.wanasit.chrono.ParsedResult;
import com.wanasit.chrono.parser.ParserAbstract;
import com.wanasit.chrono.parser.en.EnglishConstants;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ENCustomizeMonthNameLittleEndianParser extends ParserAbstract {
    protected static String regPattern = "(\\W|^)((Sunday|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sun|Mon|Tue|Wed|Thu|Fri|Sat)\\s*,?\\s*)?([0-9]{1,2})(st|nd|rd|th)?(\\s*(to|\\-|\\s)\\s*([0-9]{1,2})(st|nd|rd|th)?)?\\s*(Jan(?:uary|\\.)?|Feb(?:ruary|\\.)?|Mar(?:ch|\\.)?|Apr(?:il|\\.)?|May|Jun(?:e|\\.)?|Jul(?:y|\\.)?|Aug(?:ust|\\.)?|Sep(?:tember|\\.)?|Oct(?:ober|\\.|\\s)?|Nov(?:ember|\\.)?|Dec(?:ember|\\.)?)((\\s*[0-9]{4})(\\s*BE)?)?(\\W|$)";

    public ENCustomizeMonthNameLittleEndianParser() {
    }

    protected Pattern pattern() {
        return Pattern.compile(regPattern, 2);
    }

    protected ParsedResult extract(String text, Date refDate, Matcher matcher, ChronoOption option) {
        ParsedResult result = new ParsedResult(this, matcher.start() + matcher.group(1).length(), matcher.group());
        result.text = result.text.substring(matcher.group(1).length(), result.text.length() - matcher.group(14).length());
        int year = 0;
        if(matcher.group(11) != null) {
            year = Integer.parseInt(matcher.group(12).trim());
            if(year < 100) {
                if(year > 20) {
                    year = 0;
                } else {
                    year += 2000;
                }
            } else if(matcher.group(13) != null) {
                year -= 543;
            }
        }

        Calendar calendar = Calendar.getInstance(Locale.ENGLISH);
        calendar.setTime(refDate);
        Date date = null;
        int day = Integer.parseInt(matcher.group(4));
        int month = EnglishConstants.valueForMonth(matcher.group(10).toLowerCase().trim());
        if(year > 0) {
            calendar.set(year, month, day);
            if(calendar.get(5) == day) {
                date = calendar.getTime();
            }
        } else {
            year = calendar.get(1);
            calendar.set(year, month, day);
            if(calendar.get(5) == day) {
                date = calendar.getTime();
            }

            calendar.set(year, month, day);
            Date endDay;
            if(calendar.get(5) == day) {
                endDay = calendar.getTime();
                if(date == null || Math.abs(endDay.getTime() - refDate.getTime()) < Math.abs(date.getTime() - refDate.getTime())) {
                    date = endDay;
                }
            }

            calendar.set(year, month, day);
            if(calendar.get(5) == day) {
                endDay = calendar.getTime();
                if(date == null || Math.abs(endDay.getTime() - refDate.getTime()) < Math.abs(date.getTime() - refDate.getTime())) {
                    date = endDay;
                }
            }
        }

        if(date == null) {
            return null;
        } else if(matcher.group(8) != null) {
            int endDay1 = Integer.parseInt(matcher.group(8));
            int startDay = Integer.parseInt(matcher.group(4));
            calendar.setTime(date);
            calendar.set(5, startDay);
            if(calendar.get(5) != startDay) {
                return null;
            } else {
                result.start.assign(ParsedDateComponent.Components.Year, calendar.get(1));
                result.start.assign(ParsedDateComponent.Components.Month, calendar.get(2) + 1);
                result.start.assign(ParsedDateComponent.Components.DayOfMonth, calendar.get(5));
                calendar.setTime(date);
                calendar.set(5, endDay1);
                if(calendar.get(5) != endDay1) {
                    return null;
                } else {
                    result.end = new ParsedDateComponent();
                    result.end.assign(ParsedDateComponent.Components.Year, calendar.get(1));
                    result.end.assign(ParsedDateComponent.Components.Month, calendar.get(2) + 1);
                    result.end.assign(ParsedDateComponent.Components.DayOfMonth, calendar.get(5));
                    return result;
                }
            }
        } else {
            calendar.setTime(date);
            result.start.assign(ParsedDateComponent.Components.Year, calendar.get(1));
            result.start.assign(ParsedDateComponent.Components.Month, calendar.get(2) + 1);
            result.start.assign(ParsedDateComponent.Components.DayOfMonth, calendar.get(5));
            return result;
        }
    }
}

