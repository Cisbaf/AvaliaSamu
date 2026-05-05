package com.avaliadados.service.avaliacao;

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
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.avaliadados.service.utils.SheetsUtils.*;

@Service
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

        Map<String, Map<String, Object>> consolidatedData = new HashMap<>();

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            // FIX: cabeçalho agora está na linha 0 (novo formato)
            Row headerRow = sheet.getRow(0);
            Map<String, Integer> cols = getColumnMapping(headerRow);

            Integer idxColab   = encontrarIndiceColuna(cols, "COLABORADOR");
            Integer idxTarm    = encontrarIndiceColuna(cols, "TEMPO REGULAÇÃO TARM");
            Integer idxFrota   = encontrarIndiceColuna(cols, "OP. FROTA REGULAÇÃO MÉDICA");
            Integer idxPlantao = encontrarIndiceColuna(cols, "TOTAL DE PLANTÃO DE 12 HORAS");

            if (idxColab == null) {
                throw new RuntimeException("Coluna de colaborador não encontrada na planilha");
            }

            // FIX: começa na linha 2 — pula cabeçalho (0) e linha de totais (1)
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String name = getCellStringValue(row, idxColab);
                if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("nan")) continue;

                double plantao = 0;
                if (idxPlantao != null) {
                    plantao = getNumericValue(row.getCell(idxPlantao));
                }

                long tarmSecs = 0;
                if (idxTarm != null) {
                    tarmSecs = parseCellToSeconds(row.getCell(idxTarm));
                }

                long frotaSecs = 0;
                if (idxFrota != null) {
                    frotaSecs = parseCellToSeconds(row.getCell(idxFrota));
                }

                consolidar(consolidatedData, name.trim(), plantao, tarmSecs, frotaSecs);
            }
        }

        List<SheetRow> srList = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> entry : consolidatedData.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> data = entry.getValue();

            SheetRow sr = new SheetRow();
            sr.setProjectId(projectId);
            sr.setType(TypeAv.TARM_FROTA);

            sr.getData().put("COLABORADOR", name);
            sr.getData().put("PLANTAO", String.valueOf(data.get("PLANTAO")));

            long tarmSecs  = (long) data.get("TARM_SECONDS");
            long frotaSecs = (long) data.get("FROTA_SECONDS");

            sr.getData().put(KEY_TEMPO_REGULACAO_TARM,   String.valueOf(tarmSecs));
            sr.getData().put("TEMPO.REGULACAO.TARM",     String.valueOf(tarmSecs));
            sr.getData().put(KEY_TEMPO_REGULACAO_FROTA,  String.valueOf(frotaSecs));
            sr.getData().put("TEMPO.REGULACAO.FROTA",    String.valueOf(frotaSecs));

            srList.add(sr);
        }

        sheetRowRepository.saveAll(srList);

        List<String> result = atualizarColaboradoresDoProjeto(projectId);
        return !result.isEmpty() ? result : List.of();
    }

    private void consolidar(Map<String, Map<String, Object>> consolidatedData,
                            String name, double plantao, long tarmSecs, long frotaSecs) {
        Map<String, Object> data = consolidatedData.computeIfAbsent(name, k -> {
            Map<String, Object> m = new HashMap<>();
            m.put("PLANTAO", 0.0);
            m.put("TARM_SECONDS", 0L);
            m.put("FROTA_SECONDS", 0L);
            return m;
        });
        data.put("PLANTAO",       (double) data.get("PLANTAO")       + plantao);
        data.put("TARM_SECONDS",  (long)   data.get("TARM_SECONDS")  + tarmSecs);
        data.put("FROTA_SECONDS", (long)   data.get("FROTA_SECONDS") + frotaSecs);
    }

    private long parseCellToSeconds(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            double excelTime = cell.getNumericCellValue();
            return Math.round(excelTime * 86400);
        } else if (cell.getCellType() == CellType.STRING) {
            return parseStringToSeconds(cell.getStringCellValue().trim());
        }
        return 0;
    }

    private long parseStringToSeconds(String timeStr) {
        if (timeStr == null || timeStr.isEmpty() || timeStr.equals("-")) return 0;
        try {
            long days = 0;
            String workingTime = timeStr.trim();
            if (workingTime.contains("d.")) {
                String[] parts = workingTime.split("d\\.");
                days = Long.parseLong(parts[0]);
                workingTime = parts[1];
            }
            String[] parts = workingTime.split(":");
            long h = 0, m = 0, s = 0;
            if (parts.length == 3) {
                h = Long.parseLong(parts[0]);
                m = Long.parseLong(parts[1]);
                s = Long.parseLong(parts[2]);
            } else if (parts.length == 2) {
                m = Long.parseLong(parts[0]);
                s = Long.parseLong(parts[1]);
            }
            return (days * 86400) + (h * 3600) + (m * 60) + s;
        } catch (Exception e) {
            return 0;
        }
    }

    private double getNumericValue(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().replace(",", "."));
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private Map<String, Integer> getColumnMapping(Row row) {
        Map<String, Integer> mapping = new HashMap<>();
        if (row == null) return mapping;
        DataFormatter formatter = new DataFormatter();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) continue;
            String val = formatter.formatCellValue(cell).toUpperCase().trim();
            if (!val.isEmpty()) mapping.put(val, i);
        }
        return mapping;
    }

    private String getCellStringValue(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf(cell.getNumericCellValue());
        return null;
    }

    private Integer encontrarIndiceColuna(Map<String, Integer> cols, String... possiveisNomes) {
        for (String nome : possiveisNomes) {
            String normBusca = normalize(nome);
            // 1. match exato
            for (Map.Entry<String, Integer> e : cols.entrySet()) {
                if (normalize(e.getKey()).equals(normBusca)) return e.getValue();
            }
            // 2. contains como fallback
            for (Map.Entry<String, Integer> e : cols.entrySet()) {
                if (normalize(e.getKey()).contains(normBusca)) return e.getValue();
            }
        }
        return null;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    @Transactional
    public List<String> atualizarColaboradoresDoProjeto(String projectId) {
        ProjetoEntity projeto = projetoRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        List<ProjectCollaborator> tarmFrotaColabs = projeto.getCollaborators().stream()
                .filter(pc -> !"MEDICO".equals(pc.getRole()))
                .toList();

        Map<String, ProjectCollaborator> colaboradores = tarmFrotaColabs.stream()
                .collect(Collectors.toMap(
                        pc -> normalizeName(pc.getNome()),
                        pc -> pc,
                        (a, b) -> a));

        List<SheetRow> rows = sheetRowRepository.findByProjectIdAndType(projectId, TypeAv.TARM_FROTA);

        // FIX: pré-carrega todos os idCallRote em 1 única query, evitando N+1
        Map<String, String> idCallRoteMap = colaboradorRepository
                .findAllById(tarmFrotaColabs.stream()
                        .map(ProjectCollaborator::getCollaboratorId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(
                        CollaboratorEntity::getId,
                        c -> c.getIdCallRote() != null ? c.getIdCallRote() : ""));

        List<ProjectCollaborator> pcsToUpdate   = new ArrayList<>();
        List<String>              idCallroutList = new ArrayList<>();

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
                                    // FIX: usa mapa pré-carregado — sem query adicional
                                    idCallroutList.add(idCallRoteMap.get(pc.getCollaboratorId()));
                                });
                    });
        }

        // FIX: pré-computa melhor match por colaborador em O(N×M) passagem única,
        //      eliminando o duplo loop O(N×M + N×M) anterior
        Map<String, Double>  bestScoreByCollab = new HashMap<>();
        Map<String, String>  bestNameByCollab  = new HashMap<>();

        for (SheetRow sr : rows) {
            String sheetName = sr.getData().get("COLABORADOR");
            String sheetNorm = normalizeName(sheetName);
            for (ProjectCollaborator pc : tarmFrotaColabs) {
                String collabNorm = normalizeName(pc.getNome());
                double score = similarity(sheetNorm, collabNorm);
                if (score > bestScoreByCollab.getOrDefault(collabNorm, 0.0)) {
                    bestScoreByCollab.put(collabNorm, score);
                    bestNameByCollab.put(collabNorm, sheetName);
                }
            }
        }

        List<String> naoEncontrados = new ArrayList<>();

        for (ProjectCollaborator pc : tarmFrotaColabs) {
            String collabNorm  = normalizeName(pc.getNome());
            double bestScore   = bestScoreByCollab.getOrDefault(collabNorm, 0.0);
            String bestMatch   = bestNameByCollab.get(collabNorm);

            if (bestScore < 0.75) {
                if (bestMatch != null && bestScore >= 0.4) {
                    naoEncontrados.add(String.format("%s (possível correspondência: %s)", collabNorm, bestMatch));
                } else {
                    naoEncontrados.add(collabNorm + " (nenhuma correspondência próxima encontrada)");
                }
            }

            if (!pcsToUpdate.contains(pc)) {
                pcsToUpdate.add(pc);
                // FIX: usa mapa pré-carregado
                idCallroutList.add(idCallRoteMap.get(pc.getCollaboratorId()));
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

    private void atualizarDadosColaborador(ProjectCollaborator pc, Map<String, String> data) {
        if (pc.getWasEdited()) return;

        var plantao    = data.get("PLANTAO") != null ? data.get("PLANTAO") : "0";
        int plantaoQtd = (int) Math.round(Double.parseDouble(
                Objects.equals(plantao, "00:00:00") ? "0" : plantao));
        pc.setPlantao(plantaoQtd);

        NestedScoringParameters params = Optional.ofNullable(pc.getParametros())
                .orElseGet(() -> {
                    NestedScoringParameters np = new NestedScoringParameters();
                    pc.setParametros(np);
                    return np;
                });
        if (params.getTarm()  == null) params.setTarm(new ScoringSectionParams());
        if (params.getFrota() == null) params.setFrota(new ScoringSectionParams());

        Map<String, List<String>> keyMap = new HashMap<>();
        keyMap.put("TARM",  Arrays.asList(KEY_TEMPO_REGULACAO_TARM,  "TEMPO.REGULACAO.TARM"));
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