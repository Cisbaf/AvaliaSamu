package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.MedicoEntity;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.MedicoRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class ProjetosService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final MedicoRepository medicoRepository;
    private final ScoringService scoringService;
    private final ObjectMapper objectMapper;

    public ProjetoEntity updateProjeto(String id, Map<String, Object> updates) {
        var p = projetoRepo.findById(id).orElseThrow();

        if (updates.containsKey("parameters")) {
            NestedScoringParameters newParams = objectMapper.convertValue(
                    updates.get("parameters"), NestedScoringParameters.class);

            p.setParameters(newParams);

            p.getCollaborators().forEach(collab ->
                    recalculateCollaboratorPoints(collab, p)
            );
        }

        p.setUpdatedAt(Instant.now());
        return projetoRepo.save(p);
    }


    private void recalculateCollaboratorPoints(ProjectCollaborator collaborator, ProjetoEntity projeto) {
        if (collaborator.getMedicoRole() == null)
            collaborator.setMedicoRole(MedicoRole.valueOf("NENHUM"));
        Map<String, Integer> pontos = scoringService.calculateCollaboratorScore(
                collaborator.getRole(),
                collaborator.getMedicoRole().name(),
                "H12",
                collaborator.getDurationSeconds(),
                collaborator.getCriticos(),
                collaborator.getRemovidos(),
                collaborator.getPausaMensalSeconds(),
                collaborator.getSaidaVtrSeconds(),
                projeto.getParameters()
        );
        collaborator.setPoints(pontos);
    }

    public ProjetoEntity createProjetoWithCollaborators(ProjetoEntity projeto) {
        projeto.setCreatedAt(Instant.now());
        ProjetoEntity novo = projetoRepo.save(projeto);
        List<CollaboratorEntity> globais = collaboratorRepo.findAll();
        List<MedicoEntity> medicos = medicoRepository.findAll();


        var collabs = globais.stream().map(g -> {
                    if (!Objects.equals(g.getRole(), "MEDICO")) {
                        return ProjectCollaborator.builder()
                                .nome(g.getNome())
                                .collaboratorId(g.getId())
                                .role(g.getRole())
                                .build();
                    }
                    return medicos.stream()
                            .filter(m -> m.getId().equals(g.getId()))
                            .map(m -> ProjectCollaborator.builder()
                                    .nome(m.getNome())
                                    .collaboratorId(m.getId())
                                    .role(g.getRole())
                                    .medicoRole(m.getMedicoRole())
                                    .shiftHours(m.getShiftHours())
                                    .build())
                            .findFirst()
                            .orElse(null);
                }
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
