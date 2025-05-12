package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjetosService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final ScoringService scoringService;
    private final ObjectMapper objectMapper;  // para converter Map -> NestedScoringParameters


    public ProjetoEntity updateProjeto(String id, Map<String, Object> updates) {
        var p = projetoRepo.findById(id).orElseThrow();
        if (updates.containsKey("parameters")) {
            NestedScoringParameters nested = objectMapper.convertValue(
                    updates.get("parameters"), NestedScoringParameters.class);
            p.setParameters(nested);

            p.getCollaborators().forEach(collab ->
                    recalculateCollaboratorPoints(collab, p)
            );
        }
        p.setUpdatedAt(Instant.now());
        return projetoRepo.save(p);
    }

    private void recalculateCollaboratorPoints(ProjectCollaborator collaborator, ProjetoEntity projeto) {
        int pontos = scoringService.calculateCollaboratorScore(
                collaborator.getRole(),
                collaborator.getDurationSeconds(),
                collaborator.getQuantity(),
                collaborator.getPausaMensalSeconds(),
                projeto.getParameters()  // agora nested
        );
        collaborator.setPontuacao(pontos);
    }

    public ProjetoEntity createProjetoWithCollaborators(ProjetoEntity projeto) {
        projeto.setCreatedAt(Instant.now());
        ProjetoEntity novo = projetoRepo.save(projeto);
        List<CollaboratorEntity> globais = collaboratorRepo.findAll();
        var collabs = globais.stream().map(g ->
                ProjectCollaborator.builder()
                        .collaboratorId(g.getId())
                        .role(g.getRole())
                        .pontuacao(0)
                        .build()
        ).toList();
        novo.setCollaborators(collabs);
        return projetoRepo.save(novo);
    }


    public List<ProjetoEntity> getAllProjeto() {
        return projetoRepo.findAll();
    }

    public void deleteProject(String projectId) {
        projetoRepo.deleteById(projectId);
    }

}
