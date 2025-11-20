package com.feiyu.discord.sg.tavern.utils;

import java.util.regex.Pattern;

public class RegexUtil {
    
    private static final String TIME_REGEX = "\\b(?:" +
            // 24-hour formats: 14:30, 14.30
            "\\d{1,2}[:.]\\d{2}" + "|" +
            // 12-hour formats: 2:30 PM, 2:30pm (max 1 space before AM/PM)
            "\\d{1,2}[:.]\\d{2}[\\s]?[AaPp][Mm]" + "|" +
            // Simple formats: 9 AM, 11 PM, 5am (max 1 space before AM/PM)
            "\\d{1,2}[\\s]?[AaPp][Mm]" + "|" +
            // Pattern for 0000 format time: 0000, 2359, 1230, 0900
            "(?:[01]\\d|2[0-3])[0-5]\\d" + ")\\b";
    
    private static final String DATE_REGEX = "(?i)\\b(?:" +
            // DD/MM/YYYY or DD-MM-YYYY: 1/1/1990, 25/12/2023, 15-01-2024
            "(?:0?[1-9]|[12]\\d|3[01])[/\\-](?:0?[1-9]|1[0-2])[/\\-]\\d{4}" + "|" +
            // DD MMM format: 1 jan, 25 Dec, 05 Jan 2023, 5 March, 15 Mar 2025
            "(?i)(?:0?[1-9]|[12]\\d|3[01])\\s?(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]{0,6}(?:\\s?\\d{4})?" + ")\\b";
    
    private static final String ASCII_REGEX = "[^\\x00-\\x7F]";
    
    public static boolean containTime(String text) {
        return Pattern.compile(TIME_REGEX).matcher(text).find();
    }
    
    public static boolean containDate(String text){
        return Pattern.compile(DATE_REGEX).matcher(text).find();
    }
    
    public static String keepAscii(String input){
        if (input == null) return null;
        return input.replaceAll(ASCII_REGEX, "");
    }
    
}
