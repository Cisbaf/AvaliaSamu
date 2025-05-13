package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.ProjectCollabRequest;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.DTO.UpdateProjectCollabRequest;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.MedicoEntityRepository;
import com.avaliadados.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCollabService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final ScoringService scoringService;
    private final MedicoEntityRepository medicoRepo;

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

        Duration regDur = dto.getDurationSeconds() != null ?
                Duration.ofSeconds(dto.getDurationSeconds()) : Duration.ofSeconds(0);
        Duration pauseDur = dto.getPausaMensalSeconds() != null ?
                Duration.ofSeconds(dto.getPausaMensalSeconds()) : Duration.ofSeconds(0);
        Integer quantity = dto.getQuantity() != null ? dto.getQuantity() : 0;

        log.debug("Valores recebidos para cálculo: duração={}, quantidade={}, pausa={}",
                regDur, quantity, pauseDur);

        int pontos = scoringService.calculateCollaboratorScore(
                dto.getRole(),
                dto.getDurationSeconds(),
                dto.getQuantity(),
                dto.getPausaMensalSeconds(),
                scoringParams
        );

        log.info("Pontuação calculada para colaborador [{}]: {}", dto.getCollaboratorId(), pontos);

        ProjectCollaborator pc = ProjectCollaborator.builder()
                .collaboratorId(dto.getCollaboratorId())
                .nome(collab.getNome())
                .role(dto.getRole())
                .durationSeconds(dto.getDurationSeconds())
                .quantity(dto.getQuantity())
                .pausaMensalSeconds(dto.getPausaMensalSeconds())
                .parametros(scoringParams)
                .pontuacao(pontos)
                .build();

        projeto.getCollaborators().removeIf(p -> p.getCollaboratorId().equals(dto.getCollaboratorId()));
        projeto.getCollaborators().add(pc);
        return projetoRepo.save(projeto);
    }

    public List<CollaboratorsResponse> getAllProjectCollaborators(String projectId) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        return projeto.getCollaborators().stream()
                .map(pc -> {
                    CollaboratorEntity collab = collaboratorRepo.findById(pc.getCollaboratorId())
                            .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));
                    if (Objects.equals(collab.getRole(), "MEDICO")) {
                        var medico = medicoRepo.findById(collab.getId());
                        log.info("Medico é isso aqui: {}", medico);
                        if (medico.isPresent()) {
                            pc.setMedicoRole(medico.get().getMedicoRole());
                            pc.setShiftHours(medico.get().getShiftHours());
                        } else {
                            throw new RuntimeException("Médico não encontrado");
                        }
                    }
                    return new CollaboratorsResponse(
                            pc.getCollaboratorId(),
                            collab.getNome(),
                            collab.getCpf(),
                            collab.getIdCallRote(),
                            pc.getPontuacao(),
                            pc.getRole(),
                            pc.getShiftHours(),
                            pc.getMedicoRole()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjetoEntity updateProjectCollaborator(
            String projectId,
            String collaboratorId,
            UpdateProjectCollabRequest dto
    ) {
        log.info("Atualizando colaborador [{}] no projeto [{}]", collaboratorId, projectId);

        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        projeto.getCollaborators()
                .stream()
                .filter(pc -> pc.getCollaboratorId().equals(collaboratorId))
                .findFirst()
                .ifPresent(pc -> {
                    pc.setRole(dto.getRole());
                    pc.setDurationSeconds(dto.getDurationSeconds());
                    pc.setQuantity(dto.getQuantity());
                    pc.setPausaMensalSeconds(dto.getPausaMensalSeconds());

                    int pontos = scoringService.calculateCollaboratorScore(
                            pc.getRole(),
                            pc.getDurationSeconds(),
                            pc.getQuantity(),
                            pc.getPausaMensalSeconds(),
                            projeto.getParameters()
                    );
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

    public void syncCollaboratorData(String collaboratorId) {
        CollaboratorEntity collaborator = collaboratorRepo.findById(collaboratorId)
                .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));

        List<ProjetoEntity> projetos = projetoRepo.findByCollaboratorsCollaboratorId(collaboratorId);

        projetos.forEach(projeto -> {
            projeto.getCollaborators().stream()
                    .filter(pc -> pc.getCollaboratorId().equals(collaboratorId))
                    .findFirst()
                    .ifPresent(pc -> pc.setNome(collaborator.getNome()));
            projetoRepo.save(projeto);
        });
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
