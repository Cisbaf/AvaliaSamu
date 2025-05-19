package com.avaliadados.service;

import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.MedicoEntity;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.enums.TypeAv;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.MedicoEntityRepository;
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
import java.text.Normalizer;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvaliacaoServiceMedico implements AvaliacaoProcessor {

    private final ProjetoRepository projetoRepo;
    private final MedicoEntityRepository medicoRepo;
    private final SheetRowRepository sheetRowRepo;
    private final ScoringService scoringService;

    @Transactional
    public void processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {
        sheetRowRepo.deleteByProjectIdAndType(projectId, TypeAv.MEDICO);

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> cols = getColumnMapping(sheet.getRow(0));
            log.info("Colunas disponíveis na sheet: {}", cols.keySet());

            Integer idxMedicoReg = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("MEDICO REGULADOR"))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);

            Integer idxTempoMed = cols.entrySet().stream()
                    .filter(e -> e.getKey().contains("TEMPO MEDIO REGULACAO MEDICA"))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);

            Integer idxCriticos = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("CRITICOS"))
                    .map(Map.Entry::getValue)
                    .findFirst().orElse(null);

            log.info("Índices fuzzy → MEDICO_REGULADOR: {}, TEMPO_MED: {}, CRITICOS: {}",
                    idxMedicoReg, idxTempoMed, idxCriticos);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nomeMedico = idxMedicoReg != null ? getCellStringValue(row, idxMedicoReg) : null;
                String tempoReg = idxTempoMed    != null ? getCellStringValue(row, idxTempoMed)    : null;
                if (nomeMedico == null || tempoReg == null) {
                    log.debug("Descartando linha {}: nome='{}', tempo='{}'", i, nomeMedico, tempoReg);
                    continue;
                }

                SheetRow sr = new SheetRow();
                sr.setProjectId(projectId);
                sr.getData().put("MEDICO_REGULADOR", nomeMedico);
                sr.getData().put("TEMPO_REGULACAO", tempoReg);

                if (idxCriticos != null) {
                    String crit = getCellStringValue(row, idxCriticos);
                    if (crit != null) sr.getData().put("CRITICOS", crit);
                }

                sheetRowRepo.save(sr);
                log.debug("Salvou linha {} com dados: {}", i, sr.getData());
            }
        }

        log.info("Planilha médica do projeto {} salva com {} linhas específicas", projectId,
                sheetRowRepo.findByProjectId(projectId).size());

        sincronizarColaboradores(projectId);
    }

    @Transactional
    public void sincronizarColaboradores(String projectId) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        Map<String, MedicoEntity> medicos = medicoRepo.findAll().stream()
                .collect(Collectors.toMap(
                        m -> normalizarNome(m.getNome()),
                        m -> m,
                        (a, b) -> a
                ));

        for (SheetRow sr : sheetRowRepo.findByProjectId(projectId)) {
            String nomeNorm = normalizarNome(sr.getData().get("MEDICO_REGULADOR"));
            medicos.entrySet().stream()
                    .filter(e -> similaridade(e.getKey(), nomeNorm) >= 0.85)
                    .max(Comparator.comparingDouble(e -> similaridade(e.getKey(), nomeNorm)))
                    .ifPresent(match -> {
                        MedicoEntity med = match.getValue();
                        String collabId = med.getId();
                        ProjectCollaborator pc = projeto.getCollaborators().stream()
                                .filter(c -> c.getCollaboratorId().equals(collabId))
                                .findFirst()
                                .orElseGet(() -> {
                                    ProjectCollaborator novo = ProjectCollaborator.builder()
                                            .collaboratorId(collabId)
                                            .role(med.getMedicoRole().name())
                                            .build();
                                    projeto.getCollaborators().add(novo);
                                    return novo;
                                });
                        atualizarDadosMedico(pc, sr.getData(), med);
                    });
        }

        projetoRepo.save(projeto);
        log.info("Colaboradores sincronizados para projeto {}", projectId);
    }

    private void atualizarDadosMedico(ProjectCollaborator pc,
                                      Map<String, String> data,
                                      MedicoEntity med) {
        String tempoKey = "TEMPO_REGULACAO";
        String criticosKey = "CRITICOS";

        NestedScoringParameters params = pc.getParametros();
        if (params == null) params = new NestedScoringParameters();

        if (pc.getMedicoRole() == MedicoRole.REGULADOR && data.containsKey(tempoKey)) {
            Long secs = parseTimeToSeconds(data.get(tempoKey));
            pc.setDurationSeconds(secs);
            params.setMedico(ScoringSectionParams.builder()
                    .regulacao(List.of(ScoringRule.builder().duration(secs).build()))
                    .build());
            log.info("Regulação méd. {}: {}s", pc.getNome(), secs);
        }
        if (pc.getMedicoRole() == MedicoRole.LIDER && data.containsKey(criticosKey)) {
            Long crit = Long.parseLong(data.get(criticosKey));
            pc.setQuantity(crit.intValue());
            pc.setShiftHours(ShiftHours.H24);
            params.setMedico(ScoringSectionParams.builder()
                    .regulacaoLider(List.of(ScoringRule.builder().duration(crit).build()))
                    .build());
            log.info("Críticos méd. {}: {}", pc.getNome(), crit);
        }

        int pontos = scoringService.calculateCollaboratorScore(
                pc.getRole(), pc.getDurationSeconds(), pc.getQuantity(), pc.getPausaMensalSeconds(), params);
        pc.setPontuacao(pontos);
        pc.setParametros(params);
    }

    private Map<String, Integer> getColumnMapping(Row header) {
        Map<String, Integer> map = new HashMap<>();
        DataFormatter fmt = new DataFormatter();
        for (Cell c : header) {
            String raw = fmt.formatCellValue(c);
            String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                    .replaceAll("[^\\p{ASCII}]", "")
                    .replaceAll("[^A-Z0-9 ]", "")
                    .trim()
                    .toUpperCase();
            map.put(normalized, c.getColumnIndex());
        }
        log.info("Colunas normalizadas: {}", map.keySet());
        return map;
    }

    private String getCellStringValue(Row row, Integer idx) {
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return new DataFormatter().formatCellValue(cell);
        return null;
    }

    private Long parseTimeToSeconds(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            LocalTime lt = LocalTime.parse(s);
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
