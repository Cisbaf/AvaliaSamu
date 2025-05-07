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

    public ProjetoEntity updateProjeto(String id, Map<String, Object> updates) {
        var p = projetoRepo.findById(id)
                .orElseThrow();
        if (updates.containsKey("parameters")) {
            Map<String, Integer> params = (Map<String, Integer>) updates.get("parameters");
            p.setParameters(params);
        }
        p.setUpdatedAt(Instant.now());
        return projetoRepo.save(p);
    }

    public ProjetoEntity createProjetoWithCollaborators(ProjetoEntity projeto) {
        ProjetoEntity novoProjeto = projetoRepo.save(projeto);

        List<CollaboratorEntity> globais = collaboratorRepo.findAll();

        List<ProjectCollaborator> projectCollabs = globais.stream().map(g ->
                ProjectCollaborator.builder()
                        .collaboratorId(g.getId())
                        .role(g.getRole())
                        .points(0)
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
