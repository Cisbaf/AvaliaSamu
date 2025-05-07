// src/main/java/com/avaliadados/service/ProjectCollabService.java
package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

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
            Integer quantity
    ) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        CollaboratorEntity collab = collaboratorRepo.findById(collaboratorId)
                .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));

        // calcula pontos dinamicamente
        int points;
        if (durationSeconds != null) {
            points = scoringService.scoreByRoleDuration(role,
                    Duration.ofSeconds(durationSeconds));
        } else if (quantity != null) {
            points = scoringService.scoreByRoleQuantity(role, quantity);
        } else {
            points = 0;
        }

        ProjectCollaborator pc = ProjectCollaborator.builder()
                .collaboratorId(collaboratorId)
                .nome(collab.getNome())
                .role(role)
                .durationSeconds(durationSeconds)
                .quantity(quantity)
                .points(points)
                .build();

        // substitui se já existia
        projeto.getCollaborators().removeIf(p -> p.getCollaboratorId().equals(collaboratorId));
        projeto.getCollaborators().add(pc);
        return projetoRepo.save(projeto);
    }

    public List<CollaboratorsResponse> getAllProjectCollaborators(String projectId) {
        return projetoRepo.findById(projectId)
                .map(proj -> proj.getCollaborators().stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    private CollaboratorsResponse toResponse(ProjectCollaborator pc) {
        var collab = collaboratorRepo.findById(pc.getCollaboratorId())
                .orElseThrow();
        return new CollaboratorsResponse(
                pc.getCollaboratorId(),
                pc.getNome(),
                collab.getCpf(),
                collab.getIdCallRote(),
                pc.getPoints(),
                pc.getRole()
        );
    }

    @Transactional
    public ProjetoEntity updateCollaboratorRole(
            String projectId,
            String collaboratorId,
            String newRole,
            Long durationSeconds,
            Integer quantity
    ) {
        // mesma lógica de recalcular pontos
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        projeto.getCollaborators().stream()
                .filter(pc -> pc.getCollaboratorId().equals(collaboratorId))
                .findFirst()
                .ifPresent(pc -> {
                    pc.setRole(newRole);
                    if (durationSeconds != null) {
                        pc.setDurationSeconds(durationSeconds);
                        pc.setPoints(scoringService.scoreByRoleDuration(newRole,
                                Duration.ofSeconds(durationSeconds)));
                    } else if (quantity != null) {
                        pc.setQuantity(quantity);
                        pc.setPoints(scoringService.scoreByRoleQuantity(newRole, quantity));
                    }
                });

        return projetoRepo.save(projeto);
    }

    public void removeCollaborator(String projectId, String collaboratorId) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        projeto.getCollaborators().removeIf(pc -> pc.getCollaboratorId().equals(collaboratorId));
        projetoRepo.save(projeto);
    }
}
