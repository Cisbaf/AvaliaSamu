package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.TypeAv;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.repository.SheetRowRepository;
import com.avaliadados.service.factory.AvaliacaoProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.avaliadados.service.ProjectCollabService.convertMapToNested;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvaliacaoService implements AvaliacaoProcessor {

    private final CollaboratorRepository colaboradorRepository;
    private final ProjetoRepository projetoRepository;
    private final SheetRowRepository sheetRowRepository;
    private final ScoringService scoringService;

    @Transactional
    public void processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {
        // limpa apenas as linhas deste tipo
        sheetRowRepository.deleteByProjectIdAndType(projectId, TypeAv.TARM_FROTA);

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> cols = getColumnMapping(sheet.getRow(0));
            log.info("Colunas disponíveis na sheet: {}", cols.keySet());

            // índices fuzzy para COLABORADOR, TARM e FROTA
            Integer idxColab = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("COLABORADOR"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            Integer idxTarm = cols.entrySet().stream()
                    .filter(e -> e.getKey().contains("TEMPO_REGULACAO_TARM")
                            || e.getKey().contains("TARM"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            Integer idxFrota = cols.entrySet().stream()
                    .filter(e -> e.getKey().contains("FROTA"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);

            log.info("Índices fuzzy → COLAB: {}, TARM: {}, FROTA: {}", idxColab, idxTarm, idxFrota);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = idxColab != null ? getCellStringValue(row, idxColab) : null;
                String tarmVal = idxTarm != null ? getCellStringValue(row, idxTarm) : null;
                String frotaVal = idxFrota != null ? getCellStringValue(row, idxFrota) : null;
                log.debug("Linha {} → COLAB='{}', TARM='{}', FROTA='{}'", i, name, tarmVal, frotaVal);

                // somente se COLABORADOR existe e ao menos um valor de duração
                if (name == null || (tarmVal == null && frotaVal == null)) {
                    log.debug("  → descartando linha {}", i);
                    continue;
                }

                SheetRow sr = new SheetRow();
                sr.setProjectId(projectId);
                sr.setType(TypeAv.TARM_FROTA);
                sr.getData().put("COLABORADOR", name);
                if (tarmVal != null) sr.getData().put("TEMPO_REGULACAO_TARM", tarmVal);
                if (frotaVal != null) sr.getData().put("TEMPO_REGULACAO_FROTA", frotaVal);

                sheetRowRepository.save(sr);
                log.debug("  → salvou linha {}: {}", i, sr.getData());
            }
        }
        log.info("Planilha TARM/FROTA do projeto {} salva com {} linhas específicas", projectId,
                sheetRowRepository.findByProjectIdAndType(projectId, TypeAv.TARM_FROTA).size());

        // atualiza colaboradores com base nas linhas filtradas
        atualizarColaboradoresDoProjeto(projectId);
    }

    /**
     * Atualiza colaboradores no projeto com base nos dados filtrados (TARM/FROTA).
     */
    @Transactional
    public void atualizarColaboradoresDoProjeto(String projectId) {
        ProjetoEntity projeto = projetoRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        Map<String, CollaboratorEntity> colaboradores = colaboradorRepository.findAll().stream()
                .collect(Collectors.toMap(
                        c -> normalizarNome(c.getNome()), c -> c, (a, b) -> a));

        List<SheetRow> rows = sheetRowRepository.findByProjectIdAndType(projectId, TypeAv.TARM_FROTA);
        for (SheetRow sr : rows) {
            String nomeNorm = normalizarNome(sr.getData().get("COLABORADOR"));
            colaboradores.entrySet().stream()
                    .filter(e -> similaridade(e.getKey(), nomeNorm) >= 0.85)
                    .max(Comparator.comparingDouble(e -> similaridade(e.getKey(), nomeNorm)))
                    .ifPresent(match -> {
                        CollaboratorEntity colEnt = match.getValue();
                        projeto.getCollaborators().stream()
                                .filter(pc -> pc.getCollaboratorId().equals(colEnt.getId()))
                                .findFirst()
                                .ifPresent(pc -> atualizarDadosColaborador(pc, sr.getData(), colEnt));
                    });
        }
        projetoRepository.save(projeto);
        log.info("Colaboradores TARM/FROTA atualizados para projeto {}", projectId);
    }

    private void atualizarDadosColaborador(ProjectCollaborator pc,
                                           Map<String, String> data,
                                           CollaboratorEntity collab) {
        Map<String, String> map = Map.of(
                "TARM", "TEMPO_REGULACAO_TARM",
                "FROTA", "TEMPO_REGULACAO_FROTA"
        );
        String colKey = map.get(pc.getRole());
        if (colKey != null && data.containsKey(colKey)) {
            Long secs = parseTimeToSeconds(data.get(colKey));
            pc.setDurationSeconds(secs);
            log.info("Duração para {} ({}): {}s", collab.getNome(), pc.getRole(), secs);
        }
        NestedScoringParameters params = pc.getParametros();
        if (params == null) params = convertMapToNested(collab.getParametros());
        if (params.getTarm() == null) params.setTarm(new ScoringSectionParams());
        if (params.getFrota() == null) params.setFrota(new ScoringSectionParams());

        int pontos = scoringService.calculateCollaboratorScore(
                pc.getRole(), pc.getDurationSeconds(), pc.getQuantity(), pc.getPausaMensalSeconds(), params);
        pc.setPontuacao(pontos);
        pc.setParametros(params);
    }

    private Map<String, Integer> getColumnMapping(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        DataFormatter fmt = new DataFormatter();
        for (Cell cell : headerRow) {
            String raw = fmt.formatCellValue(cell);
            String normalized = raw.trim().toUpperCase().replaceAll("[^A-Z0-9 ]", "");
            columnMap.put(normalized, cell.getColumnIndex());
        }
        return columnMap;
    }

    private String getCellStringValue(Row row, Integer colIndex) {
        if (colIndex == null) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return new DataFormatter().formatCellValue(cell);
        return null;
    }

    private Long parseTimeToSeconds(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            LocalTime lt = LocalTime.parse(s.length() == 5 ? s + ":00" : s);
            return (long) lt.toSecondOfDay();
        } catch (Exception e) {
            log.error("Erro convertendo tempo '{}': {}", s, e.getMessage());
            return null;
        }
    }

    private String normalizarNome(String nome) {
        return nome == null ? null : nome.trim().toUpperCase().replaceAll("[^A-Z0-9 ]", "");
    }

    private double similaridade(String a, String b) {
        if (a == null || b == null) return 0;
        int dist = LevenshteinDistance.getDefaultInstance().apply(a, b);
        return 1.0 - (double) dist / Math.max(a.length(), b.length());
    }
}