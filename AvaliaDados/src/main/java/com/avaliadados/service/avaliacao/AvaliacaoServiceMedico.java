package com.avaliadados.service.avaliacao;

import com.avaliadados.model.*;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.enums.TypeAv;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.MedicoEntityRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.repository.SheetRowRepository;
import com.avaliadados.service.factory.AvaliacaoProcessor;
import com.avaliadados.service.utils.CollabParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.avaliadados.service.utils.SheetsUtils.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AvaliacaoServiceMedico implements AvaliacaoProcessor {

    private final ProjetoRepository projetoRepo;
    private final MedicoEntityRepository medicoRepo;
    private final SheetRowRepository sheetRowRepo;
    private final CollabParams collabParams;
    private final CollaboratorRepository colaboradorRepository;


    @Transactional
    public void processarPlanilha(MultipartFile arquivo, String projectId) throws IOException {
        sheetRowRepo.deleteByProjectIdAndType(projectId, TypeAv.MEDICO);

        try (Workbook wb = WorkbookFactory.create(arquivo.getInputStream())) {
            var sheet = wb.getSheetAt(0);
            var cols = getColumnMapping(sheet.getRow(0));

            if (cols.entrySet().stream().noneMatch(e -> e.getKey().startsWith("MEDICO REGULADOR"))) {
                cols = getColumnMapping(sheet.getRow(1));
            }

            Integer idxMedReg = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("MEDICO REGULADOR"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            Integer idxTempoMed = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("TEMPO MEDIO REGULAÇÃO MEDICA"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
            Integer idxCrit = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("CRITICOS"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);

            Integer idxPlantao = cols.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("PLANTÃO 12 HORAS"))
                    .map(Map.Entry::getValue).findFirst().orElse(null);

            log.info("Índices fuzzy → MEDICO_REGULADOR: {}, TEMPO_MED: {}, CRITICOS: {}, PLANTAO: {}",
                    idxMedReg, idxTempoMed, idxCrit, idxPlantao);
            log.info("Índices sheets : {}", cols.keySet());

            String nomeMed = "";
            String tempoReg = "";
            String plantao = "";

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                var row = sheet.getRow(i);
                if (row == null) continue;
                if (idxMedReg != null) {
                    nomeMed = getCellStringValue(row, idxMedReg);
                }
                if (idxTempoMed != null) {
                    tempoReg = getCellStringValue(row, idxTempoMed);
                }
                if (idxPlantao != null) {
                    plantao = getCellStringValue(row, idxPlantao);
                }
                if (nomeMed == null || tempoReg == null || plantao == null) continue;

                List<CollaboratorEntity> encontrados =
                        colaboradorRepository.findByNomeIgnoreCase(nomeMed.trim());

                String idEncontrado;
                if (encontrados.isEmpty()) {
                    continue;
                } else if (encontrados.size() == 1) {
                    // Exatamente um → OK
                    idEncontrado = encontrados.getFirst().getId();
                } else {
                    // Mais de um → tomamos alguma decisão (por ex. usar o primeiro ou logar)
                    log.warn("Mais de um colaborador encontrado com nome '{}'. IDs retornados = {}",
                            nomeMed,
                            encontrados.stream().map(CollaboratorEntity::getId).toList());
                    // Por hora, vamos usar o primeiro da lista:
                    idEncontrado = encontrados.getFirst().getId();
                }

                SheetRow sr = new SheetRow();
                sr.setProjectId(projectId);
                if (idEncontrado != null) {
                    sr.setCollaboratorId(idEncontrado);
                }
                sr.setType(TypeAv.MEDICO);
                sr.getData().put("MEDICO.REGULADOR", nomeMed);
                sr.getData().put("TEMPO.REGULACAO", tempoReg);
                sr.getData().put("PLANTAO", plantao);
                if (idxCrit != null) {
                    var crit = getCellStringValue(row, idxCrit);
                    if (crit != null) sr.getData().put("CRITICOS", crit);
                }
                sheetRowRepo.save(sr);
            }
        }

        log.info("Planilha médica do projeto {} salva com {} linhas específicas",
                projectId,
                sheetRowRepo.findByProjectId(projectId).size());

        sincronizarColaboradores(projectId);
    }

    @Transactional
    public void sincronizarColaboradores(String projectId) {
        var projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado: " + projectId));

        Map<String, List<MedicoEntity>> medicosPorNome = medicoRepo.findAll().stream()
                .collect(Collectors.groupingBy(
                        m -> normalizeName(m.getNome())
                ));

        for (SheetRow sr : sheetRowRepo.findByProjectId(projectId)) {
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
                atualizarDadosMedico(pc, sr.getData(), projeto);
            }
        }

        projetoRepo.save(projeto);
        log.info("Colaboradores sincronizados para projeto {}", projectId);
    }

    private void atualizarDadosMedico(ProjectCollaborator pc,
                                      Map<String, String> data,
                                      ProjetoEntity projeto) {

        log.info("Atualizando Colaborador: ID={}, Nome={}, Role={}, Dados Planilha={}",
                pc.getCollaboratorId(), pc.getNome(), pc.getMedicoRole(), data);

        long duration = 0L;
        long criticos = 0L; // Separar os dois valores
        int quantity = 0;
        long pausaMensal = 0L;

        MedicoRole medicoRole = pc.getMedicoRole();

        var plantao = data.get("PLANTAO") != null ? data.get("PLANTAO") : "0";
        int plantaoQtd = (int) Math.round(Double.parseDouble(plantao));
        pc.setPlantao(plantaoQtd);

        switch (medicoRole) {
            case REGULADOR:
                duration = data.containsKey("TEMPO.REGULACAO")
                        ? parseTimeToSeconds(data.get("TEMPO.REGULACAO")) : 0L;
                log.info("  -> Role REGULADOR. Usando TEMPO.REGULACAO: {}. duration={}",
                        data.getOrDefault("TEMPO.REGULACAO", "N/A"), duration);
                break;

            case LIDER:
                criticos = data.containsKey("CRITICOS")
                        ? parseTimeToSeconds(data.get("CRITICOS")) : 0L;
                // Adicione este log
                log.info("  -> Role LIDER. Usando CRITICOS: {}. criticos={}",
                        data.getOrDefault("CRITICOS", "N/A"), criticos);
                break;
        }

        if (data.containsKey("REMOVIDOS")) {
            quantity = Integer.parseInt(data.get("REMOVIDOS"));
        }

        if (data.containsKey("PAUSAS")) {
            pausaMensal = parseTimeToSeconds(data.get("PAUSAS"));
        }

        if (pc.getShiftHours() == null) {
            pc.setShiftHours(ShiftHours.H12);
            log.info("Definindo turno padrão H12 para colaborador {}", pc.getNome());
        }
        log.info("  -> Valores calculados para setParams: duration={}, criticos={}, quantity={}, pausaMensal={}",
                duration, criticos, quantity, pausaMensal);

        var apiData = collabParams.setDataFromApi(pc, projeto, colaboradorRepository.getReferenceById(pc.getCollaboratorId()).getIdCallRote());
        var removidos = Math.toIntExact(apiData.get("removeds"));
        var pausas = apiData.get("pauses");
        pc.setRemovidos(removidos);

        int pontos = collabParams.setParams(
                pc,
                projeto,
                removidos,
                duration,
                criticos,
                pausas,
                criticos
        );

        log.info("  -> Pontuação final: {}. Atualizando pc.setPontuacao={}, pc.setDurationSeconds={}, pc.setCriticos={}",
                pontos, pontos, duration, criticos);

        pc.setPontuacao(pontos);
        pc.setDurationSeconds(duration);
        pc.setCriticos(criticos);

    }

}