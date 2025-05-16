package com.avaliadados.service;

import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.MedicoEntity;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.MedicoEntityRepository;
import com.avaliadados.repository.ProjetoRepository;
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
    private final ScoringService scoringService;
    private final ProjectCollabService projectCollabService;

    @Transactional
    public void processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {
        // 1) busca o projeto
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        Map<String, MedicoEntity> medicos = medicoRepo.findAll().stream()
                .collect(Collectors.toMap(
                        m -> normalizarNome(m.getNome()),
                        m -> m,
                        (a, b) -> a
                ));

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> cols = getColumnMapping(sheet.getRow(0));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String nomePlanilha = normalizarNome(getCellStringValue(row, cols.get("MEDICO REGULADOR")));
                if (nomePlanilha == null) continue;

                medicos.entrySet().stream()
                        .filter(e -> similaridade(e.getKey(), nomePlanilha) >= 0.85)
                        .max(Comparator.comparingDouble(e -> similaridade(e.getKey(), nomePlanilha)))
                        .ifPresent(match -> {
                            MedicoEntity medico = match.getValue();
                            projeto.getCollaborators().stream()
                                    .filter(pc -> pc.getCollaboratorId().equals(medico.getId()))
                                    .findFirst()
                                    .ifPresent(pc -> {
                                        pc.setMedicoRole(medico.getMedicoRole());

                                        atualizarDadosMedico(pc, row, cols, projectId);
                                    });
                        });
            }
        }

        log.info("ROle do medico {}, {}", projeto.getCollaborators().stream().map(ProjectCollaborator::getRole).collect(Collectors.toList()), projeto.getCollaborators().stream().map(ProjectCollaborator::getMedicoRole).collect(Collectors.toList()));
        projetoRepo.save(projeto);
    }

    private void atualizarDadosMedico(ProjectCollaborator pc,
                                      Row row,
                                      Map<String, Integer> cols, String projectID) {
        String[] tempoKeys = {"TEMPO MEDIO REGULACAO MEDICA"};
        String[] criticosKeys = {"CRITICOS"};

        Integer tempoMedioCol = findBestColumnMatch(cols, tempoKeys);
        Integer criticosCol = findBestColumnMatch(cols, criticosKeys);

        var params = pc.getParametros();
        if (params == null) {
            params = new NestedScoringParameters();
        }


        if (criticosCol != null
                && pc.getMedicoRole() == MedicoRole.REGULADOR) {
            Long regulacoesSec = getCellTimeInSeconds(row.getCell(tempoMedioCol));
            pc.setDurationSeconds(regulacoesSec);
            params.setMedico(ScoringSectionParams.builder().regulacao(List.of(ScoringRule.builder().duration(regulacoesSec).build())).build());
            log.info("Regulacoes para {} ({}): {}s", pc.getNome(), pc.getRole(), regulacoesSec);
        }

        if (criticosCol != null
                && pc.getMedicoRole() == MedicoRole.LIDER) {
            Long criticos = getCellTimeInSeconds(row.getCell(criticosCol));
            pc.setDurationSeconds(criticos);
            pc.setShiftHours(ShiftHours.H24);
            params.setMedico(ScoringSectionParams.builder().regulacaoLider(List.of(ScoringRule.builder().duration(criticos).build())).build());
            log.info("Criticos para {} ({}): {}s", pc.getNome(), pc.getRole(), criticos);
        }

        int pontos = scoringService.calculateCollaboratorScore(
                pc.getRole(),
                pc.getDurationSeconds(),
                pc.getQuantity(),
                pc.getPausaMensalSeconds(),
                params
        );
        log.info("Pontuação para {} ({}): {}", pc.getNome(), pc.getRole(), pontos);
        log.info("Parâmetros: {}", params);
        pc.setPontuacao(pontos);
        pc.setParametros(params);
        projectCollabService.updateProjectCollaborator(projectID, pc.getCollaboratorId(), pc);
    }

    private Integer findBestColumnMatch(Map<String, Integer> cols, String[] possibleKeys) {
        for (String key : possibleKeys) {
            String normalizedKey = Normalizer.normalize(key, Normalizer.Form.NFD)
                    .replaceAll("[^A-Z0-9 ]", "")
                    .trim()
                    .toUpperCase();

            if (cols.containsKey(normalizedKey)) {
                return cols.get(normalizedKey);
            }
        }
        return null;
    }

    private Map<String, Integer> getColumnMapping(Row header) {
        Map<String, Integer> map = new HashMap<>();
        DataFormatter fmt = new DataFormatter();

        for (Cell c : header) {
            String raw = fmt.formatCellValue(c);

            String normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
                    .replaceAll("[^\\p{ASCII}]", "") // Remove acentos
                    .replaceAll("[^A-Z0-9 ]", "")    // Mantém apenas letras, números e espaços
                    .replaceAll("\\d+$", "")         // Remove números no final (ex: 0000220)
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

    private Long getCellTimeInSeconds(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.STRING) {
                String s = cell.getStringCellValue().trim();
                if (s.matches("^\\d{1,2}:\\d{2}:\\d{2}$")) { // Novo tratamento para HH:mm:ss
                    String[] parts = s.split(":");
                    return Long.parseLong(parts[0]) * 3600L
                            + Long.parseLong(parts[1]) * 60L
                            + Long.parseLong(parts[2]);
                }
                LocalTime lt = LocalTime.parse(s.length() == 5 ? s + ":00" : s);
                return (long) lt.toSecondOfDay();
            }
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return Math.round(cell.getNumericCellValue() * 24 * 3600);
            }
        } catch (Exception e) {
            log.error("Erro convertendo tempo na célula {}: {}", cell.getAddress(), e.getMessage());
        }
        return null;
    }

    private String normalizarNome(String nome) {
        return nome == null ? null
                : nome.trim().toUpperCase().replaceAll("[^A-Z0-9 ]", "");
    }

    private double similaridade(String a, String b) {
        if (a == null || b == null) return 0;
        int dist = LevenshteinDistance.getDefaultInstance().apply(a, b);
        return 1.0 - (double) dist / Math.max(a.length(), b.length());
    }
}
