package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.enums.TypeAv;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.MedicoEntityRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.repository.SheetRowRepository;
import com.avaliadados.service.factory.AvaliacaoProcessor;
import com.avaliadados.service.utils.CollabParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
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
    private final CollabParams collabParams;
    private final CollaboratorRepository colaboradorRepository;


    @Transactional
    public void processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {
        sheetRowRepo.deleteByProjectIdAndType(projectId, TypeAv.MEDICO);

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            var sheet = wb.getSheetAt(0);
            var cols = getColumnMapping(sheet.getRow(0));

            if (cols.entrySet().stream().noneMatch(e -> e.getKey().startsWith("MEDICO REGULADOR"))){
                cols = getColumnMapping(sheet.getRow(1));
            }

            Integer idxMedReg = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("MEDICO REGULADOR"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            Integer idxTempoMed = cols.entrySet().stream()
                    .filter(e -> e.getKey().contains("TEMPO MEDIO REGULAÇÃO MEDICA"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            Integer idxCrit = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("CRITICOS"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);

            log.info("Índices fuzzy → MEDICO_REGULADOR: {}, TEMPO_MED: {}, CRITICOS: {}",
                    idxMedReg, idxTempoMed, idxCrit);
            log.info("Índices sheets : {}", cols.keySet());

            String nomeMed = "";
            String tempoReg = "";

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) continue;
                if (idxMedReg != null) {
                    nomeMed = getCellStringValue(row, idxMedReg);
                }
                if (idxTempoMed != null) {
                    tempoReg = getCellStringValue(row, idxTempoMed);
                }
                if (nomeMed == null || tempoReg == null) continue;

                var id = colaboradorRepository.findByNome(nomeMed).map(CollaboratorEntity::getId).orElse(null);

                SheetRow sr = new SheetRow();
                sr.setProjectId(projectId);
                if (id != null) {
                    sr.setCollaboratorId(id);
                }
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

        if (pc.getWasEdited()) {
            return;
        }

        long duration = 0L;
        int quantity = 0;
        long pausaMensal = 0L;

        MedicoRole medicoRole = pc.getMedicoRole();

        if (medicoRole == MedicoRole.REGULADOR && data.containsKey("TEMPO.REGULACAO")) {
            duration = parseTimeToSeconds(data.get("TEMPO.REGULACAO"));
        } else if (medicoRole == MedicoRole.LIDER && data.containsKey("CRITICOS")) {
            duration = parseTimeToSeconds(data.get("CRITICOS"));
        }

        if (data.containsKey("REMOVIDOS")) {
            quantity = Integer.parseInt(data.get("REMOVIDOS"));
        }

        if (data.containsKey("PAUSAS")) {
            pausaMensal = parseTimeToSeconds(data.get("PAUSAS"));
        }

        if (pc.getShiftHours() == null) {
            pc.setShiftHours(ShiftHours.H12);
            log.info("Definindo turno padrão H12 para colaborador {}", pc.getNome());
        }

        pc.setDurationSeconds(duration);

        int pontos = collabParams.setParams(pc, projeto, duration, quantity, pausaMensal, 0L);

        pc.setPontuacao(pontos);
    }

}