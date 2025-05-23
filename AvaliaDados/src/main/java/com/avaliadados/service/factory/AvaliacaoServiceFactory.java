package com.avaliadados.service.factory;

import com.avaliadados.service.AvaliacaoService;
import com.avaliadados.service.AvaliacaoServiceMedico;
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

    /**
     * Decide o processor com base no valor da primeira célula (coluna 0) da planilha.
     */
    public AvaliacaoProcessor getProcessor(MultipartFile arquivo) throws IOException {
        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Row primeiraLinha = sheet.getRow(0);
            Cell primeiraCelula = primeiraLinha.getCell(0);
            String valor = new DataFormatter()
                    .formatCellValue(primeiraCelula)
                    .trim()
                    .toUpperCase();

            if ("TOTAL DE PLANTÃO DE 12 HORAS".equals(valor)) {
                return avaliacaoService;
            }
            if ("MEDICO REGULADOR".equals(valor)) {
                return avaliacaoServiceMedico;
            }

            throw new IllegalArgumentException(
                    "Tipo de planilha não reconhecido: " + valor
            );
        }
    }
}
