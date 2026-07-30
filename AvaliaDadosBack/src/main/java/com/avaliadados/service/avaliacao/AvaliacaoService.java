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
import com.avaliadados.service.utils.WorkbookReader;
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

        try (Workbook wb = WorkbookReader.read(arquivo)) {
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();

            Sheet sheet = null;
            Row headerRow = null;
            Map<String, Integer> cols = null;

            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet currentSheet = wb.getSheetAt(i);
                if (currentSheet == null) continue;

                for (int rowIndex = 0; rowIndex <= 1; rowIndex++) {
                    Row tempHeader = currentSheet.getRow(rowIndex);
                    Map<String, Integer> tempCols = getColumnMapping(tempHeader, evaluator, formatter);
                    if (temCabecalhoValido(tempCols)) {
                        sheet = currentSheet;
                        headerRow = tempHeader;
                        cols = tempCols;
                        break;
                    }
                }

                if (sheet != null) break;
            }

            if (sheet == null) {
                throw new RuntimeException("Aba com a coluna de COLABORADOR ou MÉDICO REGULADOR não encontrada no arquivo.");
            }

            Integer idxColab   = encontrarIndiceColuna(cols, "COLABORADOR", "MEDICO REGULADOR", "MEDICO REGULADOR");
            Integer idxTarm    = encontrarIndiceColuna(cols, "TEMPO REGULAÇÃO TARM", "TEMPO REGULACAO TARM", "TEMPO REGULACAO", "TEMPO MEDIO REGULACAO MEDICA", "TEMPO MEDIO REGULACAO");
            Integer idxFrota   = encontrarIndiceColuna(cols, "OP. FROTA REGULAÇÃO MÉDICA", "OP. FROTA REGULACAO MEDICA", "OP FROTA REGULACAO MEDICA", "TIH", "TEMPO MEDIO TIH", "TEMPO MEDIO CRITICOS", "CRITICOS");
            Integer idxPlantao = encontrarIndiceColuna(cols, "TOTAL DE PLANTÃO DE 12 HORAS", "TOTAL DE PLANTAO", "PLANTAO 12 HORAS", "PLANTAO");

            int startRow = headerRow.getRowNum() + 1;
            if (headerRow.getRowNum() <= 1) {
                for (int offset = 1; offset <= 2; offset++) {
                    Row rowAfterHeader = sheet.getRow(headerRow.getRowNum() + offset);
                    if (rowAfterHeader == null) continue;
                    String valRow = Objects.requireNonNullElse(getCellStringValue(rowAfterHeader, 0, evaluator, formatter), "").toUpperCase();
                    if (valRow.contains("PLANTAO") || valRow.contains("PLANTÃO")) {
                        startRow = headerRow.getRowNum() + offset + 1;
                        break;
                    }
                }
            }

            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || row == headerRow) continue;

                if (idxColab == null) {
                    throw new RuntimeException("Não foi possível localizar a coluna de colaborador na planilha.");
                }

                String name = getCellStringValue(row, idxColab, evaluator, formatter);
                if (name == null || name.trim().isEmpty() || name.equalsIgnoreCase("nan") || name.equalsIgnoreCase("COLABORADOR") || name.equalsIgnoreCase("MÉDICO REGULADOR") || name.equalsIgnoreCase("MEDICO REGULADOR")) continue;

                double plantao = 0;
                if (idxPlantao != null) {
                    plantao = getNumericValue(row.getCell(idxPlantao), evaluator);
                }

                long tarmSecs = 0;
                if (idxTarm != null) {
                    tarmSecs = parseCellToSeconds(row.getCell(idxTarm), evaluator, formatter);
                }

                long frotaSecs = 0;
                if (idxFrota != null) {
                    frotaSecs = parseCellToSeconds(row.getCell(idxFrota), evaluator, formatter);
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

    private boolean temCabecalhoValido(Map<String, Integer> cols) {
        if (cols == null || cols.isEmpty()) return false;

        boolean hasColaborador = encontrarIndiceColuna(cols, "COLABORADOR", "MEDICO REGULADOR", "MEDICO REGULADOR") != null;
        boolean hasPlantao = encontrarIndiceColuna(cols, "TOTAL DE PLANTÃO DE 12 HORAS", "TOTAL DE PLANTAO", "PLANTAO 12 HORAS", "PLANTAO") != null;
        boolean hasTime = encontrarIndiceColuna(cols, "TEMPO REGULAÇÃO TARM", "TEMPO REGULACAO TARM", "TEMPO REGULACAO", "TEMPO MEDIO REGULACAO MEDICA", "TEMPO MEDIO REGULACAO") != null;
        boolean hasFrota = encontrarIndiceColuna(cols, "OP. FROTA REGULAÇÃO MÉDICA", "OP. FROTA REGULACAO MEDICA", "OP FROTA REGULACAO MEDICA", "TIH", "TEMPO MEDIO TIH", "TEMPO MEDIO CRITICOS", "CRITICOS") != null;

        return hasColaborador && (hasPlantao || hasTime || hasFrota);
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

    private long parseCellToSeconds(Cell cell, FormulaEvaluator evaluator, DataFormatter formatter) {
        if (cell == null) return 0;
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();

        if (type == CellType.NUMERIC) {
            double excelTime = cell.getNumericCellValue();
            return Math.round(excelTime * 86400);
        } else {
            String timeStr = formatter.formatCellValue(cell, evaluator);
            return parseStringToSeconds(timeStr.trim());
        }
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

    private double getNumericValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return 0;
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();

        if (type == CellType.NUMERIC) return cell.getNumericCellValue();
        if (type == CellType.STRING || type == CellType.FORMULA) {
            try {
                String val = cell.getStringCellValue();
                if(val == null) return 0;
                return Double.parseDouble(val.replace(",", "."));
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    private Map<String, Integer> getColumnMapping(Row row, FormulaEvaluator evaluator, DataFormatter formatter) {
        Map<String, Integer> mapping = new HashMap<>();
        if (row == null) return mapping;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) continue;
            String val = formatter.formatCellValue(cell, evaluator).toUpperCase().trim();
            if (!val.isEmpty()) mapping.put(val, i);
        }
        return mapping;
    }

    private String getCellStringValue(Row row, int idx, FormulaEvaluator evaluator, DataFormatter formatter) {
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        return formatter.formatCellValue(cell, evaluator);
    }

    private Integer encontrarIndiceColuna(Map<String, Integer> cols, String... possiveisNomes) {
        for (String nome : possiveisNomes) {
            String normBusca = normalize(nome);
            for (Map.Entry<String, Integer> e : cols.entrySet()) {
                if (normalize(e.getKey()).equals(normBusca)) return e.getValue();
            }
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
                                    idCallroutList.add(idCallRoteMap.get(pc.getCollaboratorId()));
                                });
                    });
        }

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