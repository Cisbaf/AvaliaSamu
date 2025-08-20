package com.avaliadados.service.avaliacao;

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

    private static final String KEY_TEMPO_REGULACAO_TARM = "TEMPO_REGULACAO_TARM";
    private static final String KEY_TEMPO_REGULACAO_FROTA = "TEMPO_REGULACAO_FROTA";


    @Transactional
    public List<String> processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {
        sheetRowRepository.deleteByProjectIdAndType(projectId, TypeAv.TARM_FROTA);

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> cols = getColumnMapping(sheet.getRow(0));

            Integer idxColab = encontrarIndiceColuna(cols, "COLABORADOR");
            Integer idxTarm = encontrarIndiceColuna(cols, "TEMPO REGULAÇÃO TARM", "TEMPO.REGULACAO.TARM");
            Integer idxFrota = encontrarIndiceColuna(cols, "OP. FROTA REGULAÇÃO MÉDICA", "TEMPO.REGULACAO.FROTA");
            Integer idxPlantao = encontrarIndiceColuna(cols, "TOTAL DE PLANTÃO DE 12 HORAS", "PLANTAO");

            List<SheetRow> srList = new ArrayList<>();

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
                String plantaoVal = idxPlantao != null ? getCellStringValue(row, idxPlantao) : null;

                if (name == null || (tarmVal == null && frotaVal == null)) {
                    continue;
                }

                SheetRow sr = new SheetRow();
                sr.setProjectId(projectId);

                sr.setType(TypeAv.TARM_FROTA);
                sr.getData().put("COLABORADOR", name);
                sr.getData().put("PLANTAO", plantaoVal != null ? plantaoVal : "0");

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
                srList.add(sr);

            }
            sheetRowRepository.saveAll(srList);
        }
        return !atualizarColaboradoresDoProjeto(projectId).isEmpty() ?
                atualizarColaboradoresDoProjeto(projectId) :
                List.of();
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
    public List<String> atualizarColaboradoresDoProjeto(String projectId) {
        ProjetoEntity projeto = projetoRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        List<ProjectCollaborator> tarmFrotaColabs = projeto.getCollaborators().stream()
                .filter(pc -> "TARM".equals(pc.getRole()) || "FROTA".equals(pc.getRole()))
                .toList();


        Map<String, ProjectCollaborator> colaboradores = tarmFrotaColabs.stream()
                .collect(Collectors.toMap(
                        pc -> normalizeName(pc.getNome()),
                        pc -> pc,
                        (a, b) -> a));

        List<SheetRow> rows = sheetRowRepository.findByProjectIdAndType(projectId, TypeAv.TARM_FROTA);

        List<ProjectCollaborator> pcsToUpdate = new ArrayList<>();
        List<String> idCallroutList = new ArrayList<>();

        // Lógica existente para atualizar colaboradores que estão na planilha
        for (SheetRow sr : rows) {
            String nomeNorm = normalizeName(sr.getData().get("COLABORADOR"));
            colaboradores.entrySet().stream()
                    .filter(e -> similarity(e.getKey(), nomeNorm) >= 0.85)
                    .max(Comparator.comparingDouble(e -> similarity(e.getKey(), nomeNorm)))
                    .ifPresent(match -> {
                        ProjectCollaborator colEnt = match.getValue();
                        projeto.getCollaborators().stream()
                                .filter(pc -> pc.getCollaboratorId().equals(colEnt.getCollaboratorId()))
                                .findFirst()
                                .ifPresent(pc -> {
                                    atualizarDadosColaborador(pc, sr.getData());
                                    pcsToUpdate.add(pc);
                                    idCallroutList.add(
                                            colaboradorRepository.getReferenceById(pc.getCollaboratorId()).getIdCallRote());
                                });
                    });
        }
        List<String> naoEncontrados = new ArrayList<>();

        // Nova lógica para identificar colaboradores TARM/FROTA que não estão na planilha
        log.info("\n--- Verificando colaboradores TARM/FROTA ausentes na planilha ---");
        for (ProjectCollaborator pc : tarmFrotaColabs) {
            String normalizedCollabName = normalizeName(pc.getNome());

            // Verifica se existe na planilha com similaridade >= 0.85
            boolean encontrado = rows.stream().anyMatch(sr ->
                    similarity(normalizeName(sr.getData().get("COLABORADOR")), normalizedCollabName) >= 0.75
            );

            if (!encontrado) {
                log.warn("Colaborador TARM/FROTA ausente na planilha: {}", pc.getNome());

                // Restante da lógica de matching...
                String bestMatchName = null;
                double highestScore = 0.0;
                for (SheetRow sr : rows) {
                    String sheetName = sr.getData().get("COLABORADOR");
                    double currentScore = similarity(normalizedCollabName, normalizeName(sheetName));
                    if (currentScore > highestScore) {
                        highestScore = currentScore;
                        bestMatchName = sheetName;
                    }
                }

                if (bestMatchName != null && highestScore >= 0.4) {
                    naoEncontrados.add(String.format("%s (possível correspondência: %s)", normalizedCollabName, bestMatchName));
                    log.warn("{} (possível correspondência: {})", normalizedCollabName, bestMatchName);
                } else {
                    naoEncontrados.add(normalizedCollabName + " (nenhuma correspondência próxima encontrada)");
                    log.warn("{} (nenhuma correspondência próxima encontrada)", normalizedCollabName);
                }
            }
            if (!pcsToUpdate.contains(pc)) {
                pcsToUpdate.add(pc);
                idCallroutList.add(
                        colaboradorRepository.getReferenceById(pc.getCollaboratorId()).getIdCallRote()
                );
            }
        }

        collabParams.setDataFromApi(pcsToUpdate, projeto, idCallroutList);


        for (ProjectCollaborator pc : pcsToUpdate) {
            if (pc.getPausaMensalSeconds() != null || pc.getDurationSeconds() != null) {
                int pontos = collabParams.setParams(
                        pc,
                        projeto,
                        pc.getRemovidos(),
                        pc.getRemovidosLider() != null ? pc.getRemovidosLider() : 0,
                        pc.getDurationSeconds(),
                        0L,
                        pc.getPausaMensalSeconds() != null ? pc.getPausaMensalSeconds() : 0L,
                        pc.getSaidaVtrSeconds());
                pc.setPontuacao(pontos);
            } else {
                pc.setPontuacao(0);
            }
        }

        projetoRepository.save(projeto);
        return naoEncontrados;
    }

    private void atualizarDadosColaborador(ProjectCollaborator pc,
                                           Map<String, String> data) {
        if (pc.getWasEdited()) return;

        var plantao = data.get("PLANTAO") != null ? data.get("PLANTAO") : "0";

        int plantaoQtd = (int) Math.round(Double.parseDouble(Objects.equals(plantao, "00:00:00") ? "0" : plantao));
        pc.setPlantao(plantaoQtd);


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

                Long existingSaidaVtr = Optional.ofNullable(section.getSaidaVtr())
                        .filter(list -> !list.isEmpty())
                        .map(list -> list.getLast().getDuration())
                        .orElse(0L);

                pc.setDurationSeconds(secs);
                pc.setSaidaVtrSeconds(existingSaidaVtr);
            }
        }
    }
}