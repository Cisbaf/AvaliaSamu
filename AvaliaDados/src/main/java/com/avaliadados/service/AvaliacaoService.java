package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.TypeAv;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.repository.SheetRowRepository;
import com.avaliadados.service.factory.AvaliacaoProcessor;
import com.avaliadados.service.utils.CollabParams;
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

import static com.avaliadados.service.utils.SheetsUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvaliacaoService implements AvaliacaoProcessor {

    private final CollaboratorRepository colaboradorRepository;
    private final ProjetoRepository projetoRepository;
    private final SheetRowRepository sheetRowRepository;
    private final CollabParams collabParams;

    // Constantes para as chaves de dados
    private static final String KEY_TEMPO_REGULACAO_TARM = "TEMPO_REGULACAO_TARM";
    private static final String KEY_TEMPO_REGULACAO_FROTA = "TEMPO_REGULACAO_FROTA";

    @Transactional
    public void processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {
        sheetRowRepository.deleteByProjectIdAndType(projectId, TypeAv.TARM_FROTA);

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> cols = getColumnMapping(sheet.getRow(0));
            log.info("Colunas disponíveis na sheet: {}", cols.keySet());

            Integer idxColab = encontrarIndiceColuna(cols, "COLABORADOR");
            Integer idxTarm = encontrarIndiceColuna(cols, "TEMPO REGULAÇÃO TARM", "TEMPO.REGULACAO.TARM");
            Integer idxFrota = encontrarIndiceColuna(cols, "OP. FROTA REGULAÇÃO MÉDICA", "TEMPO.REGULACAO.FROTA");

            log.info("Índices encontrados → COLAB: {}, TARM: {}, FROTA: {}", idxColab, idxTarm, idxFrota);

            if (idxColab == null) {
                log.error("Coluna de colaborador não encontrada na planilha");
                throw new RuntimeException("Coluna de colaborador não encontrada na planilha");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getCellStringValue(row, idxColab);
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

                if (id != null) {
                    sr.setCollaboratorId(id);
                }
                sr.setType(TypeAv.TARM_FROTA);
                sr.getData().put("COLABORADOR", name);

                // Salvar com múltiplas chaves para garantir compatibilidade
                if (tarmVal != null) {
                    sr.getData().put(KEY_TEMPO_REGULACAO_TARM, tarmVal);
                    sr.getData().put("TEMPO.REGULACAO.TARM", tarmVal);
                }

                if (frotaVal != null) {
                    sr.getData().put(KEY_TEMPO_REGULACAO_FROTA, frotaVal);
                    sr.getData().put("TEMPO.REGULACAO.FROTA", frotaVal);
                    sr.getData().put("OP FROTA REGULAO MDICA", frotaVal);
                }

                sheetRowRepository.save(sr);
                log.debug("  → salvou linha {}: {}", i, sr.getData());
            }
        }
        log.info("Planilha TARM/FROTA do projeto {} salva com {} linhas específicas", projectId,
                sheetRowRepository.findByProjectIdAndType(projectId, TypeAv.TARM_FROTA).size());

        atualizarColaboradoresDoProjeto(projectId);
    }

    private Integer encontrarIndiceColuna(Map<String, Integer> cols, String... possiveisNomes) {
        for (String nome : possiveisNomes) {
            // Busca exata
            if (cols.containsKey(nome)) {
                return cols.get(nome);
            }

            // Busca por substring
            Optional<Map.Entry<String, Integer>> coluna = cols.entrySet().stream()
                    .filter(e -> e.getKey().toUpperCase().contains(nome.toUpperCase()))
                    .findFirst();

            if (coluna.isPresent()) {
                return coluna.get().getValue();
            }
        }

        return null;
    }

    @Transactional
    public void atualizarColaboradoresDoProjeto(String projectId) {
        ProjetoEntity projeto = projetoRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        Map<String, CollaboratorEntity> colaboradores = colaboradorRepository.findAll().stream()
                .collect(Collectors.toMap(
                        c -> normalizeName(c.getNome()), c -> c, (a, b) -> a));

        List<SheetRow> rows = sheetRowRepository.findByProjectIdAndType(projectId, TypeAv.TARM_FROTA);
        log.info("Encontradas {} linhas de SheetRow para o projeto {}", rows.size(), projectId);

        for (SheetRow sr : rows) {
            String nomeNorm = normalizeName(sr.getData().get("COLABORADOR"));
            colaboradores.entrySet().stream()
                    .filter(e -> similarity(e.getKey(), nomeNorm) >= 0.85)
                    .max(Comparator.comparingDouble(e -> similarity(e.getKey(), nomeNorm)))
                    .ifPresent(match -> {
                        CollaboratorEntity colEnt = match.getValue();
                        log.info("Encontrado match para colaborador: {} -> {}", sr.getData().get("COLABORADOR"), colEnt.getNome());

                        projeto.getCollaborators().stream()
                                .filter(pc -> pc.getCollaboratorId().equals(colEnt.getId()))
                                .findFirst()
                                .ifPresent(pc -> {
                                    log.info("Atualizando dados do colaborador {} ({})", colEnt.getNome(), pc.getRole());
                                    atualizarDadosColaborador(pc, sr.getData(), projeto);
                                    log.info("Dados do SheetRow: {}", sr.getData());
                                });
                    });
        }
        projetoRepository.save(projeto);
        log.info("Colaboradores TARM/FROTA atualizados para projeto {}", projectId);
    }

    private void atualizarDadosColaborador(ProjectCollaborator pc,
                                           Map<String, String> data,
                                           ProjetoEntity projeto) {
        if (pc.getWasEdited()) return;


        NestedScoringParameters params = Optional.ofNullable(pc.getParametros())
                .orElseGet(() -> {
                    NestedScoringParameters np = new NestedScoringParameters();
                    pc.setParametros(np);
                    return np;
                });
        if (params.getTarm() == null) params.setTarm(new ScoringSectionParams());
        if (params.getFrota() == null) params.setFrota(new ScoringSectionParams());

        Map<String, List<String>> keyMap = new HashMap<>();
        keyMap.put("TARM", Arrays.asList(KEY_TEMPO_REGULACAO_TARM, "TEMPO.REGULACAO.TARM"));
        keyMap.put("FROTA", Arrays.asList(KEY_TEMPO_REGULACAO_FROTA, "TEMPO.REGULACAO.FROTA", "OP FROTA REGULAO MDICA"));

        List<String> possiveisChaves = keyMap.getOrDefault(pc.getRole(), Collections.emptyList());

        for (String chave : possiveisChaves) {
            if (data.containsKey(chave)) {
                Long secs = SheetsUtils.parseTimeToSeconds(data.get(chave));

                ScoringSectionParams section = pc.getRole().equals("TARM") ? params.getTarm() : params.getFrota();

                long existingPausaMensal = Optional.ofNullable(section.getPausas())
                        .filter(list -> !list.isEmpty())
                        .map(list -> list.getLast().getDuration())
                        .orElse(0L);
                int existingRemovidos = Optional.ofNullable(section.getRemovidos())
                        .filter(list -> !list.isEmpty())
                        .map(list -> list.getLast().getQuantity())
                        .orElse(0);
                Long existingSaidaVtr = Optional.ofNullable(section.getSaidaVtr())
                        .filter(list -> !list.isEmpty())
                        .map(list -> list.getLast().getDuration())
                        .orElse(0L);

                int pontos = collabParams.setParams(pc, projeto, secs,0L, existingRemovidos,  existingPausaMensal, existingSaidaVtr);
                pc.setPontuacao(pontos);
                pc.setDurationSeconds(secs);

            }
        }
    }
}
