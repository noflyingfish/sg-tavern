package com.feiyu.discord.sg.tavern.utils;

import java.time.LocalDateTime;

public class StringUtils {
    
    public static String truncateTo(String value, int maxLen, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }
    
    public static String datetimeToString(LocalDateTime localDateTime, String fallback) {
        if (localDateTime != null) {
            return truncateTo(localDateTime.toString(), 16, fallback).replace('T', ' ');
        }
        return fallback;
    }
    
    public static String intToString(Integer i, int maxLen, String fallback) {
        if (i != null) {
            return truncateTo(i.toString(), maxLen, fallback);
        }
        return fallback;
    }
}
