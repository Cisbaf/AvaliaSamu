package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjetosService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final ScoringService scoringService;
    private final ObjectMapper objectMapper;

    public ProjetoEntity updateProjeto(String id, Map<String, Object> updates) {
        var p = projetoRepo.findById(id).orElseThrow();

        if (updates.containsKey("parameters")) {
            NestedScoringParameters newParams = objectMapper.convertValue(
                    updates.get("parameters"), NestedScoringParameters.class);

            // Mesclar com parâmetros existentes
            NestedScoringParameters mergedParams = mergeParameters(p.getParameters(), newParams);
            p.setParameters(mergedParams);

            p.getCollaborators().forEach(collab ->
                    recalculateCollaboratorPoints(collab, p)
            );
        }

        p.setUpdatedAt(Instant.now());
        return projetoRepo.save(p);
    }

    private NestedScoringParameters mergeParameters(NestedScoringParameters existing, NestedScoringParameters updates) {
        if (existing == null) existing = new NestedScoringParameters();
        if (updates == null) return existing;

        // Mesclar seção TARM
        existing.setTarm(mergeSection(existing.getTarm(), updates.getTarm()));
        // Mesclar seção FROTA
        existing.setFrota(mergeSection(existing.getFrota(), updates.getFrota()));
        // Mesclar seção MEDICO
        existing.setMedico(mergeSection(existing.getMedico(), updates.getMedico()));

        return existing;
    }

    private ScoringSectionParams mergeSection(ScoringSectionParams existing, ScoringSectionParams updates) {
        if (existing == null) existing = new ScoringSectionParams();
        if (updates == null) return existing;

        // Mesclar lista de regras
        existing.setRegulacao(mergeRules(existing.getRegulacao(), updates.getRegulacao()));
        // Repetir para outras listas (removidos, pausas, etc.)

        return existing;
    }

    private List<ScoringRule> mergeRules(List<ScoringRule> existing, List<ScoringRule> updates) {
        Map<Long, ScoringRule> ruleMap = existing.stream()
                .collect(Collectors.toMap(ScoringRule::getDuration, r -> r));

        updates.forEach(update -> {
            if (ruleMap.containsKey(update.getDuration())) {
                ScoringRule existingRule = ruleMap.get(update.getDuration());
                existingRule.setPoints(update.getPoints() != null ?
                        update.getPoints() : existingRule.getPoints());
            } else {
                ruleMap.put(update.getDuration(), update);
            }
        });

        return new ArrayList<>(ruleMap.values());
    }

    private void recalculateCollaboratorPoints(ProjectCollaborator collaborator, ProjetoEntity projeto) {
        if (collaborator.getMedicoRole() == null)
            collaborator.setMedicoRole(MedicoRole.valueOf("NENHUM"));
        int pontos = scoringService.calculateCollaboratorScore(
                collaborator.getRole(),
                collaborator.getMedicoRole().name(),
                collaborator.getDurationSeconds(),
                collaborator.getQuantity(),
                collaborator.getPausaMensalSeconds(),
                projeto.getParameters()
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
