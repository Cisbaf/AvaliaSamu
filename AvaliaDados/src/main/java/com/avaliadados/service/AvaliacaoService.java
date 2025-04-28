package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.TarmEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.FrotaRepository;
import com.avaliadados.repository.TarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvaliacaoService {
    private final TarmRepository tarmRepository;
    private final FrotaRepository frotaRepository;
    private final CollaboratorRepository colaboradorRepository;

    public void processarPlanilha(MultipartFile arquivo) throws IOException {
        // Busca todos os colaboradores do banco
        Map<String, CollaboratorEntity> colaboradores = colaboradorRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        c -> normalizarNome(c.getNome()),
                        c -> c,
                        (existing, replacement) -> existing
                ));

        try (Workbook workbook = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Mapear colunas
            Map<String, Integer> colunas = getColumnMapping(headerRow);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nomePlanilha = normalizarNome(getCellStringValue(row, colunas.get("COLABORADOR")));
                if (nomePlanilha == null || nomePlanilha.isEmpty()) continue;

                // Encontrar melhor correspondência
                Optional<Map.Entry<String, CollaboratorEntity>> melhorMatch = colaboradores.entrySet()
                        .stream()
                        .filter(e -> calcularSimilaridade(e.getKey(), nomePlanilha) > 0.7)
                        .max(Comparator.comparingDouble(e -> calcularSimilaridade(e.getKey(), nomePlanilha)));

                if (melhorMatch.isPresent()) {
                    CollaboratorEntity colaborador = melhorMatch.get().getValue();
                    // Atualizar TARM
                    if (colaborador instanceof TarmEntity tarm) {
                        tarm.setTempoRegulaco(convertToLocalTime(row.getCell(colunas.get("TEMPO REGULAÇÃO TARM"))));
                        tarmRepository.save(tarm);
                    }
                    // Atualizar Frota
                    if (colaborador instanceof FrotaEntity frota) {
                        frota.setRegulacaoMedica(convertToLocalTime(row.getCell(colunas.get("OP. FROTA REGULAÇÃO MÉDICA"))));
                        frotaRepository.save(frota);
                    }
                }
            }
        }
    }
    private Map<String, Integer> getColumnMapping(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        for (Cell cell : headerRow) {
            columnMap.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
        }
        return columnMap;
    }

    private String normalizarNome(String nome) {
        return nome != null ? nome.trim().toUpperCase().replaceAll("[^A-Z ]", "") : null;
    }

    private double calcularSimilaridade(String nome1, String nome2) {
        String n1 = normalizarNome(nome1);
        String n2 = normalizarNome(nome2);
        int distancia = LevenshteinDistance.getDefaultInstance().apply(n1, n2);
        return 1 - (double) distancia / Math.max(n1.length(), n2.length());
    }

    private String getCellStringValue(Row row, Integer colIndex) {
        if (colIndex == null || row.getCell(colIndex) == null) return null;
        return row.getCell(colIndex).getStringCellValue();
    }

    private LocalTime convertToLocalTime(Cell cell) {
        if (cell == null) return null;

        try {
            return LocalTime.parse(cell.getStringCellValue());
        } catch (Exception e1) {
            try {
                double num = cell.getNumericCellValue();
                int hours = (int) (num * 24);
                int minutes = (int) ((num * 24 * 60) % 60);
                return LocalTime.of(hours, minutes);
            } catch (Exception e2) {
                return null;
            }
        }
    }


}