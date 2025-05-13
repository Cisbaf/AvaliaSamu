package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.avaliadados.service.ProjectCollabService.convertMapToNested;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvaliacaoService {

    private final CollaboratorRepository colaboradorRepository;
    private final ProjetoRepository projetoRepository;
    private final ScoringService scoringService;

    @Transactional
    public void processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {
        ProjetoEntity projeto = projetoRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        String hash = computeFileHash(arquivo.getInputStream());

        if (projeto.getProcessedSpreadsheetHashes() == null) {
            projeto.setProcessedSpreadsheetHashes(new ArrayList<>());
        }
        if (projeto.getProcessedSpreadsheetHashes().contains(hash)) {
            throw new RuntimeException("Esta planilha já foi processada para este projeto.");
        }

        Map<String, CollaboratorEntity> colaboradores = colaboradorRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        c -> normalizarNome(c.getNome()),
                        c -> c,
                        (a, b) -> a
                ));

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Map<String, Integer> cols = getColumnMapping(sheet.getRow(0));

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                processarLinha(projeto, colaboradores, cols, row);
            }
        }

        projeto.getProcessedSpreadsheetHashes().add(hash);
        projetoRepository.save(projeto);
    }

    private void processarLinha(ProjetoEntity projeto,
                                Map<String, CollaboratorEntity> colaboradores,
                                Map<String, Integer> cols,
                                Row row) {
        String nomePlanilha = normalizarNome(getCellStringValue(row, cols.get("COLABORADOR")));
        if (nomePlanilha == null) return;

        colaboradores.entrySet().stream()
                .filter(e -> similaridade(e.getKey(), nomePlanilha) >= 0.85)
                .max(Comparator.comparingDouble(e -> similaridade(e.getKey(), nomePlanilha)))
                .ifPresent(match -> {
                    CollaboratorEntity colEnt = match.getValue();
                    projeto.getCollaborators().stream()
                            .filter(pc -> pc.getCollaboratorId().equals(colEnt.getId()))
                            .findFirst()
                            .ifPresent(pc -> atualizarDadosColaborador(pc, row, cols, colEnt));
                });
    }

    private void atualizarDadosColaborador(ProjectCollaborator pc,
                                           Row row,
                                           Map<String, Integer> cols,
                                           CollaboratorEntity collab) {
        Map<String, String> roleToColumnMap = Map.of(
                "TARM", "TEMPO REGULAÇÃO TARM",
                "FROTA", "OP. FROTA REGULAÇÃO MÉDICA"
        );

        Long dur;
        String colName = roleToColumnMap.get(pc.getRole());
        if (colName != null && cols.containsKey(colName)) {
            dur = getCellTimeInSeconds(row.getCell(cols.get(colName)));
            pc.setDurationSeconds(dur);
            log.info("Duração para {} ({}): {}s", collab.getNome(), pc.getRole(), dur);
        }

        NestedScoringParameters params = pc.getParametros();
        if (params == null) {
            params = convertMapToNested(collab.getParametros());
        }
        if (params.getTarm() == null) params.setTarm(new ScoringSectionParams());
        if (params.getFrota() == null) params.setFrota(new ScoringSectionParams());
        if (params.getMedico() == null) params.setMedico(new ScoringSectionParams());

        int pontos = scoringService.calculateCollaboratorScore(
                pc.getRole(),
                pc.getDurationSeconds(),
                pc.getQuantity(),
                pc.getPausaMensalSeconds(),
                params
        );
        pc.setPontuacao(pontos);
        pc.setParametros(params);
    }

    private String computeFileHash(InputStream is) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream dis = new DigestInputStream(is, md)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                }
            }
            byte[] digest = md.digest();
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Não foi possível criar hash da planilha", e);
        }
    }

    private Map<String, Integer> getColumnMapping(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        for (Cell cell : headerRow) {
            columnMap.put(cell.getStringCellValue().trim().toUpperCase(), cell.getColumnIndex());
        }
        return columnMap;
    }

    private String getCellStringValue(Row row, Integer colIndex) {
        if (colIndex == null) return null;
        Cell cell = row.getCell(colIndex);
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
                LocalTime lt = s.matches("\\d{1,2}:\\d{2}(:\\d{2})?")
                        ? LocalTime.parse(s.length() == 5 ? s + ":00" : s)
                        : null;
                return lt != null ? (long) lt.toSecondOfDay() : null;
            }
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return Math.round(cell.getNumericCellValue() * 24 * 3600);
            }
        } catch (Exception e) {
            log.error("Erro convertendo tempo: {} na célula {}", e.getMessage(), cell.getAddress());
        }
        return null;
    }

    private String normalizarNome(String nome) {
        return nome != null ? nome.trim().toUpperCase().replaceAll("[^A-Z0-9 ]", "") : null;
    }

    private double similaridade(String a, String b) {
        if (a == null || b == null) return 0;
        int dist = LevenshteinDistance.getDefaultInstance().apply(a, b);
        return 1.0 - (double) dist / Math.max(a.length(), b.length());
    }
}
