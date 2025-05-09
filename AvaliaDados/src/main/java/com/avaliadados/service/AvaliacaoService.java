package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvaliacaoService {

    private final CollaboratorRepository colaboradorRepository;
    private final ProjetoRepository projetoRepository;
    private final ScoringService scoringService;


    public void processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {
        ProjetoEntity projeto = projetoRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        Map<String, CollaboratorEntity> colaboradores = colaboradorRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        c -> normalizarNome(c.getNome()),
                        c -> c,
                        (a, b) -> a
                ));

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> cols = getColumnMapping(sheet.getRow(0));

            Map<String, Integer> paramsProjeto = Optional.ofNullable(projeto.getParameters())
                    .orElseGet(HashMap::new);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                processarLinhaPlanilha(projeto, colaboradores, cols, paramsProjeto, row);
            }
        }

        projetoRepository.save(projeto);
    }

    private void processarLinhaPlanilha(ProjetoEntity projeto,
                                        Map<String, CollaboratorEntity> colaboradores,
                                        Map<String, Integer> cols,
                                        Map<String, Integer> paramsProjeto,
                                        Row row) {
        String nomePlanilha = normalizarNome(getCellStringValue(row, cols.get("COLABORADOR")));
        if (nomePlanilha == null) return;

        colaboradores.entrySet().stream()
                .filter(e -> calcularSimilaridade(e.getKey(), nomePlanilha) > 0.7)
                .max(Comparator.comparingDouble(e -> calcularSimilaridade(e.getKey(), nomePlanilha)))
                .ifPresent(match -> {
                    CollaboratorEntity colEnt = match.getValue();
                    String colaboratorId = colEnt.getId();

                    projeto.getCollaborators().stream()
                            .filter(pc -> pc.getCollaboratorId().equals(colaboratorId))
                            .findFirst()
                            .ifPresent(pc -> atualizarDadosColaborador(pc, row, cols, paramsProjeto));
                });
    }

    private void atualizarDadosColaborador(ProjectCollaborator pc,
                                           Row row,
                                           Map<String, Integer> cols,
                                           Map<String, Integer> paramsProjeto) {
        // Atualiza durações e quantidades
        if (cols.containsKey("TEMPO REGULAÇÃO TARM")) {
            Long durationSeconds = getCellTimeInSeconds(row.getCell(cols.get("TEMPO REGULAÇÃO TARM")));
            pc.setDurationSeconds(durationSeconds);
        }

        if (cols.containsKey("QUANTIDADE TARM")) {
            Integer quantity = getSafeIntValue(row.getCell(cols.get("QUANTIDADE TARM")));
            pc.setQuantity(quantity);
        }

        // Calcula pontos
        int pontos = calcularPontuacao(pc, paramsProjeto);
        pc.setPontuacao(pontos);
    }

    private int calcularPontuacao(ProjectCollaborator pc, Map<String, Integer> paramsProjeto) {
        Duration duracao = pc.getDurationSeconds() != null ? Duration.ofSeconds(pc.getDurationSeconds()) : null;
        Duration pausaMensal = pc.getPausaMensalSeconds() != null ? Duration.ofSeconds(pc.getPausaMensalSeconds()) : null;

        return scoringService.calculateScore(
                pc.getRole(),
                duracao,
                pc.getQuantity(),
                pausaMensal,
                paramsProjeto
        );
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

    private Integer getSafeIntValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    double value = cell.getNumericCellValue();
                    if (value > Integer.MAX_VALUE) {
                        log.warn("Valor {} excede Integer.MAX_VALUE", value);
                        return Integer.MAX_VALUE;
                    }
                    if (value < Integer.MIN_VALUE) {
                        log.warn("Valor {} menor que Integer.MIN_VALUE", value);
                        return Integer.MIN_VALUE;
                    }
                    return (int) Math.round(value);
                case STRING:
                    try {
                        return Integer.parseInt(cell.getStringCellValue().trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            log.error("Erro ao converter célula para inteiro", e);
            return null;
        }
    }

    private Long getCellTimeInSeconds(Cell cell) {
        if (cell == null) return null;

        try {
            if (cell.getCellType() == CellType.STRING) {
                String timeString = cell.getStringCellValue().trim();
                LocalTime time = LocalTime.parse(timeString);
                return (long) time.toSecondOfDay();
            } else if (cell.getCellType() == CellType.NUMERIC) {
                double excelTime = cell.getNumericCellValue();
                return Math.round(excelTime * 24 * 3600);
            }
        } catch (Exception e) {
            log.error("Erro ao converter tempo da célula", e);
        }
        return null;
    }


}