package com.avaliadados.service.utils;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SheetsUtils {

    public static Map<String, Integer> getColumnMapping(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String value = getCellStringValue(cell);
                if (value != null && !value.isBlank()) {
                    map.put(value, i);
                }
            }
        }
        return map;
    }

    public static String getCellStringValue(Cell cell) {
        if (cell == null) return null;


        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return formattedTime(date);
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue >= 0 && numericValue < 1) {
                        long totalSeconds = Math.round(numericValue * 24 * 60 * 60);
                        long hours = totalSeconds / 3600;
                        long minutes = (totalSeconds % 3600) / 60;
                        long seconds = totalSeconds % 60;

                        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    }
                    return String.valueOf(numericValue);
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                CellType resultType = cell.getCachedFormulaResultType();
                switch (resultType) {
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            Date date = cell.getDateCellValue();
                            return formattedTime(date);
                        } else {
                            return String.valueOf(cell.getNumericCellValue());
                        }
                    case STRING:
                        return cell.getStringCellValue();
                    case BOOLEAN:
                        return String.valueOf(cell.getBooleanCellValue());
                    default:
                        return cell.getCellFormula();
                }
            default:
                return null;
        }
    }

    public static String getCellStringValue(Row row, int idx) {
        Cell cell = row.getCell(idx);
        return getCellStringValue(cell);
    }

    private static String formattedTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int hours = cal.get(Calendar.HOUR_OF_DAY);
        int minutes = cal.get(Calendar.MINUTE);
        int seconds = cal.get(Calendar.SECOND);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static Long parseTimeToSeconds(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return 0L;
        }
        timeStr = timeStr.trim();

        try {
            // HH:mm:ss
            if (timeStr.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
                String[] parts = timeStr.split(":");
                return Long.parseLong(parts[0]) * 3600
                        + Long.parseLong(parts[1]) * 60
                        + Long.parseLong(parts[2]);
            }
            // HH:mm
            if (timeStr.matches("\\d{1,2}:\\d{2}")) {
                String[] parts = timeStr.split(":");
                return Long.parseLong(parts[0]) * 3600
                        + Long.parseLong(parts[1]) * 60;
            }
            // numérico direto
            if (timeStr.matches("\\d+(\\.\\d+)?")) {
                return Math.round(Double.parseDouble(timeStr));
            }
            // formatos com SimpleDateFormat
            SimpleDateFormat[] formats = {
                    new SimpleDateFormat("hh:mm:ss a"),
                    new SimpleDateFormat("hh:mm a"),
                    new SimpleDateFormat("HH:mm:ss"),
                    new SimpleDateFormat("HH:mm")
            };
            for (SimpleDateFormat fmt : formats) {
                try {
                    Date date = fmt.parse(timeStr);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    long total = cal.get(Calendar.HOUR_OF_DAY) * 3600L
                            + cal.get(Calendar.MINUTE) * 60L
                            + cal.get(Calendar.DAY_OF_MONTH);
                    total += cal.get(Calendar.SECOND);
                    return total;
                } catch (ParseException ignored) {
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter tempo: " + timeStr, e);
        }
        return 0L;
    }

    public static String normalizeName(String name) {
        if (name == null || name.isBlank()) return null;
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll(" +", " ")
                .trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static double similarity(String a, String b) {
        if (a == null || b == null) return 0;
        int dist = LevenshteinDistance
                .getDefaultInstance()
                .apply(a, b);
        return 1.0 - (double) dist / Math.max(a.length(), b.length());
    }
}
