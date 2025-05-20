package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.TypeAv;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.repository.SheetRowRepository;
import com.avaliadados.service.factory.AvaliacaoProcessor;
import com.avaliadados.service.utils.SheetsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.avaliadados.service.ProjectCollabService.convertMapToNested;
import static com.avaliadados.service.utils.SheetsUtils.*;

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
        sheetRowRepository.deleteByProjectIdAndType(projectId, TypeAv.TARM_FROTA);

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> cols = getColumnMapping(sheet.getRow(0));
            log.info("Colunas disponíveis na sheet: {}", cols.keySet());

            Integer idxColab = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("COLABORADOR"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            Integer idxTarm = cols.entrySet().stream()
                    .filter(e -> e.getKey().contains("TEMPO REGULAO TARM")
                            || e.getKey().contains("TARM"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            Integer idxFrota = cols.entrySet().stream()
                    .filter(e -> e.getKey().contains("OP FROTA REGULAO MDICA"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);

            log.info("Índices fuzzy → COLAB: {}, TARM: {}, FROTA: {}", idxColab, idxTarm, idxFrota);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = idxColab != null ? getCellStringValue(row, idxColab) : null;
                String tarmVal = idxTarm != null ? getCellStringValue(row, idxTarm) : null;
                String frotaVal = idxFrota != null ? getCellStringValue(row, idxFrota) : null;
                log.debug("Linha {} → COLAB='{}', TARM='{}', FROTA='{}'", i, name, tarmVal, frotaVal);

                if (name == null || (tarmVal == null && frotaVal == null)) {
                    log.debug("  → descartando linha {}", i);
                    continue;
                }

                var id = colaboradorRepository.findByNome(name).map(CollaboratorEntity::getId).orElse(null);


                SheetRow sr = new SheetRow();
                sr.setProjectId(projectId);

                if (id != null) {sr.setCollaboratorId(id);}
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

        atualizarColaboradoresDoProjeto(projectId);
    }

    @Transactional
    public void atualizarColaboradoresDoProjeto(String projectId) {
        ProjetoEntity projeto = projetoRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));


        Map<String, CollaboratorEntity> colaboradores = colaboradorRepository.findAll().stream()
                .collect(Collectors.toMap(
                        c -> normalizeName(c.getNome()), c -> c, (a, b) -> a));

        List<SheetRow> rows = sheetRowRepository.findByProjectIdAndType(projectId, TypeAv.TARM_FROTA);
        for (SheetRow sr : rows) {
            String nomeNorm = normalizeName(sr.getData().get("COLABORADOR"));
            colaboradores.entrySet().stream()
                    .filter(e -> similarity(e.getKey(), nomeNorm) >= 0.85)
                    .max(Comparator.comparingDouble(e -> similarity(e.getKey(), nomeNorm)))
                    .ifPresent(match -> {
                        CollaboratorEntity colEnt = match.getValue();
                        projeto.getCollaborators().stream()
                                .filter(pc -> pc.getCollaboratorId().equals(colEnt.getId()))
                                .findFirst()
                                .ifPresent(pc -> atualizarDadosColaborador(pc, sr.getData(), colEnt, projeto));
                    });
        }
        projetoRepository.save(projeto);
        log.info("Colaboradores TARM/FROTA atualizados para projeto {}", projectId);
    }

    private void atualizarDadosColaborador(ProjectCollaborator pc,
                                           Map<String, String> data,
                                           CollaboratorEntity collab,
                                           ProjetoEntity projeto) {

        NestedScoringParameters params = Optional.ofNullable(pc.getParametros())
                .orElseGet(() -> {
                    NestedScoringParameters np = convertMapToNested(collab.getParametros());

                    pc.setParametros(np);
                    return np;
                });
        if (params.getTarm() == null) params.setTarm(new ScoringSectionParams());
        if (params.getFrota() == null) params.setFrota(new ScoringSectionParams());

        Map<String, String> map = Map.of(
                "TARM", "TEMPO.REGULACAO.TARM",
                "FROTA", "TEMPO.REGULACAO.FROTA"
        );
        String colKey = map.get(pc.getRole());
        if (colKey != null && data.containsKey(colKey)) {
            Long secs = SheetsUtils.parseTimeToSeconds(data.get(colKey));

            ScoringRule rule = ScoringRule.builder()
                    .duration(secs)
                    .build();

            ScoringSectionParams section = pc.getRole().equals("TARM")
                    ? params.getTarm()
                    : params.getFrota();

            if (section.getRegulacao() == null) {
                section.setRegulacao(new ArrayList<>());
            }
            section.getRegulacao().add(rule);

            log.info("Adicionada regra de duração para {} ({}): {}s",
                    collab.getNome(), pc.getRole(), secs);
        }

        long lastDuration = Optional.ofNullable(
                        "TARM".equals(pc.getRole())
                                ? params.getTarm()
                                : params.getFrota()
                )
                .map(ScoringSectionParams::getRegulacao)
                .filter(l -> !l.isEmpty())
                .map(l -> l.getLast().getDuration())
                .orElse(0L);
        long lastPausas = Optional.ofNullable(
                        "TARM".equals(pc.getRole())
                                ? params.getTarm()
                                : params.getFrota()
                )
                .map(ScoringSectionParams::getPausas)
                .filter(l -> !l.isEmpty())
                .map(l -> l.getLast().getDuration())
                .orElse(0L);
        int lastRemovidos = Optional.ofNullable(
                        "TARM".equals(pc.getRole())
                                ? params.getTarm()
                                : params.getFrota()
                )
                .map(ScoringSectionParams::getRemovidos)
                .filter(list -> !list.isEmpty())
                .map(list -> list.getLast().getQuantity())
                .orElse(0);

        int pontos = scoringService.calculateCollaboratorScore(
                pc.getRole(),
                null,
                lastDuration,
                lastRemovidos,
                lastPausas,
                projeto.getParameters()
        );
        pc.setPontuacao(pontos);
    }


}