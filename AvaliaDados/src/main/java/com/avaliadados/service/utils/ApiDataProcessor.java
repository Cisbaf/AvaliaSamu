package com.avaliadados.service.utils;

public class ApiDataProcessor {


    static long parseTimeToSeconds(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return 0L;
        }
        timeStr = timeStr.trim();
        String[] parts = timeStr.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Formato de tempo inválido: " + timeStr);
        }
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);
        return hours * 3600L + minutes * 60L + seconds;
    }
}