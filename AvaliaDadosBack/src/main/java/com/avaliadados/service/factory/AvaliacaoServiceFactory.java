package com.avaliadados.service.factory;

import com.avaliadados.service.avaliacao.AvaliacaoService;
import com.avaliadados.service.avaliacao.AvaliacaoServiceMedico;
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
            DataFormatter formatter = new DataFormatter();

            Row primeiraLinha = sheet.getRow(0);
            Row segundaLinha  = sheet.getRow(1);

            // Célula [0][0]: "TOTAL DE PLANTÃO DE 12 HORAS" (novo formato)
            //             ou "COLABORADOR TEMPO RESPOSTA TARM E FROTA" (formato antigo)
            String valorCelula0 = formatter.formatCellValue(primeiraLinha.getCell(0)).trim().toUpperCase();

            // Célula [0][2]: "COLABORADOR" no novo formato
            String valorCelula2 = primeiraLinha.getCell(2) != null
                    ? formatter.formatCellValue(primeiraLinha.getCell(2)).trim().toUpperCase()
                    : "";

            String valor2 = segundaLinha != null && segundaLinha.getCell(0) != null
                    ? formatter.formatCellValue(segundaLinha.getCell(0)).trim().toUpperCase()
                    : "";

            // Formato antigo: linha 0 era o título centralizado
            // Formato novo:   linha 0 é o cabeçalho — célula[0]="TOTAL DE PLANTÃO DE 12 HORAS", célula[2]="COLABORADOR"
            if ("COLABORADOR TEMPO RESPOSTA TARM E FROTA".equals(valorCelula0)
                    || ("COLABORADOR".equals(valorCelula2) && valorCelula0.contains("PLANTÃO"))) {
                return avaliacaoService;
            }

            if ("MEDICO REGULADOR".equalsIgnoreCase(valorCelula0)
                    || "MEDICO REGULADOR".equalsIgnoreCase(valor2)) {
                return avaliacaoServiceMedico;
            }

            throw new IllegalArgumentException(
                    "Tipo de planilha não reconhecido: " + valorCelula0 + " / " + valor2
            );
        }
    }
}