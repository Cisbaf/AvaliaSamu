package com.avaliadados.service.avaliacao;

import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.TypeAv;
import com.avaliadados.repository.SheetRowRepository;
import com.avaliadados.service.factory.AvaliacaoProcessor;
import com.avaliadados.service.utils.WorkbookReader;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Planilha de VTR (TEMPO RESPOSTA ANALÍTICO), uma linha por chamado.
 *
 * Para o frota (coluna RÁDIO OPERADOR):
 * - Tempo de regulação = EMPENHO VTR - SOLICITAÇÃO VTR
 * - Tempo de saída    = SAÍDA VTR - EMPENHO VTR
 * - Linhas com tempo negativo ficam de fora do cálculo.
 * - Média = soma das diferenças ÷ quantidade de diferenças válidas (cada chamado com tempo não negativo).
 * - Plantões = quantidade de turnos (diurno 07h-19h, noturno 19h-07h) com chamado.
 */
@Service
@RequiredArgsConstructor
public class AvaliacaoServiceFrotaVtr implements AvaliacaoProcessor {

    public static final String KEY_COLABORADOR = "COLABORADOR";
    public static final String KEY_PLANTAO = "PLANTAO";
    public static final String KEY_REGULACAO_FROTA = "TEMPO.REGULACAO.FROTA";
    public static final String KEY_SAIDA_VTR = "SAIDA.VTR";

    private static final DateTimeFormatter DATA_HORA_SEG = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATA_HORA_MIN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final LocalTime INICIO_DIURNO = LocalTime.of(7, 0);
    private static final LocalTime FIM_DIURNO = LocalTime.of(19, 0);

    private final SheetRowRepository sheetRowRepository;
    private final AvaliacaoService avaliacaoService;

    @Override
    @Transactional
    public List<String> processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {
        sheetRowRepository.deleteByProjectIdAndType(projectId, TypeAv.FROTA_VTR);

        Map<String, Acumulado> porFrota = new LinkedHashMap<>();

        try (Workbook wb = WorkbookReader.read(arquivo)) {
            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter();

            Sheet sheet = null;
            Map<String, Integer> cols = null;
            int headerRowNum = -1;

            for (int i = 0; i < wb.getNumberOfSheets() && sheet == null; i++) {
                Sheet candidata = wb.getSheetAt(i);
                for (int r = 0; r <= Math.min(4, candidata.getLastRowNum()); r++) {
                    Map<String, Integer> mapa = mapearCabecalho(candidata.getRow(r), evaluator, formatter);
                    if (mapa.containsKey("RADIO OPERADOR") && mapa.containsKey("SOLICITACAO VTR")
                            && mapa.containsKey("EMPENHO VTR") && mapa.containsKey("SAIDA VTR")) {
                        sheet = candidata;
                        cols = mapa;
                        headerRowNum = r;
                        break;
                    }
                }
            }

            if (sheet == null) {
                throw new IllegalArgumentException("Planilha de VTR sem as colunas RÁDIO OPERADOR, SOLICITAÇÃO VTR, EMPENHO VTR e SAÍDA VTR.");
            }

            int idxOperador = cols.get("RADIO OPERADOR");
            int idxSolicitacao = cols.get("SOLICITACAO VTR");
            int idxEmpenho = cols.get("EMPENHO VTR");
            int idxSaida = cols.get("SAIDA VTR");

            for (int i = headerRowNum + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String operador = texto(row, idxOperador, evaluator, formatter);
                if (operador.isBlank()) continue;

                LocalDateTime solicitacao = parseDataHora(texto(row, idxSolicitacao, evaluator, formatter));
                LocalDateTime empenho = parseDataHora(texto(row, idxEmpenho, evaluator, formatter));
                LocalDateTime saida = parseDataHora(texto(row, idxSaida, evaluator, formatter));
                if (solicitacao == null || empenho == null || saida == null) continue;

                Acumulado ac = porFrota.computeIfAbsent(normalizar(operador), k -> new Acumulado(operador));
                ac.turnos.add(chaveTurno(solicitacao));

                long regulacao = Duration.between(solicitacao, empenho).getSeconds();
                long saidaSeg = Duration.between(empenho, saida).getSeconds();

                if (regulacao >= 0) {
                    ac.regulacaoSeg += regulacao;
                    ac.regulacaoQtd++;
                }
                if (saidaSeg >= 0) {
                    ac.saidaSeg += saidaSeg;
                    ac.saidaQtd++;
                }
            }
        }

        List<SheetRow> srList = new ArrayList<>();
        for (Acumulado ac : porFrota.values()) {
            // Média geral: soma das diferenças dividida pela quantidade delas (cada chamado válido).
            long plantoes = ac.turnos.size();
            long regulacaoMedia = ac.regulacaoQtd > 0 ? Math.round((double) ac.regulacaoSeg / ac.regulacaoQtd) : 0L;
            long saidaMedia = ac.saidaQtd > 0 ? Math.round((double) ac.saidaSeg / ac.saidaQtd) : 0L;

            SheetRow sr = new SheetRow();
            sr.setProjectId(projectId);
            sr.setType(TypeAv.FROTA_VTR);
            sr.getData().put(KEY_COLABORADOR, ac.nome);
            sr.getData().put(KEY_PLANTAO, String.valueOf(plantoes));
            sr.getData().put(KEY_REGULACAO_FROTA, String.valueOf(regulacaoMedia));
            sr.getData().put(KEY_SAIDA_VTR, String.valueOf(saidaMedia));
            srList.add(sr);
        }

        sheetRowRepository.saveAll(srList);

        return avaliacaoService.atualizarColaboradoresDoProjeto(projectId);
    }

    private static final class Acumulado {
        final String nome;
        long regulacaoSeg;
        long regulacaoQtd;
        long saidaSeg;
        long saidaQtd;
        final Set<String> turnos = new HashSet<>();

        Acumulado(String nome) {
            this.nome = nome.trim();
        }
    }

    private Map<String, Integer> mapearCabecalho(Row row, FormulaEvaluator evaluator, DataFormatter formatter) {
        Map<String, Integer> mapa = new HashMap<>();
        if (row == null) return mapa;
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null) continue;
            String valor = normalizar(formatter.formatCellValue(cell, evaluator));
            if (!valor.isEmpty()) mapa.putIfAbsent(valor, c);
        }
        return mapa;
    }

    private String texto(Row row, int idx, FormulaEvaluator evaluator, DataFormatter formatter) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        return formatter.formatCellValue(cell, evaluator).trim();
    }

    // Aceita "dd/MM/yyyy HH:mm:ss" e também "dd/MM/yyyy HH:mm".
    static LocalDateTime parseDataHora(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String v = valor.trim();
        try {
            return LocalDateTime.parse(v, DATA_HORA_SEG);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(v, DATA_HORA_MIN);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    // Diurno 07h-18h59:59; noturno 19h-06h59:59 (madrugada pertence ao noturno do dia anterior).
    static String chaveTurno(LocalDateTime dt) {
        LocalTime t = dt.toLocalTime();
        if (!t.isBefore(INICIO_DIURNO) && t.isBefore(FIM_DIURNO)) {
            return dt.toLocalDate() + "-DIURNO";
        }
        LocalDate inicio = t.isBefore(INICIO_DIURNO) ? dt.toLocalDate().minusDays(1) : dt.toLocalDate();
        return inicio + "-NOTURNO";
    }

    private static String normalizar(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
