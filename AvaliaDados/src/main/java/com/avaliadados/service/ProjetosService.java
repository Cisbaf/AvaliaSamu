package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjetosService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final ScoringService scoringService;

    public ProjetoEntity updateProjeto(String id, Map<String, Object> updates) {
        var p = projetoRepo.findById(id)
                .orElseThrow();
        if (updates.containsKey("parameters")) {
            Map<String, Integer> params = ((Map<String, Number>) updates.get("parameters")).entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> e.getValue().intValue()
                    ));
            p.setParameters(params);

            for (ProjectCollaborator collaborator : p.getCollaborators()) {
                recalculateCollaboratorPoints(collaborator, p);
            }
        }
        p.setUpdatedAt(Instant.now());
        return projetoRepo.save(p);
    }

    private void recalculateCollaboratorPoints(ProjectCollaborator collaborator, ProjetoEntity projeto) {
        String role = collaborator.getRole();
        Integer quantity = collaborator.getQuantity();
        Long durationSeconds = collaborator.getDurationSeconds();
        Long pausaMensalSeconds = collaborator.getPausaMensalSeconds();

        // 1) thresholds do projeto
        Map<String, Integer> params = projeto.getParameters().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue  // já é Integer
                ));

        // 2) valores brutos do colaborador em Duration / Integer
        Duration regDur = durationSeconds != null
                ? Duration.ofSeconds(durationSeconds)
                : null;
        Duration pauseDur = pausaMensalSeconds != null
                ? Duration.ofSeconds(pausaMensalSeconds)
                : null;

        // 3) cálculo unificado
        int pontos = scoringService.calculateScore(
                role,
                regDur,
                quantity,
                pauseDur,
                params
        );

        // 4) atualiza a pontuação
        collaborator.setPontuacao(pontos);
    }

    public ProjetoEntity createProjetoWithCollaborators(ProjetoEntity projeto) {
        ProjetoEntity novoProjeto = projetoRepo.save(projeto);

        List<CollaboratorEntity> globais = collaboratorRepo.findAll();

        List<ProjectCollaborator> projectCollabs = globais.stream().map(g ->
                ProjectCollaborator.builder()
                        .collaboratorId(g.getId())
                        .role(g.getRole())
                        .pontuacao(0)
                        .build()
        ).toList();

        novoProjeto.setCollaborators(projectCollabs);
        return projetoRepo.save(novoProjeto);
    }

    public List<ProjetoEntity> getAllProjeto() {
        return projetoRepo.findAll();
    }

    public void deleteProject(String projectId) {
        projetoRepo.deleteById(projectId);
    }

}
