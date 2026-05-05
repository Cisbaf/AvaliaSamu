package com.avaliadados.service.utils;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SheetsUtils {

    public static Map<String, Integer> getColumnMapping(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String value = getCellStringValueForHeader(cell);
                if (value != null && !value.isBlank()) {
                    map.put(value, i);
                }
            }
        }
        return map;
    }

    private static String getCellStringValueForHeader(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();

            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                CellType resultType = cell.getCachedFormulaResultType();
                switch (resultType) {
                    case NUMERIC:
                        return String.valueOf(cell.getNumericCellValue());
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
        if (cell == null) return null;

        log.debug("Lendo célula [{}] do tipo: {}", idx, cell.getCellType());

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    log.debug("Valor de data/hora detectado: {}", date);
                    return formattedTime(date);
                } else {
                    double numericValue = cell.getNumericCellValue();

                    // TRATAMENTO PARA VALORES NUMÉRICOS QUE REPRESENTAM TEMPO
                    if (numericValue >= 0 && numericValue < 1) {
                        long totalSeconds = Math.round(numericValue * 24 * 60 * 60);
                        long hours = totalSeconds / 3600;
                        long minutes = (totalSeconds % 3600) / 60;
                        long seconds = totalSeconds % 60;

                        String formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                        log.debug("Valor numérico {} interpretado como tempo: {} ({}s)", numericValue, formatted, totalSeconds);
                        return formatted;
                    }
                    return String.valueOf(numericValue);
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                CellType resultType = cell.getCachedFormulaResultType();
                log.debug("Fórmula detectada, tipo do resultado: {}", resultType);
                switch (resultType) {
                    case NUMERIC:
                        if (DateUtil.isCellDateFormatted(cell)) {
                            Date date = cell.getDateCellValue();
                            log.debug("Resultado da fórmula é data/hora: {}", date);
                            return formattedTime(date);
                        } else {
                            double numericValue = cell.getNumericCellValue();

                            // TRATAMENTO PARA FÓRMULAS NUMÉRICAS QUE REPRESENTAM TEMPO
                            if (numericValue >= 0 && numericValue < 1) {
                                long totalSeconds = Math.round(numericValue * 24 * 60 * 60);
                                long hours = totalSeconds / 3600;
                                long minutes = (totalSeconds % 3600) / 60;
                                long seconds = totalSeconds % 60;

                                String formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                                log.debug("Valor de fórmula numérica {} interpretado como tempo: {} ({}s)", numericValue, formatted, totalSeconds);
                                return formatted;
                            }
                            return String.valueOf(numericValue);
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

    private static String formattedTime(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int hours = cal.get(Calendar.HOUR_OF_DAY);
        int minutes = cal.get(Calendar.MINUTE);
        int seconds = cal.get(Calendar.SECOND);
        String formatted = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        log.debug("Tempo formatado: {}", formatted);
        return formatted;
    }

    public static Long parseTimeToSeconds(String timeStr) {
        if (timeStr == null || timeStr.isBlank()) {
            return 0L;
        }
        log.debug("Convertendo tempo: '{}'", timeStr);
        timeStr = timeStr.trim().toLowerCase();

        try {
            // Novo formato: Xd HH:mm:ss (ex: "3d 78:59:11")
            if (timeStr.matches("\\d+\\s*d\\s*\\d{1,3}:\\d{2}:\\d{2}")) {
                String[] dayTimeParts = timeStr.split("\\s*d\\s*");
                long days = Long.parseLong(dayTimeParts[0]);
                String[] timeParts = dayTimeParts[1].split(":");
                long hours = Long.parseLong(timeParts[0]);
                long minutes = Long.parseLong(timeParts[1]);
                long seconds = Long.parseLong(timeParts[2]);
                long total = days * 24 * 3600 + hours * 3600 + minutes * 60 + seconds;
                log.debug("Tempo convertido com dias: {}s", total);
                return total;
            }

            // Novo formato: Xd HH:mm (ex: "3d 78:59")
            if (timeStr.matches("\\d+\\s*d\\s*\\d{1,3}:\\d{2}")) {
                String[] dayTimeParts = timeStr.split("\\s*d\\s*");
                long days = Long.parseLong(dayTimeParts[0]);
                String[] timeParts = dayTimeParts[1].split(":");
                long hours = Long.parseLong(timeParts[0]);
                long minutes = Long.parseLong(timeParts[1]);
                long total = days * 24 * 3600 + hours * 3600 + minutes * 60;
                log.debug("Tempo convertido com dias (sem segundos): {}s", total);
                return total;
            }

            // HH:mm:ss (permite horas com até 3 dígitos para casos como "78:59:11")
            if (timeStr.matches("\\d{1,3}:\\d{2}:\\d{2}")) {
                String[] parts = timeStr.split(":");
                long total = Long.parseLong(parts[0]) * 3600
                        + Long.parseLong(parts[1]) * 60
                        + Long.parseLong(parts[2]);
                log.debug("Tempo convertido: {}s", total);
                return total;
            }

            // HH:mm (permite horas com até 3 dígitos)
            if (timeStr.matches("\\d{1,3}:\\d{2}")) {
                String[] parts = timeStr.split(":");
                long total = Long.parseLong(parts[0]) * 3600
                        + Long.parseLong(parts[1]) * 60;
                log.debug("Tempo convertido: {}s", total);
                return total;
            }

            // numérico direto (incluindo notação científica)
            if (timeStr.matches("-?\\d+(\\.\\d+)?([eE][-+]?\\d+)?")) {
                double value = Double.parseDouble(timeStr);
                long seconds;

                // Se for um valor muito pequeno (< 1), provavelmente é fração de dia
                if (value > 0 && value < 1) {
                    seconds = Math.round(value * 24 * 60 * 60);
                    log.debug("Valor numérico {} interpretado como fração de dia: {}s", value, seconds);
                } else {
                    seconds = Math.round(value);
                    log.debug("Tempo convertido numericamente: {}s", seconds);
                }
                return seconds;
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
                            + cal.get(Calendar.SECOND);
                    log.debug("Tempo convertido com formato {}: {}s", fmt.toPattern(), total);
                    return total;
                } catch (ParseException ignored) {
                }
            }
        } catch (Exception e) {
            log.error("Erro ao converter tempo '{}': {}", timeStr, e.getMessage());
        }
        log.warn("Não foi possível converter '{}', retornando 0", timeStr);
        return 0L;
    }

    public static String normalizeName(String name) {
        if (name == null || name.isBlank()) return null;

        // 1) Remover conteúdo entre parênteses e colchetes e sufixos comuns de turno
        String cleaned = name.replaceAll("\\(.*?\\)", " ")
                .replaceAll("\\[.*?]", " ")
                // remove sufixos como " - DIA", " - NOITE", " (NOITE)" etc.
                .replaceAll("(?i)\\s*[-–—:]\\s*(DIA|NOITE|TURNO|TARDE|MANHA|MATUTINO|NOTURNO)\\b", " ")
                .replaceAll("(?i)\\b(DIA|NOITE|TURNO|TARDE|MANHA|MATUTINO|NOTURNO)\\b", " ");

        // 2) Normalização de acentos
        String normalized = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);

        // 3) Manter apenas letras/números e espaços, colapsar múltiplos espaços
        normalized = normalized.replaceAll("[^A-Z0-9 ]", " ")
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