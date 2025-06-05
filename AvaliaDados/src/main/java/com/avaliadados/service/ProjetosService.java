package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.MedicoEntity;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.MedicoRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;


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

    private NestedScoringParameters mergeParameters(NestedScoringParameters existing, NestedScoringParameters updates) {
        if (existing == null) existing = new NestedScoringParameters();
        if (updates == null) return existing;

        existing.setColab(mergeSection(existing.getColab(), updates.getColab()));
        existing.setTarm(mergeSection(existing.getTarm(), updates.getTarm()));
        existing.setFrota(mergeSection(existing.getFrota(), updates.getFrota()));
        existing.setMedico(mergeSection(existing.getMedico(), updates.getMedico()));

        return existing;
    }

    private ScoringSectionParams mergeSection(ScoringSectionParams existing, ScoringSectionParams updates) {
        if (existing == null) existing = new ScoringSectionParams();
        if (updates == null) return existing;

        existing.setPausas(mergeRules(existing.getPausas(), updates.getPausas()));
        existing.setRegulacao(mergeRules(existing.getRegulacao(), updates.getRegulacao()));

        if (updates.getRemovidos() != null) {
            existing.setRemovidos(new ArrayList<>(updates.getRemovidos()));
        }
        existing.setSaidaVtr(mergeRules(existing.getSaidaVtr(), updates.getSaidaVtr()));
        existing.setRegulacaoLider(mergeRules(existing.getRegulacaoLider(), updates.getRegulacaoLider()));

        return existing;
    }

    private List<ScoringRule> mergeRules(List<ScoringRule> existing, List<ScoringRule> updates) {
        if (existing == null) existing = new ArrayList<>();
        if (updates == null) return existing;

        if (updates.isEmpty()) return existing;

        Map<Long, ScoringRule> ruleMap = existing.stream()
                .collect(Collectors.toMap(
                        rule -> rule.getDuration() != null ? rule.getDuration() : 0L,
                        r -> r,
                        (r1, r2) -> r1
                ));

        updates.forEach(update -> {
            Long key = update.getDuration() != null ? update.getDuration() : 0L;
            if (ruleMap.containsKey(key)) {
                ScoringRule existingRule = ruleMap.get(key);
                if (update.getPoints() != null) {
                    existingRule.setPoints(update.getPoints());
                }
                if (update.getQuantity() != null) {
                    existingRule.setQuantity(update.getQuantity());
                }
            } else {
                ruleMap.put(key, update);
            }
        });

        return new ArrayList<>(ruleMap.values());
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

        String[] mesAno = novo.getMonth().split("-");

        Month mesReal = Month.of(Integer.parseInt(Arrays.stream(mesAno).findFirst().get()));
        String nome = String.format("%02d", mesReal.getValue());
        Year anoReal = Year.of(Integer.parseInt(Arrays.stream(mesAno).toList().getLast()));
        var dia = mesReal.length(anoReal.isLeap());
        
        if (nome.length() == 1) {
            nome = "0" + nome;
        }
        System.out.println(dia + "/" + nome + "/" + anoReal.getValue());

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
