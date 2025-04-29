package com.avaliadados.service;

import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.repository.ProjetoRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ProjetosService {

    private final ProjetoRepository projetoRepo;

    public ProjetosService(ProjetoRepository projetoRepo) {
        this.projetoRepo = projetoRepo;
    }

    public Optional<ProjetoEntity> getProjeto(String projectId) {
        return projetoRepo.findById(projectId);
    }

    public ProjetoEntity addCollaborator(String projectId, Long collaboratorId, String role) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        ProjectCollaborator pc = ProjectCollaborator.builder()
                .collaboratorId(collaboratorId)
                .points(0)
                .addedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        projeto.getCollaborators().add(pc);
        projeto.setUpdatedAt(Instant.now());
        return projetoRepo.save(projeto);
    }

    public ProjetoEntity updateCollaboratorRole(String projectId, Long collaboratorId, String newRole) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        projeto.getCollaborators().stream()
                .filter(pc -> pc.getId().equals(collaboratorId))
                .findFirst()
                .ifPresent(pc -> {
                    pc.setRole(newRole);
                });
        projeto.setUpdatedAt(Instant.now());
        return projetoRepo.save(projeto);
    }

    public ProjetoEntity removeCollaborator(String projectId, Long collaboratorId) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        projeto.getCollaborators().removeIf(pc -> pc.getId().equals(collaboratorId));
        projeto.setUpdatedAt(Instant.now());
        return projetoRepo.save(projeto);
    }

    public ProjetoEntity createProjeto(ProjetoEntity projeto) {
        return  projetoRepo.save(projeto);
    }

    public List<ProjetoEntity> getAllProjetos() {
        return projetoRepo.findAll();
    }
}
