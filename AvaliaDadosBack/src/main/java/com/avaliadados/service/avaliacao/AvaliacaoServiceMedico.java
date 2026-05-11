package com.avaliadados.service.avaliacao;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.enums.TypeAv;
import com.avaliadados.model.roles.MedicoEntity;
import com.avaliadados.repository.*;
import com.avaliadados.service.factory.AvaliacaoProcessor;
import com.avaliadados.service.utils.CollabParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

import static com.avaliadados.service.utils.SheetsUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvaliacaoServiceMedico implements AvaliacaoProcessor {

    private final ProjetoRepository projetoRepo;
    private final MedicoEntityRepository medicoRepo;
    private final SheetRowRepository sheetRowRepo;
    private final CollabParams collabParams;
    private final CollaboratorRepository colaboradorRepository;

    @Transactional
    public List<String> processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {

        sheetRowRepo.deleteByProjectIdAndType(projectId, TypeAv.MEDICO);

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {

            Sheet sheet = wb.getSheetAt(0);

            Map<String, Integer> cols = mapearColunasBlindado(sheet);

            Integer idxMedReg = cols.get("MEDICO");
            Integer idxPlantao = cols.get("PLANTAO");
            Integer idxTempoMed = cols.get("TEMPO_REG");
            Integer idxCrit = cols.get("CRITICOS");
            Integer idxTempoAnalitico = cols.get("TEMPO_ANALITICO");

            if (idxTempoMed == null) {
                log.warn("⚠️ Fallback tempo regulação -> coluna 16");
                idxTempoMed = 16;
            }

            // Dados de linha 0 = título, linha 1 = cabeçalho, dados começam na linha 2
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nomeMed = getCellStringValue(row, idxMedReg);
                String tempoReg = getCellStringValue(row, idxTempoMed);
                String plantao = idxPlantao != null ? getCellStringValue(row, idxPlantao) : "";

                if (isBlank(nomeMed) || isBlank(tempoReg)) continue;

                String nomeNormPlanilha = normalize(nomeMed);

                List<CollaboratorEntity> encontrados = colaboradorRepository.findAll().stream()
                        .filter(c -> normalize(c.getNome()).equals(nomeNormPlanilha))
                        .toList();

                if (!encontrados.isEmpty()) {

                    List<SheetRow> srList = new ArrayList<>();

                    for (CollaboratorEntity colaborador : encontrados) {

                        SheetRow sr = new SheetRow();
                        sr.setProjectId(projectId);
                        sr.setCollaboratorId(colaborador.getId());
                        sr.setType(TypeAv.MEDICO);

                        sr.getData().put("MEDICO.REGULADOR", nomeMed);
                        sr.getData().put("TEMPO.REGULACAO", tempoReg);
                        sr.getData().put("PLANTAO", plantao);

                        if (idxCrit != null) {
                            String crit = getCellStringValue(row, idxCrit);
                            if (!isBlank(crit)) {
                                sr.getData().put("CRITICOS", crit);
                            }
                        }

                        if (idxTempoAnalitico != null) {
                            String analitico = getCellStringValue(row, idxTempoAnalitico);
                            if (!isBlank(analitico)) {
                                sr.getData().put("TEMPO.ANALITICO", analitico);
                            }
                        }

                        srList.add(sr);
                    }

                    sheetRowRepo.saveAll(srList);
                }
            }
        }

        return sincronizarColaboradores(projectId);
    }

    // ===================== PARSER BLINDADO =====================

    private Map<String, Integer> mapearColunasBlindado(Sheet sheet) {
        Map<String, Integer> result = new HashMap<>();

        // Consideramos as linhas 0 e 1 para detectar os cabeçalhos
        Row r0 = sheet.getRow(0);
        Row r1 = sheet.getRow(1);

        if (r0 == null && r1 == null) return result;

        int maxCols = Math.max(
                r0 != null ? r0.getLastCellNum() : 0,
                r1 != null ? r1.getLastCellNum() : 0
        );

        for (int i = 0; i < maxCols; i++) {
            String h0 = normalize(getCell(r0, i));
            String h1 = normalize(getCell(r1, i));
            String fullHeader = h0 + " " + h1;

            // Nome do Médico (Pegamos apenas a primeira coluna que encontrar isso, que é a 0)
            if (fullHeader.contains("MEDICO") && fullHeader.contains("REGULADOR")) {
                result.putIfAbsent("MEDICO", i);
            }

            // Plantão
            if (fullHeader.contains("PLANTAO") && (fullHeader.contains("12") || fullHeader.contains("HORA"))) {
                result.putIfAbsent("PLANTAO", i);
            }

            // Tempo de Regulação (Geralmente coluna 14)
            if (fullHeader.contains("REGULACAO") && !fullHeader.contains("MEDICO") && !fullHeader.contains("ANALITICO")) {
                result.putIfAbsent("TEMPO_REG", i);
            }

            // Críticos (Coluna 17) - Buscamos especificamente por "CRITICO" na linha 0
            if (h0.contains("CRITICO")) {
                result.put("CRITICOS", i);
            }

            // Tempo Analítico / TIH (Coluna 16)
            if (fullHeader.contains("ANALITICO") || fullHeader.contains("TIH")) {
                // Se houver "MEDICO" no cabeçalho da linha 1 (como na col 15), ignoramos
                if (!h1.contains("MEDICO")) {
                    result.put("TEMPO_ANALITICO", i);
                }
            }
        }

        log.info("Colunas mapeadas corretamente: {}", result);
        return result;
    }

    private String getCell(Row row, int idx) {
        if (row == null) return "";
        Cell c = row.getCell(idx);
        return c == null ? "" : c.toString();
    }

    private String normalize(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty() || "-".equals(s.trim());
    }

    @Transactional
    public List<String> sincronizarColaboradores(String projectId) {
        var projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        Map<String, List<MedicoEntity>> medicosPorNome = medicoRepo.findAll().stream()
                .collect(Collectors.groupingBy(m -> normalizeName(m.getNome())));

        List<ProjectCollaborator> medicosNoProjeto = projeto.getCollaborators().stream()
                .filter(pc -> "MEDICO".equals(pc.getRole()))
                .toList();

        Map<String, CollaboratorEntity> collaboratorMap = colaboradorRepository
                .findAllById(medicosNoProjeto.stream()
                        .map(ProjectCollaborator::getCollaboratorId)
                        .toList())
                .stream()
                .collect(Collectors.toMap(CollaboratorEntity::getId, c -> c));

        List<SheetRow> sheetRows = sheetRowRepo.findByProjectId(projectId);
        Set<String> normalizedSheetNames = sheetRows.stream()
                .map(sr -> normalizeName(sr.getData().get("MEDICO.REGULADOR")))
                .collect(Collectors.toSet());

        List<String> naoEncontrados = new ArrayList<>();

        // Verificar médicos do projeto ausentes na planilha
        for (ProjectCollaborator pc : medicosNoProjeto) {
            CollaboratorEntity collab = collaboratorMap.get(pc.getCollaboratorId());
            if (collab == null) {
                log.warn("⚠️ Colaborador ID {} listado no projeto mas não existe no banco!", pc.getCollaboratorId());
                continue;
            }
            String nomeOriginal = collab.getNome();
            String nomeNorm = normalizeName(nomeOriginal);

            if (!normalizedSheetNames.contains(nomeNorm)) {
                String bestMatchName = null;
                double highestScore = 0.0;

                for (String sheetName : normalizedSheetNames) {
                    double currentScore = similarity(nomeNorm, sheetName);
                    if (currentScore > highestScore) {
                        highestScore = currentScore;
                        bestMatchName = sheetName;
                    }
                }

                if (bestMatchName != null && highestScore >= 0.5) {
                    naoEncontrados.add(nomeOriginal + " (possível correspondência: " + bestMatchName + ")");
                } else {
                    naoEncontrados.add(nomeOriginal + " (nenhuma correspondência próxima encontrada)");
                }
            }
        }

        Map<ProjectCollaborator, String> pcToIdMap = new LinkedHashMap<>();

        // Processar registros da planilha
        for (SheetRow sr : sheetRows) {
            String rawNome = Optional.ofNullable(sr.getData().get("MEDICO.REGULADOR"))
                    .orElse("MEDICO.LIDER");
            String nomeNorm = normalizeName(rawNome);

            List<MedicoEntity> possiveis = medicosPorNome.getOrDefault(nomeNorm, List.of());

            for (MedicoEntity med : possiveis) {
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

                if (pc.getWasEdited() == null) {
                    pc.setWasEdited(false);
                }
                if (!pc.getWasEdited()) {
                    CollaboratorEntity c = collaboratorMap.get(pc.getCollaboratorId());
                    String idCallRote = (c != null && c.getIdCallRote() != null) ? c.getIdCallRote() : "";
                    pcToIdMap.put(pc, idCallRote);
                }
            }
        }

        // Garantir que todos os médicos estejam no projeto
        List<MedicoEntity> todosMedicos = medicoRepo.findAll();
        for (MedicoEntity med : todosMedicos) {
            // ✅ Verifica pelo Map, não mais por pcsToUpdate
            boolean jaAdicionado = pcToIdMap.keySet().stream()
                    .anyMatch(pc -> pc.getCollaboratorId().equals(med.getId()));

            if (!jaAdicionado) {
                ProjectCollaborator pc = projeto.getCollaborators().stream()
                        .filter(c -> c.getCollaboratorId().equals(med.getId()))
                        .findFirst()
                        .orElseGet(() -> {
                            var novo = ProjectCollaborator.builder()
                                    .collaboratorId(med.getId())
                                    .nome(med.getNome())
                                    .role(med.getRole())
                                    .medicoRole(med.getMedicoRole())
                                    .build();
                            projeto.getCollaborators().add(novo);
                            return novo;
                        });
                CollaboratorEntity c = collaboratorMap.get(pc.getCollaboratorId());
                String idCallRote = (c != null && c.getIdCallRote() != null) ? c.getIdCallRote() : "";
                pcToIdMap.put(pc, idCallRote);
            }
        }

        // Atualizar dados dos médicos a partir da planilha
        for (SheetRow sr : sheetRows) {
            String rawNome = Optional.ofNullable(sr.getData().get("MEDICO.REGULADOR"))
                    .orElse("MEDICO.LIDER");
            String nomeNorm = normalizeName(rawNome);

            List<MedicoEntity> possiveis = medicosPorNome.getOrDefault(nomeNorm, List.of());

            for (MedicoEntity med : possiveis) {
                String collabId = med.getId();

                projeto.getCollaborators().stream()
                        .filter(c -> c.getCollaboratorId().equals(collabId))
                        .findFirst()
                        .ifPresent(pc -> atualizarDadosMedico(pc, sr.getData()));
            }
        }

        if (!pcToIdMap.isEmpty()) {
            collabParams.setDataFromApi(pcToIdMap, projeto);

            for (ProjectCollaborator pc : pcToIdMap.keySet()) {
                if ((pc.getPausaMensalSeconds() != null && pc.getPausaMensalSeconds() > 0)
                        || (pc.getDurationSeconds() != null && pc.getDurationSeconds() > 0)) {
                    int pontos = collabParams.setParams(
                            pc,
                            projeto,
                            pc.getRemovidos(),
                            pc.getRemovidosLider() != null ? pc.getRemovidosLider() : 0,
                            pc.getDurationSeconds(),
                            pc.getCriticos(),
                            pc.getPausaMensalSeconds() != null ? pc.getPausaMensalSeconds() : 0L,
                            0L);
                    pc.setPontuacao(pontos);
                } else {
                    pc.setPontuacao(0);
                }
            }
        }

        projetoRepo.save(projeto);
        return naoEncontrados;
    }

    private void atualizarDadosMedico(ProjectCollaborator pc, Map<String, String> data) {

        long duration = 0L;
        long criticos = 0L;

        MedicoRole medicoRole = pc.getMedicoRole();

        var plantaoStr = data.get("PLANTAO");
        log.info("Plantão: {}", plantaoStr);

        if (plantaoStr != null && !plantaoStr.isBlank()) {
            try {
                int plantaoQtd = plantaoStr.contains(":")
                        ? 0
                        : (int) Math.round(Double.parseDouble(plantaoStr));

                // ✅ Só seta se ainda não foi definido — evita sobrescrita em chamadas repetidas
                if (pc.getPlantao() == null || pc.getPlantao() == 0) {
                    pc.setPlantao(plantaoQtd);
                }

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Erro ao converter PLANTAO: " + plantaoStr, e);
            }
        }

        switch (medicoRole) {
            case REGULADOR:
                duration = data.containsKey("TEMPO.REGULACAO")
                        ? parseTimeToSeconds(data.get("TEMPO.REGULACAO")) : 0L;
                break;

            case LIDER:
                criticos = data.containsKey("CRITICOS")
                        ? parseTimeToSeconds(data.get("CRITICOS")) : 0L;
                break;
        }

        if (pc.getShiftHours() == null) {
            pc.setShiftHours(ShiftHours.H12);
        }

        pc.setDurationSeconds(duration);
        pc.setCriticos(criticos);
    }
}