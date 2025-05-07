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
            Map<String, Integer> colunas = getColumnMapping(headerRow);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nomePlanilha = normalizarNome(getCellStringValue(row, colunas.get("COLABORADOR")));
                if (nomePlanilha == null || nomePlanilha.isEmpty()) continue;

                Optional<Map.Entry<String, CollaboratorEntity>> melhorMatch = colaboradores.entrySet()
                        .stream()
                        .filter(e -> calcularSimilaridade(e.getKey(), nomePlanilha) > 0.7)
                        .max(Comparator.comparingDouble(e -> calcularSimilaridade(e.getKey(), nomePlanilha)));

                if (melhorMatch.isPresent()) {
                    CollaboratorEntity colaborador = melhorMatch.get().getValue();
                    Map<String, Double> parametros = colaborador.getParametros();
                    if (parametros == null) {
                        parametros = new HashMap<>();
                    }

                    // Atualiza os parâmetros conforme os papéis disponíveis na planilha
                    if (colunas.containsKey("TEMPO REGULAÇÃO TARM")) {
                        parametros.put("TARM_tempoRegulacao", convertToDecimalHours(row.getCell(colunas.get("TEMPO REGULAÇÃO TARM"))));
                    }

                    if (colunas.containsKey("PONTOS TARM")) {
                        parametros.put("TARM_pontuacao", getCellDoubleValue(row, colunas.get("PONTOS TARM")));
                    }

                    if (colunas.containsKey("OP. FROTA REGULAÇÃO MÉDICA")) {
                        parametros.put("FROTA_regulacaoMedica", convertToDecimalHours(row.getCell(colunas.get("OP. FROTA REGULAÇÃO MÉDICA"))));
                    }

                    if (colunas.containsKey("PONTOS FROTA")) {
                        parametros.put("FROTA_pontuacao", getCellDoubleValue(row, colunas.get("PONTOS FROTA")));
                    }

                    colaborador.setParametros(parametros);
                    colaboradorRepository.save(colaborador);
                }
            }
        }
    }

    private Double convertToDecimalHours(Cell cell) {
        if (cell == null) return null;

        try {
            String timeString = cell.getStringCellValue();
            LocalTime time = LocalTime.parse(timeString);
            return time.getHour() + time.getMinute() / 60.0;
        } catch (Exception e1) {
            try {
                double num = cell.getNumericCellValue(); // Excel time as fraction of day
                return num * 24;
            } catch (Exception e2) {
                return null;
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

    private Double getCellDoubleValue(Row row, Integer colIndex) {
        if (colIndex == null || row.getCell(colIndex) == null) return null;
        Cell cell = row.getCell(colIndex);
        try {
            return cell.getNumericCellValue();
        } catch (Exception e) {
            return null; // Handle non-numeric values as null or throw an exception
        }
    }


}