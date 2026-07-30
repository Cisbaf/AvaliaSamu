package com.avaliadados.service.utils;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ponto unico para abrir um MultipartFile como Workbook.
 * Trata tanto arquivos Excel binarios/OOXML reais quanto o "falso Excel"
 * (HTML/MHTML exportado por sistemas web, comum em relatorios de despacho, WPS Office etc.)
 * que vem com extensao .xls mas cujo conteudo e HTML.
 *
 * Qualquer servico que precise abrir a planilha enviada pelo usuario deve
 * usar esta classe, para nao duplicar (e desalinhar) a logica de leitura.
 */
public final class WorkbookReader {

    private static final String NBSP = " ";
    private static final String ZERO_WIDTH_SPACE = "​";

    private WorkbookReader() {
    }

    public static Workbook read(MultipartFile arquivo) throws IOException {
        byte[] bytes = arquivo.getBytes();
        String textContent = tryReadTextContent(bytes);

        if (looksLikeHtmlSpreadsheet(textContent)) {
            return createWorkbookFromHtml(textContent);
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            return WorkbookFactory.create(inputStream);
        } catch (Exception ex) {
            try (ByteArrayInputStream fallbackStream = new ByteArrayInputStream(bytes)) {
                return new HSSFWorkbook(fallbackStream);
            } catch (Exception fallbackEx) {
                throw new IllegalArgumentException("Nao foi possivel ler o arquivo enviado.", fallbackEx);
            }
        }
    }

    private static String tryReadTextContent(byte[] bytes) {
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }

    private static boolean looksLikeHtmlSpreadsheet(String textContent) {
        if (textContent == null || textContent.isBlank()) {
            return false;
        }
        String lower = textContent.toLowerCase(Locale.ROOT);
        return lower.contains("<html") || lower.contains("<table") || lower.contains("<tr") || lower.contains("<td") || lower.contains("<!doctype html");
    }

    private static Workbook createWorkbookFromHtml(String htmlContent) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Planilha");

        String cleaned = htmlContent.replaceAll("(?s)<!--.*?-->", " ");
        Pattern rowPattern = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher rowMatcher = rowPattern.matcher(cleaned);

        int rowIndex = 0;
        while (rowMatcher.find()) {
            String rowHtml = rowMatcher.group(1);
            List<String> values = extractCellValues(rowHtml);
            if (values.isEmpty()) {
                continue;
            }

            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < values.size(); i++) {
                row.createCell(i).setCellValue(values.get(i));
            }
        }

        return workbook;
    }

    private static List<String> extractCellValues(String rowHtml) {
        List<String> values = new ArrayList<>();
        Pattern cellPattern = Pattern.compile("<(?:td|th)[^>]*>(.*?)</(?:td|th)>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = cellPattern.matcher(rowHtml);
        while (matcher.find()) {
            String rawValue = matcher.group(1);
            String value = StringEscapeUtils.unescapeHtml4(rawValue)
                    .replaceAll("<[^>]+>", " ")
                    .replace(NBSP, " ")
                    .replace(ZERO_WIDTH_SPACE, " ")
                    .replace('\r', ' ')
                    .replace('\n', ' ')
                    .replace('\t', ' ');
            values.add(value.replaceAll("\\s+", " ").trim());
        }
        return values;
    }
}
