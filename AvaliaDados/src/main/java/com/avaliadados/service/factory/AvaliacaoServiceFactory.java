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


    public AvaliacaoProcessor getProcessor(MultipartFile arquivo) throws IOException {
        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Row primeiraLinha = sheet.getRow(0);
            Row segundaLinha = sheet.getRow(1);
            Cell primeiraCelula = primeiraLinha.getCell(0);
            Cell segundaCelula = segundaLinha.getCell(0);
            String valor = new DataFormatter()
                    .formatCellValue(primeiraCelula)
                    .trim()
                    .toUpperCase();
            String valor2 = new DataFormatter()
                    .formatCellValue(segundaCelula)
                    .trim()
                    .toUpperCase();

            if ("TOTAL DE PLANTÃO DE 12 HORAS".equals(valor)) {
                return avaliacaoService;
            }
            if ("MEDICO REGULADOR".equalsIgnoreCase(valor) || "MEDICO REGULADOR".equalsIgnoreCase(valor2)) {
                return avaliacaoServiceMedico;
            }
            throw new IllegalArgumentException(
                    "Tipo de planilha não reconhecido: " + valor + " ou " + valor2
            );
        }
    }
}
