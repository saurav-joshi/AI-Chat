package com.aibot.entityextraction;

import com.wanasit.chrono.ChronoOption;
import com.wanasit.chrono.ParsedDateComponent;
import com.wanasit.chrono.ParsedResult;
import com.wanasit.chrono.parser.ParserAbstract;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ENCustomizeTimeExpressionParser extends ParserAbstract {
    protected static String FIRST_REG_PATTERN = "(^|\\W|T)(at|from)?\\s*(\\d{1,2}|noon|midnight)((\\.|\\:|\\：)(\\d{2})((\\.|\\:|\\：)(\\d{2}))?)?(?!%)(\\s*(dinner|tonight|AM|PM|A\\.M\\.|P\\.M\\.))?(?=\\W|$)";
    protected static String SECOND_REG_PATTERN = "^\\s*(\\-|\\~|\\〜|to|\\?)\\s*(\\d{1,2})((\\.|\\:|\\：)(\\d{2})((\\.|\\:|\\：)(\\d{2}))?)?(?!%)(\\s*(dinner|tonight|AM|PM|A\\.M\\.|P\\.M\\.))?(?=\\W|$)";

    public ENCustomizeTimeExpressionParser() {
    }

    protected Pattern pattern() {
        return Pattern.compile(FIRST_REG_PATTERN, 2);
    }

    protected ParsedResult extract(String text, Date refDate, Matcher matcher, ChronoOption option) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(refDate);
        if(matcher.group(2) == null && matcher.group(11) == null && matcher.group(6) == null) {
            return null;
        } else {
            ParsedResult result = new ParsedResult();
            result.tags.add(this.getClass().getName());
            result.start.imply(ParsedDateComponent.Components.DayOfMonth, calendar.get(5));
            result.start.imply(ParsedDateComponent.Components.Month, calendar.get(2) + 1);
            result.start.imply(ParsedDateComponent.Components.Year, calendar.get(1));
            boolean hour = false;
            int minute = 0;
            int second = 0;
            byte meridiem = -1;
            int hour1;
            if(matcher.group(3).toLowerCase().equals("noon")) {
                meridiem = 1;
                hour1 = 12;
            } else if(matcher.group(3).toLowerCase().equals("midnight")) {
                meridiem = 0;
                hour1 = 0;
            } else {
                hour1 = Integer.parseInt(matcher.group(3));
            }

            if(matcher.group(6) != null) {
                minute = Integer.parseInt(matcher.group(6));
                if(minute >= 60) {
                    return null;
                }
            } else if(hour1 > 100) {
                minute = hour1 % 100;
                hour1 /= 100;
            }

            if(matcher.group(9) != null) {
                second = Integer.parseInt(matcher.group(9));
                if(second >= 60) {
                    return null;
                }
            }

            if(matcher.group(11) != null) {
                if(hour1 > 12) {
                    return null;
                }

                if(matcher.group(11).replace(".", "").toLowerCase().equals("am")) {
                    meridiem = 0;
                    if(hour1 == 12) {
                        hour1 = 0;
                    }
                }

                if(matcher.group(11).replace(".", "").toLowerCase().equals("pm")
                        || matcher.group(11).replace(".", "").toLowerCase().equals("dinner")
                        || matcher.group(11).replace(".", "").toLowerCase().equals("tonight")) {
                    meridiem = 1;
                    if(hour1 != 12) {
                        hour1 += 12;
                    }
                }
            }

            if(hour1 > 24) {
                return null;
            } else {
                if(hour1 >= 12) {
                    meridiem = 1;
                }

                result.index = matcher.start() + matcher.group(1).length();
                result.text = matcher.group().substring(matcher.group(1).length());
                result.start.assign(ParsedDateComponent.Components.Hour, hour1);
                result.start.assign(ParsedDateComponent.Components.Minute, minute);
                result.start.assign(ParsedDateComponent.Components.Second, second);
                if(meridiem >= 0) {
                    result.start.assign(ParsedDateComponent.Components.Meridiem, meridiem);
                }

                Pattern secondPattern = Pattern.compile(SECOND_REG_PATTERN, 2);
                matcher = secondPattern.matcher(text.substring(result.index + result.text.length()));
                if(!matcher.find()) {
                    return result;
                } else {
                    meridiem = -1;
                    minute = 0;
                    second = 0;
                    hour1 = Integer.parseInt(matcher.group(2));
                    if(matcher.group(5) != null) {
                        minute = Integer.parseInt(matcher.group(5));
                        if(minute >= 60) {
                            return result;
                        }
                    } else if(hour1 > 100) {
                        minute = hour1 % 100;
                        hour1 /= 100;
                    }

                    if(matcher.group(8) != null) {
                        second = Integer.parseInt(matcher.group(8));
                        if(second >= 60) {
                            return result;
                        }
                    }

                    if(matcher.group(10) != null) {
                        if(hour1 > 12) {
                            return result;
                        }

                        if(matcher.group(10).toLowerCase().equals("am") && hour1 == 12) {
                            hour1 = 0;
                            if(result.end == null) {
                                result.end = new ParsedDateComponent(result.start);
                            }

                            result.end.assign(ParsedDateComponent.Components.DayOfMonth, result.end.get(ParsedDateComponent.Components.DayOfMonth).intValue() + 1);
                        }

                        if(matcher.group(10).toLowerCase().equals("pm") && hour1 != 12) {
                            hour1 += 12;
                        }

                        if(!result.start.isCertain(ParsedDateComponent.Components.Meridiem)) {
                            if(matcher.group(10).toLowerCase().equals("am")) {
                                result.start.imply(ParsedDateComponent.Components.Meridiem, 0);
                                if(result.start.get(ParsedDateComponent.Components.Hour).intValue() == 12) {
                                    result.start.assign(ParsedDateComponent.Components.Hour, 0);
                                }
                            }

                            if(matcher.group(10).toLowerCase().equals("pm")) {
                                result.start.imply(ParsedDateComponent.Components.Meridiem, 1);
                                if(result.start.get(ParsedDateComponent.Components.Hour).intValue() != 12) {
                                    result.start.assign(ParsedDateComponent.Components.Hour, result.start.get(ParsedDateComponent.Components.Hour).intValue() + 12);
                                }
                            }
                        }
                    }

                    if(hour1 >= 12) {
                        meridiem = 1;
                    }

                    result.text = result.text + matcher.group();
                    if(result.end == null) {
                        result.end = new ParsedDateComponent(result.start);
                    }

                    result.end.assign(ParsedDateComponent.Components.Hour, hour1);
                    result.end.assign(ParsedDateComponent.Components.Minute, minute);
                    result.end.assign(ParsedDateComponent.Components.Second, second);
                    if(meridiem >= 0) {
                        result.end.assign(ParsedDateComponent.Components.Meridiem, meridiem);
                    }

                    return result;
                }
            }
        }
    }
}
