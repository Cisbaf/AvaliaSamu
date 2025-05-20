package com.avaliadados.service.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class SheetsUtils {
    /**
     * Mapeia cabeçalho de planilha para índice de colunas, normalizando texto.
     */
    public static Map<String, Integer> getColumnMapping(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        DataFormatter fmt = new DataFormatter();
        for (Cell cell : headerRow) {
            String raw = fmt.formatCellValue(cell);
            String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                    .replaceAll("[^\\p{ASCII}]", "")
                    .replaceAll("[^A-Z0-9 ]", "")
                    .trim()
                    .toUpperCase(Locale.ROOT);
            columnMap.put(normalized, cell.getColumnIndex());
        }
        return columnMap;
    }

    /**
     * Busca valor string de uma célula, lidando com tipos NUMERIC e STRING.
     */
    public static String getCellStringValue(Row row, Integer colIndex) {
        if (colIndex == null) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return new DataFormatter().formatCellValue(cell);
        }
        return null;
    }

    /**
     * Converte representações de tempo em segundos ("HH:mm:ss", "12:00:00 AM" ou apenas segundos).
     */
    public static Long parseTimeToSeconds(String s) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        try {
            if (s.matches("\\d+")) {
                return Long.parseLong(s);
            }
            if (s.toUpperCase(Locale.ROOT).matches("\\d{1,2}:\\d{2}:\\d{2}\\s*(AM|PM)")) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.US);
                return (long) LocalTime.parse(s.toUpperCase(Locale.ROOT), fmt).toSecondOfDay();
            }
            if (s.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("H:mm:ss", Locale.US);
                return (long) LocalTime.parse(s, fmt).toSecondOfDay();
            }
        } catch (DateTimeParseException e) {
            // log se necessário
        }
        return null;
    }

    /**
     * Normaliza nome, removendo acentuação e caracteres especiais.
     */
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

    /**
     * Calcula similaridade entre duas strings pelo Levenshtein (0.0 a 1.0).
     */
    public static double similarity(String a, String b) {
        if (a == null || b == null) return 0;
        int dist = org.apache.commons.text.similarity.LevenshteinDistance
                .getDefaultInstance()
                .apply(a, b);
        return 1.0 - (double) dist / Math.max(a.length(), b.length());
    }
}
