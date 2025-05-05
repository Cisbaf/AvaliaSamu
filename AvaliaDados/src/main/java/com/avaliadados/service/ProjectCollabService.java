package com.avaliadados.service;

import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectCollabService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final CollaboratorsService collaboratorsService;

    public ProjetoEntity addCollaborator(String projectId, String collaboratorId, String role) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        collaboratorRepo.findById(collaboratorId) // Verifica se existe
                .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));

        ProjectCollaborator pc = new ProjectCollaborator();
        pc.setCollaboratorId(collaboratorId);
        pc.setRole(role);


        projeto.getCollaborators().add(pc);
        return projetoRepo.save(projeto);
    }

    public List<CollaboratorsResponse> getAllProjectCollaborators(String projectId) {
        return projetoRepo.findById(projectId)
                .map(proj -> proj.getCollaborators().stream()
                        .map(ProjectCollaborator::getCollaboratorId)
                        .distinct()
                        .map(collaboratorsService::findByid)
                        .collect(Collectors.toList())
                ).orElse(Collections.emptyList());
    }

    public ProjetoEntity updateCollaboratorRole(String projectId, String collaboratorId, String newRole) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        projeto.getCollaborators().stream()
                .filter(pc -> pc.getCollaboratorId().equals(collaboratorId))
                .findFirst()
                .ifPresent(pc -> pc.setRole(newRole));

        return projetoRepo.save(projeto);
    }

    public ProjetoEntity removeCollaborator(String projectId, String collaboratorId) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        projeto.getCollaborators().removeIf(pc -> pc.getCollaboratorId().equals(collaboratorId));
        projeto.setUpdatedAt(Instant.now());
        return projetoRepo.save(projeto);
    }



}
