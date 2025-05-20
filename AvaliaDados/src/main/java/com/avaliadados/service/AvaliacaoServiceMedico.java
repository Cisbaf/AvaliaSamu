package com.avaliadados.service;

import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.MedicoRole;
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
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.avaliadados.service.utils.SheetsUtils.*;

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
            var sheet = wb.getSheetAt(0);
            var cols = getColumnMapping(sheet.getRow(0));

            Integer idxMedReg = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("MEDICO REGULADOR"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            Integer idxTempoMed = cols.entrySet().stream()
                    .filter(e -> e.getKey().contains("TEMPO MEDIO REGULACAO MEDICA"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            Integer idxCrit = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("CRITICOS"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);

            log.info("Índices fuzzy → MEDICO_REGULADOR: {}, TEMPO_MED: {}, CRITICOS: {}",
                    idxMedReg, idxTempoMed, idxCrit);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) continue;
                String nomeMed = getCellStringValue(row, idxMedReg);
                String tempoReg = getCellStringValue(row, idxTempoMed);
                if (nomeMed == null || tempoReg == null) continue;

                SheetRow sr = new SheetRow();
                sr.setProjectId(projectId);
                sr.setType(TypeAv.MEDICO);
                sr.getData().put("MEDICO.REGULADOR", nomeMed);
                sr.getData().put("TEMPO.REGULACAO", tempoReg);
                if (idxCrit != null) {
                    var crit = getCellStringValue(row, idxCrit);
                    if (crit != null) sr.getData().put("CRITICOS", crit);
                }
                sheetRowRepo.save(sr);
            }
        }
        log.info("Planilha médica do projeto {} salva com {} linhas específicas", projectId,
                sheetRowRepo.findByProjectId(projectId).size());

        sincronizarColaboradores(projectId);
    }

    @Transactional
    public void sincronizarColaboradores(String projectId) {
        var projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        var medicos = medicoRepo.findAll().stream()
                .collect(Collectors.toMap(
                        m -> normalizeName(m.getNome()),
                        m -> m,
                        (a, b) -> a
                ));

        for (SheetRow sr : sheetRowRepo.findByProjectId(projectId)) {
            String rawNome = Optional.ofNullable(sr.getData().get("MEDICO.REGULADOR")).orElse("MEDICO.LIDER");
            String nomeNorm = normalizeName(rawNome);

            medicos.entrySet().stream()
                    .filter(e -> similarity(e.getKey(), nomeNorm) >= 0.85)
                    .max(Comparator.comparingDouble(e -> similarity(e.getKey(), nomeNorm)))
                    .ifPresent(entry -> {
                        var med = entry.getValue();
                        String collabId = med.getId();
                        ProjectCollaborator pc = projeto.getCollaborators().stream()
                                .filter(c -> c.getCollaboratorId().equals(collabId))
                                .findFirst()
                                .orElseGet(() -> {
                                    var novo = ProjectCollaborator.builder()
                                            .collaboratorId(collabId)
                                            .nome(med.getNome())
                                            .role(med.getRole())
                                            .medicoRole(med.getMedicoRole())
                                            .build();
                                    projeto.getCollaborators().add(novo);
                                    return novo;
                                });
                        if (pc.getMedicoRole() == null) pc.setMedicoRole(med.getMedicoRole());
                        atualizarDadosMedico(pc, sr.getData(), projeto);
                    });
        }
        projetoRepo.save(projeto);
        log.info("Colaboradores sincronizados para projeto {}", projectId);
    }

    private void atualizarDadosMedico(ProjectCollaborator pc,
                                      Map<String, String> data,
                                      ProjetoEntity projeto) {
        NestedScoringParameters params = pc.getParametros();
        if (params == null) params = new NestedScoringParameters();

        long regulacao = 0L;

        if (pc.getMedicoRole().equals(MedicoRole.REGULADOR) && data.containsKey("TEMPO.REGULACAO")) {
            Long secs = parseTimeToSeconds(data.get("TEMPO.REGULACAO"));
            pc.setDurationSeconds(secs);
              params.setMedico(ScoringSectionParams.builder().regulacao(List.of(ScoringRule.builder().duration(secs).build())).build());
            regulacao = params.getMedico().getRegulacao().getLast().getDuration();
        }
        if (pc.getMedicoRole().equals(MedicoRole.LIDER) && data.containsKey("CRITICOS")) {
            Long crit = parseTimeToSeconds(data.get("CRITICOS"));
            pc.setDurationSeconds(crit);
            params.setMedico(ScoringSectionParams.builder().regulacaoLider(List.of(ScoringRule.builder().duration(crit).build())).build());
            regulacao = params.getMedico().getRegulacaoLider().getLast().getDuration();
        }


        int pontos = scoringService.calculateCollaboratorScore(
                pc.getRole(),
                pc.getMedicoRole().name(),
                regulacao,
                params.getMedico().getRemovidos().getLast().getQuantity(),
                params.getMedico().getPausas().getLast().getDuration(),
                projeto.getParameters());

        pc.setPontuacao(pontos);
        pc.setParametros(params);
    }
}