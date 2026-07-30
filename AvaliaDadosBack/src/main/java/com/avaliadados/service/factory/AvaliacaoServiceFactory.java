package com.avaliadados.service.factory;

import com.avaliadados.service.avaliacao.AvaliacaoService;
import com.avaliadados.service.avaliacao.AvaliacaoServiceMedico;
import com.avaliadados.service.utils.WorkbookReader;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AvaliacaoServiceFactory {

    private final AvaliacaoService avaliacaoService;
    private final AvaliacaoServiceMedico avaliacaoServiceMedico;

    public AvaliacaoProcessor getProcessor(MultipartFile arquivo) throws IOException {
        try (Workbook wb = WorkbookReader.read(arquivo)) {
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet sheet = wb.getSheetAt(i);
                Row primeiraLinha = sheet.getRow(0);
                Row segundaLinha = sheet.getRow(1);

                String valorCelula0 = readHeaderValue(primeiraLinha, 0, formatter, evaluator);
                String valorCelula2 = readHeaderValue(primeiraLinha, 2, formatter, evaluator);
                String valor2 = readHeaderValue(segundaLinha, 0, formatter, evaluator);

                String normalized0 = normalizeHeader(valorCelula0);
                String normalized2 = normalizeHeader(valorCelula2);
                String normalized2Row = normalizeHeader(valor2);

                boolean isMedicoTitle = normalized0.contains("MEDICO") && normalized0.contains("REGULADOR");
                boolean isMedicoHeaderRow = normalized2Row.contains("MEDICO") && normalized2Row.contains("REGULADOR");
                boolean isMedico = isMedicoTitle || isMedicoHeaderRow || normalized0.contains("MEDICO REGULADOR") || normalized2Row.contains("MEDICO REGULADOR");

                if (isMedico) {
                    return avaliacaoServiceMedico;
                }

                boolean hasCollaboratorHeader = normalized0.contains("COLABORADOR")
                        || normalized2.contains("COLABORADOR")
                        || normalized2Row.contains("COLABORADOR");
                boolean isTarmAntigo = normalized0.contains("COLABORADOR TEMPO RESPOSTA TARM E FROTA");
                boolean isTarmNovo = normalized0.contains("TOTAL DE PLANTAO")
                        || normalized0.contains("PLANTAO")
                        || normalized2.contains("PLANTAO")
                        || normalized2Row.contains("PLANTAO");
                boolean isFallbackTarm = normalized2.equals("COLABORADOR") || normalized2Row.equals("COLABORADOR");

                if (hasCollaboratorHeader || isTarmAntigo || isTarmNovo || isFallbackTarm) {
                    return avaliacaoService;
                }
            }

            throw new IllegalArgumentException(
                    "Tipo de planilha nao reconhecido em nenhuma das abas. Envie um arquivo Excel .xls ou .xlsx valido."
            );
        }
    }

    private String readHeaderValue(Row row, int cellIndex, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null) return "";
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return "";
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    private String normalizeHeader(String value) {
        if (value == null) return "";
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
