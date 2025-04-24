package com.avaliadados.service;

import com.avaliadados.model.ColaboradorEntity;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.TarmEntity;
import com.avaliadados.repository.ColaboradorRepository;
import com.avaliadados.repository.FrotaRepository;
import com.avaliadados.repository.TarmRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {
    private final TarmRepository tarmRepository;
    private final FrotaRepository frotaRepository;
    private final ColaboradorRepository colaboradorRepository;

    public void processarPlanilha(MultipartFile arquivo) throws IOException {
        List<TarmEntity> tammList = new ArrayList<>();
        List<FrotaEntity> fnotaList = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Mapear colunas
            Map<String, Integer> colunas = new HashMap<>();
            for (Cell cell : headerRow) {
                colunas.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }

            // Processar linhas
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nome = getCellStringValue(row, colunas.get("COLABORADOR"));
                if (nome == null || nome.trim().isEmpty()) continue;  // Skip empty names

                // Check if collaborator exists before processing
                if (!colaboradorRepository.existsByNome(nome)) {
                    // Process TARM entity
                    TarmEntity tamm = new TarmEntity();
                    tamm.setNome(nome);
                    tamm.setTempoRegulaco(convertToLocalTime(row.getCell(colunas.get("TEMPO REGULAÇÃO TARM"))));
                    if (tammList.stream().noneMatch(t -> t.getNome().equals(nome))) {  // Check if already in list
                        tammList.add(tamm);
                    }

                    // Process Frota entity
                    FrotaEntity fnota = new FrotaEntity();
                    fnota.setNome(nome);
                    fnota.setRegulacaoMedica(convertToLocalTime(row.getCell(colunas.get("OP. FROTA REGULAÇÃO MÉDICA"))));
                    if (fnotaList.stream().noneMatch(f -> f.getNome().equals(nome))) {  // Check if already in list
                        fnotaList.add(fnota);
                    }
                }
            }
        }

        // Salvar no banco
        tarmRepository.saveAll(tammList);
        frotaRepository.saveAll(fnotaList);
    }

    private String getCellStringValue(Row row, Integer colIndex) {
        if (colIndex == null || row.getCell(colIndex) == null) return null;
        return row.getCell(colIndex).getStringCellValue();
    }

    private LocalTime convertToLocalTime(Cell cell) {
        if (cell == null) return null;

        try {
            // Tenta parsear como string (HH:mm:ss)
            return LocalTime.parse(cell.getStringCellValue());
        } catch (Exception e1) {
            try {
                // Tenta como numérico (fração do dia)
                double num = cell.getNumericCellValue();
                int hours = (int) (num * 24);
                int minutes = (int) ((num * 24 * 60) % 60);
                return LocalTime.of(hours, minutes);
            } catch (Exception e2) {
                return null;
            }
        }
    }
    public List<String> findAll(){
        List<String> allList = new ArrayList<>();

        List<TarmEntity> tarmList = tarmRepository.findAll();
        for (TarmEntity tarm : tarmList) {
            allList.add("Tarm nome: " +  tarm.getNome() +  ", tempo: " +  tarm.getTempoRegulaco());
        }
        List<FrotaEntity> frotaList = frotaRepository.findAll();
        for (FrotaEntity fnota : frotaList) {
            allList.add("Frota nome: " +  fnota.getNome() +  ", tempo: " +  fnota.getRegulacaoMedica());
        }

        return allList;
    }
    public List<ColaboradorEntity> findByName(String nome){
       return colaboradorRepository.findByNomeApproximate(nome);
    }
    public ColaboradorEntity cadastroColaborador(ColaboradorEntity colaborador){
        return null;
    }


}