package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCollabService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final ScoringService scoringService;

    @Transactional
    public ProjetoEntity addCollaborator(
            String projectId,
            String collaboratorId,
            String role,
            Long durationSeconds,
            Integer quantity,
            Long pausaMensalSeconds,
            Map<String, Integer> parametros
    ) {
        log.info("Adicionando colaborador [{}] ao projeto [{}] com role [{}]", collaboratorId, projectId, role);

        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        CollaboratorEntity collab = collaboratorRepo.findById(collaboratorId)
                .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));

        Map<String, Integer> thresholds = (parametros != null && !parametros.isEmpty())
                ? parametros
                : projeto.getParameters();

        log.debug("Parâmetros usados para pontuação: {}", thresholds);

        Duration regDur = durationSeconds != null ? Duration.ofSeconds(durationSeconds) : null;
        Duration pauseDur = pausaMensalSeconds != null ? Duration.ofSeconds(pausaMensalSeconds) : null;

        log.debug("Valores recebidos para cálculo: duração={}, quantidade={}, pausa={}",
                regDur, quantity, pauseDur);

        int pontos = scoringService.calculateScore(
                role,
                regDur,
                quantity,
                pauseDur,
                thresholds
        );

        log.info("Pontuação calculada para colaborador [{}]: {}", collaboratorId, pontos);

        ProjectCollaborator pc = ProjectCollaborator.builder()
                .collaboratorId(collaboratorId)
                .nome(collab.getNome())
                .role(role)
                .durationSeconds(durationSeconds)
                .quantity(quantity)
                .pausaMensalSeconds(pausaMensalSeconds)
                .parametros(thresholds)
                .pontuacao(pontos)
                .build();

        projeto.getCollaborators().removeIf(p -> p.getCollaboratorId().equals(collaboratorId));
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
                    return new CollaboratorsResponse(
                            pc.getCollaboratorId(),
                            collab.getNome(),
                            collab.getCpf(),
                            collab.getIdCallRote(),
                            pc.getPontuacao(),
                            pc.getRole()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjetoEntity updatePjCollaborator(
            String projectId,
            String collaboratorId,
            String newRole,
            Long durationSeconds,
            Integer quantity,
            Long pausaMensalSeconds,
            Map<String, Integer> parametros
    ) {
        log.info("Atualizando colaborador [{}] no projeto [{}] com nova role [{}]", collaboratorId, projectId, newRole);

        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        projeto.getCollaborators().stream()
                .filter(pc -> pc.getCollaboratorId().equals(collaboratorId))
                .findFirst()
                .ifPresent(pc -> {
                    pc.setRole(newRole);
                    pc.setDurationSeconds(durationSeconds);
                    pc.setQuantity(quantity);
                    pc.setPausaMensalSeconds(pausaMensalSeconds);
                    pc.setParametros(parametros);

                    Duration regDur = durationSeconds != null ? Duration.ofSeconds(durationSeconds) : null;
                    Duration pauseDur = pausaMensalSeconds != null ? Duration.ofSeconds(pausaMensalSeconds) : null;

                    log.debug("Atualização - Valores: duração={}, quantidade={}, pausa={}, thresholds={}",
                            regDur, quantity, pauseDur, parametros);

                    int pontos = scoringService.calculateScore(
                            newRole,
                            regDur,
                            quantity,
                            pauseDur,
                            parametros
                    );

                    log.info("Nova pontuação calculada: {}", pontos);
                    pc.setPontuacao(pontos);
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
}
