package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectCollabService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;

    public ProjetoEntity addCollaborator(String projectId, String collaboratorId, String role) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        CollaboratorEntity collaborator = collaboratorRepo.findById(collaboratorId)
                .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));

        ProjectCollaborator pc = ProjectCollaborator.builder()
                .collaboratorId(collaboratorId)
                .nome(collaborator.getNome())
                .role(role)
                .points(collaborator.getPontuacao())
                .build();

        projeto.getCollaborators().removeIf(p -> p.getCollaboratorId().equals(collaboratorId));
        projeto.getCollaborators().add(pc);

        return projetoRepo.save(projeto);
    }


    public List<CollaboratorsResponse> getAllProjectCollaborators(String projectId) {
        Optional<ProjetoEntity> projetoOpt = projetoRepo.findById(projectId);

        if (projetoOpt.isEmpty()) {
            return Collections.emptyList();
        }

        ProjetoEntity projeto = projetoOpt.get();
        List<ProjectCollaborator> collaborators = projeto.getCollaborators();
        List<CollaboratorsResponse> result = new ArrayList<>();

        for (ProjectCollaborator pc : collaborators) {
            String collaboratorId = pc.getCollaboratorId();
            var response = collaboratorRepo.findById(collaboratorId).orElseThrow();
            var newResponse = transformToProject(response, pc);

            result.add(newResponse);
        }

        return result;
    }

    private CollaboratorsResponse transformToProject(CollaboratorEntity response, ProjectCollaborator pc) {
        return new CollaboratorsResponse(
                pc.getCollaboratorId(),
                pc.getNome(),
                response.getCpf(),
                response.getIdCallRote(),
                pc.getPoints(),
                pc.getRole()
        );
    }


    @Transactional
    public ProjetoEntity updateCollaboratorRole(String projectId, String collaboratorId, String newRole) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        projeto.getCollaborators().stream()
                .filter(pc -> pc.getCollaboratorId().equals(collaboratorId))
                .findFirst()
                .ifPresent(projectCollaborator -> projectCollaborator.setRole(newRole));

        return projetoRepo.save(projeto);
    }

    public void removeCollaborator(String projectId, String collaboratorId) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        projeto.getCollaborators().removeIf(pc -> pc.getCollaboratorId().equals(collaboratorId));
        projeto.setUpdatedAt(Instant.now());
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


}
