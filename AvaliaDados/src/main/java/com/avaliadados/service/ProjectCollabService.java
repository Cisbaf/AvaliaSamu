package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.ProjectCollabRequest;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.repository.SheetRowRepository;
import com.avaliadados.service.utils.CollabParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.avaliadados.service.utils.SheetsUtils.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCollabService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final ScoringService scoringService;
    private final SheetRowRepository rowRepository;
    private final CollabParams collabParams;

    @Transactional
    public ProjetoEntity addCollaborator(String projectId, ProjectCollabRequest dto) {
        log.info("Adicionando colaborador [{}] ao projeto [{}] com role [{}]",
                dto.getCollaboratorId(), projectId, dto.getRole());

        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        CollaboratorEntity collab = collaboratorRepo.findById(dto.getCollaboratorId())
                .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));

        var scoringParams = convertMapToNested(collab.getParametros());

        log.debug("Parâmetros usados para pontuação: {}", scoringParams);

        if (dto.getMedicoRole() == null) {
            dto.setMedicoRole(MedicoRole.NENHUM);
        }

        ProjectCollaborator pc = ProjectCollaborator.builder()
                .collaboratorId(dto.getCollaboratorId())
                .nome(collab.getNome())
                .role(dto.getRole())
                .durationSeconds(dto.getDurationSeconds())
                .quantity(dto.getQuantity())
                .pausaMensalSeconds(dto.getPausaMensalSeconds())
                .parametros(scoringParams)
                .medicoRole(dto.getMedicoRole())
                .shiftHours(dto.getShiftHours())
                .build();

        SheetRow sheetColab = getSheetRowForCollaborator(dto.getCollaboratorId(), projectId, collab.getNome());

        if (sheetColab != null) {
            processSheetRowData(pc, sheetColab);

            int pontos = scoringService.calculateCollaboratorScore(
                    pc.getRole(),
                    pc.getMedicoRole().name(),
                    "H12",
                    pc.getDurationSeconds() != null ? pc.getDurationSeconds() : null,
                    pc.getQuantity() != null ? pc.getQuantity() : 0,
                    pc.getPausaMensalSeconds() != null ? pc.getPausaMensalSeconds() : null,
                    projeto.getParameters()
            );
            pc.setPontuacao(pontos);
            log.debug("Pontuação calculada para o colaborador: {}", pontos);
        } else {
            log.warn("Nenhum dado de planilha encontrado para o colaborador [{}]", dto.getCollaboratorId());
        }

        projeto.getCollaborators().removeIf(p -> p.getCollaboratorId().equals(dto.getCollaboratorId()));
        projeto.getCollaborators().add(pc);
        log.debug("Dados do Collaborador {}", pc);
        return projetoRepo.save(projeto);
    }

    private SheetRow getSheetRowForCollaborator(String collaboratorId, String projectId, String collaboratorName) {
        SheetRow sheetColab = rowRepository.findByCollaboratorIdAndProjectId(collaboratorId, projectId);

        if (sheetColab == null) {
            String nomeNormalizado = normalizeName(collaboratorName);
            List<SheetRow> todasLinhas = rowRepository.findByProjectId(projectId);

            Optional<SheetRow> melhorCorrespondencia = todasLinhas.stream()
                    .filter(row -> {
                        String nomeMedico = row.getData().get("MEDICO.REGULADOR");
                        if (nomeMedico == null) {
                            nomeMedico = row.getData().get("MEDICO.LIDER");
                        }
                        if (nomeMedico == null) return false;

                        return similarity(normalizeName(nomeMedico), nomeNormalizado) >= 0.85;
                    })
                    .findFirst();

            if (melhorCorrespondencia.isPresent()) {
                sheetColab = melhorCorrespondencia.get();

                sheetColab.setCollaboratorId(collaboratorId);
                rowRepository.save(sheetColab);

                log.info("Associado colaborador [{}] à linha da planilha por similaridade de nome", collaboratorId);
            }
        }

        return sheetColab;
    }


    private void processSheetRowData(ProjectCollaborator pc, SheetRow sheetRow) {
        log.info("Dados da planilha encontrados para o colaborador [{}]", pc.getCollaboratorId());
        Map<String, String> data = sheetRow.getData();

        if (Objects.equals(pc.getRole(), "TARM")) {
            String tempoRegulacaoTarm = data.get("TEMPO.REGULACAO.TARM");
            if (tempoRegulacaoTarm != null) {
                Long segundos = parseTimeToSeconds(tempoRegulacaoTarm);
                pc.setDurationSeconds(segundos);
                pc.setParametros(NestedScoringParameters.builder()
                        .tarm(ScoringSectionParams.builder()
                                .regulacao(List.of(ScoringRule.builder().duration(segundos).build()))
                                .build())
                        .build());
                log.debug("Tempo de regulação TARM definido: {} segundos", segundos);
            }
        }

        if (Objects.equals(pc.getRole(), "FROTA")) {
            String tempoRegulacaoFrota = data.get("TEMPO.REGULACAO.FROTA");
            if (tempoRegulacaoFrota != null) {
                Long segundos = parseTimeToSeconds(tempoRegulacaoFrota);
                pc.setDurationSeconds(segundos);
                pc.setParametros(NestedScoringParameters.builder()
                        .frota(ScoringSectionParams.builder()
                                .regulacao(List.of(ScoringRule.builder().duration(segundos).build()))
                                .build())
                        .build());
                log.debug("Tempo de regulação FROTA definido: {} segundos", segundos);
            }
        }

        if (Objects.equals(pc.getRole(), "MEDICO")) {
            if (pc.getMedicoRole().equals(MedicoRole.REGULADOR)) {
                String tempoRegulacao = data.get("TEMPO.REGULACAO");
                if (tempoRegulacao != null) {
                    Long segundos = parseTimeToSeconds(tempoRegulacao);
                    pc.setDurationSeconds(segundos);
                    pc.setParametros(NestedScoringParameters.builder()
                            .medico(ScoringSectionParams.builder()
                                    .regulacao(List.of(ScoringRule.builder().duration(segundos).build()))
                                    .build())
                            .build());
                    log.debug("Tempo de regulação MÉDICO REGULADOR definido: {} segundos", segundos);
                }
            } else if (pc.getMedicoRole().equals(MedicoRole.LIDER)) {
                String criticos = data.get("CRITICOS");
                if (criticos != null) {
                    Long segundos = parseTimeToSeconds(criticos);
                    pc.setDurationSeconds(segundos);
                    pc.setParametros(NestedScoringParameters.builder()
                            .medico(ScoringSectionParams.builder()
                                    .regulacaoLider(List.of(ScoringRule.builder().duration(segundos).build()))
                                    .build())
                            .build());
                    log.debug("Tempo de críticos MÉDICO LÍDER definido: {} segundos", segundos);
                }
            }
        }
    }

    public List<CollaboratorsResponse> getAllProjectCollaborators(String projectId) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        return projeto.getCollaborators().stream()
                .map(pc -> {
                    CollaboratorEntity collab = collaboratorRepo.findById(pc.getCollaboratorId())
                            .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));

                    int pontuacao = 0;
                    if (pc.getPontuacao() != null) {
                        pontuacao = pc.getPontuacao();
                    }

                    return new CollaboratorsResponse(
                            pc.getCollaboratorId(),
                            pc.getNome(),
                            collab.getCpf(),
                            collab.getIdCallRote(),
                            pontuacao,
                            pc.getRole(),
                            pc.getShiftHours() ,
                            pc.getMedicoRole()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjetoEntity updateProjectCollaborator(
            String projectId,
            String collaboratorId,
            ProjectCollabRequest dto,
            boolean wasEdited
    ) {
        log.info("Atualizando colaborador [{}] no projeto [{}] em {} tal {}", dto, projectId, wasEdited, dto);

        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        CollaboratorEntity collab = collaboratorRepo.findById(collaboratorId)
                .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));

        projeto.getCollaborators()
                .stream()
                .filter(pc -> pc.getCollaboratorId().equals(collaboratorId))
                .findFirst()
                .ifPresent(pc -> {
                    if (!wasEdited || !pc.getWasEdited() ) {
                        SheetRow sheetColab = getSheetRowForCollaborator(collaboratorId, projectId, collab.getNome());
                        if (sheetColab != null) {
                            processSheetRowData(pc, sheetColab);
                        }
                    }
                    pc.setNome(dto.getNome());
                    pc.setRole(dto.getRole());
                    pc.setMedicoRole(dto.getMedicoRole() != null ? dto.getMedicoRole() : MedicoRole.NENHUM);
                    pc.setShiftHours(dto.getShiftHours());
                    pc.setWasEdited(wasEdited);
                    pc.setSaidaVtrSeconds(dto.getSaidaVtr() != null ? dto.getSaidaVtr() : pc.getSaidaVtrSeconds());
                    pc.setDurationSeconds(dto.getDurationSeconds() != null ? dto.getDurationSeconds() : pc.getDurationSeconds());
                    pc.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : pc.getQuantity());
                    pc.setPausaMensalSeconds(dto.getPausaMensalSeconds() != null ? dto.getPausaMensalSeconds() : pc.getPausaMensalSeconds());

                    log.info("Colaborador tal {}", pc);

                    int pontos = pc.getPontuacao();
                    if (pc.getDurationSeconds() != null) {
                         pontos = collabParams.setParams(pc, projeto, pc.getDurationSeconds(), pc.getQuantity(), pc.getPausaMensalSeconds(), pc.getSaidaVtrSeconds());
                    }


                    pc.setPontuacao(pontos);
                    syncCollaboratorData(collaboratorId);
                });

        return projetoRepo.save(projeto);
    }

    @Transactional
    public void removeCollaborator(String projectId, String collaboratorId) {
        log.warn("Removendo colaborador [{}] do projeto [{}]", collaboratorId, projectId);
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        projeto.getCollaborators().removeIf(pc -> pc.getCollaboratorId().equals(collaboratorId));
        projetoRepo.save(projeto);
    }

    @Transactional
    public void syncCollaboratorData(String collaboratorId) {
        log.info("Sincronizando (apenas) IDs do colaborador [{}]", collaboratorId);

        List<ProjetoEntity> projetos = projetoRepo.findByCollaboratorsCollaboratorId(collaboratorId);

        projetoRepo.saveAll(projetos);
    }

    public static NestedScoringParameters convertMapToNested(Map<String, Integer> flatParams) {
        NestedScoringParameters nested = new NestedScoringParameters();
        if (flatParams == null || flatParams.isEmpty()) {
            log.warn("Mapa de parâmetros plano está nulo ou vazio. Retornando estrutura de parâmetros aninhada vazia.");
            nested.setTarm(new ScoringSectionParams(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
            nested.setFrota(new ScoringSectionParams(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
            nested.setMedico(new ScoringSectionParams(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
            return nested;
        }

        nested.setTarm(convertSection(flatParams, "tarm"));
        nested.setFrota(convertSection(flatParams, "frota"));
        nested.setMedico(convertSection(flatParams, "medico"));

        return nested;
    }


    private static ScoringSectionParams convertSection(Map<String, Integer> params, String section) {
        return ScoringSectionParams.builder()
                .removidos(extractRules(params, section, "removidos"))
                .regulacao(extractRules(params, section, "regulacao"))
                .pausas(extractRules(params, section, "pausas"))
                .saidaVtr(extractRules(params, section, "saidaVtr"))
                .regulacaoLider(extractRules(params, section, "regulacaoLider"))
                .build();
    }

    private static List<ScoringRule> extractRules(Map<String, Integer> params, String section, String field) {
        List<ScoringRule> rules = new ArrayList<>();
        int index = 0;

        while (true) {
            String quantityKey = String.format("%s_%s_%d_quantity", section, field, index);
            String durationKey = String.format("%s_%s_%d_duration", section, field, index);
            String pointsKey = String.format("%s_%s_%d_points", section, field, index);

            if (!params.containsKey(pointsKey)) break;

            rules.add(new ScoringRule(
                    params.getOrDefault(quantityKey, 0),
                    parseDuration(params.get(durationKey)),
                    params.get(pointsKey)
            ));
            index++;
        }

        return rules;
    }

    private static String parseDuration(Integer seconds) {
        return seconds != null ? Duration.ofSeconds(seconds).toString() : null;
    }

}
