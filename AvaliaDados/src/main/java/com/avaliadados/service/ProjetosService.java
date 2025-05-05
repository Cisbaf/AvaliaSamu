package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjetosService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final CollaboratorsService collaboratorsService;


    public List<CollaboratorsResponse> getAllProjectCollaborators(String projectId) {
        return projetoRepo.findById(projectId)
                .map(proj -> proj.getCollaborators().stream()
                        .map(ProjectCollaborator::getCollaboratorId)
                        .distinct()
                        .map(collaboratorsService::findByid)
                        .collect(Collectors.toList())
                ).orElse(Collections.emptyList());
    }

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

    public ProjetoEntity updateCollaboratorRole(String projectId, String collaboratorId, String newRole) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        projeto.getCollaborators().stream()
                .filter(pc -> pc.getCollaboratorId().equals(collaboratorId))
                .findFirst()
                .ifPresent(pc -> pc.setRole(newRole));

        return projetoRepo.save(projeto);
    }

    public ProjetoEntity updateProjeto(String id, Map<String, Object> updates) {
        var p = projetoRepo.findById(id)
                .orElseThrow();
        if (updates.containsKey("parameters")) {
            Map<String, Double> params = (Map<String, Double>) updates.get("parameters");
            p.setParameters(params);
        }
        p.setUpdatedAt(Instant.now());
        return projetoRepo.save(p);
    }

    public ProjetoEntity removeCollaborator(String projectId, String collaboratorId) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        projeto.getCollaborators().removeIf(pc -> pc.getCollaboratorId().equals(collaboratorId));
        projeto.setUpdatedAt(Instant.now());
        return projetoRepo.save(projeto);
    }

    public List<ProjetoEntity> getAllProjeto() {
        return projetoRepo.findAll();
    }

    public ProjetoEntity createProjetoWithCollaborators(ProjetoEntity projeto) {
        // Cria o projeto vazio
        ProjetoEntity novoProjeto = projetoRepo.save(projeto);

        // Busca todos colaboradores globais
        List<CollaboratorEntity> globais = collaboratorRepo.findAll();

        // Converte para ProjectCollaborator
        List<ProjectCollaborator> projectCollabs = globais.stream().map(g ->
                ProjectCollaborator.builder()
                        .collaboratorId(g.getId())
                        .role(g.getRole())
                        .points(0)
                        .build()
        ).toList();

        // Atualiza o projeto com os colaboradores
        novoProjeto.setCollaborators(projectCollabs);
        return projetoRepo.save(novoProjeto);
    }

}
