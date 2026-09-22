package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.WorkPeriod;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringParametersByPeriod;
import com.avaliadados.model.roles.MedicoEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.MedicoRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
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

        if (updates.containsKey("scoringParameters")) {
            ScoringParametersByPeriod newParams = objectMapper.convertValue(
                    updates.get("scoringParameters"), ScoringParametersByPeriod.class);

            p.setScoringParameters(normalizeScoringParameters(newParams, p.getParameters()));
            p.setParameters(null);

            scoringService.invalidateCache();

            p.getCollaborators().forEach(collab -> recalculateCollaboratorPoints(collab, p));
        } else if (updates.containsKey("parameters")) {
            NestedScoringParameters legacyParams = objectMapper.convertValue(
                    updates.get("parameters"), NestedScoringParameters.class);

            p.setScoringParameters(ScoringParametersByPeriod.builder()
                    .diurno(copyParameters(legacyParams))
                    .noturno(copyParameters(legacyParams))
                    .h24(copyParameters(legacyParams))
                    .build());
            p.setParameters(null);

            scoringService.invalidateCache();

            p.getCollaborators().forEach(collab -> recalculateCollaboratorPoints(collab, p));
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
                collaborator.getDurationSeconds(),
                collaborator.getCriticos(),
                collaborator.getRemovidos(),
                collaborator.getRemovidosLider(),
                collaborator.getPausaMensalSeconds(),
                collaborator.getSaidaVtrSeconds(),
                projeto.parametersFor(collaborator.getWorkPeriod())
        );
        collaborator.setPoints(pontos);
        collaborator.setPontuacao(pontos.getOrDefault("Total", 0));
    }

    public ProjetoEntity createProjetoWithCollaborators(ProjetoEntity projeto) {
        projeto.setScoringParameters(parametersForNextProject(projeto));
        projeto.setParameters(null);
        projeto.setCreatedAt(Instant.now());
        projeto.setUpdatedAt(projeto.getCreatedAt());
        ProjetoEntity novo = projetoRepo.save(projeto);
        List<CollaboratorEntity> globais = collaboratorRepo.findAll();
        List<MedicoEntity> medicos = medicoRepository.findAll();

        var collabs = globais.stream().map(g -> {
            if (!Objects.equals(g.getRole(), "MEDICO")) {
                return ProjectCollaborator.builder()
                        .nome(g.getNome())
                        .collaboratorId(g.getId())
                        .role(g.getRole())
                        .idCallRote(g.getIdCallRote())
                        .workPeriod(WorkPeriod.resolve(g.getRole(), null, null, g.getWorkPeriod()))
                        // Supervisor entra no projeto novo já com a equipe padrão do cadastro
                        // global; pode ser ajustada depois só neste projeto.
                        .equipeIds(g.getEquipeIds() != null ? new java.util.ArrayList<>(g.getEquipeIds()) : new java.util.ArrayList<>())
                        .build();
            }
            return medicos.stream()
                    .filter(m -> m.getId().equals(g.getId()))
                    .map(m -> ProjectCollaborator.builder()
                            .nome(m.getNome())
                            .collaboratorId(m.getId())
                            .role(g.getRole())
                            .idCallRote(m.getIdCallRote())
                            .medicoRole(m.getMedicoRole())
                            .shiftHours(m.getShiftHours())
                            .workPeriod(WorkPeriod.resolve(g.getRole(), m.getMedicoRole(), m.getShiftHours(), m.getWorkPeriod()))
                            .build())
                    .findFirst()
                    .orElse(null);
        }).toList();

        novo.setCollaborators(collabs);
        return projetoRepo.save(novo);
    }

    public List<ProjetoEntity> getAllProjeto() {
        List<ProjetoEntity> projects = projetoRepo.findAll();
        boolean migrated = false;
        for (ProjetoEntity project : projects) {
            if (project.getScoringParameters() == null) {
                project.setScoringParameters(normalizeScoringParameters(null, project.getParameters()));
                project.setParameters(null);
                migrated = true;
            }
            if (project.getCollaborators() != null) {
                for (ProjectCollaborator collaborator : project.getCollaborators()) {
                    WorkPeriod resolved = WorkPeriod.resolve(
                            collaborator.getRole(),
                            collaborator.getMedicoRole(),
                            collaborator.getShiftHours(),
                            collaborator.getWorkPeriod()
                    );
                    if (resolved != collaborator.getWorkPeriod()) {
                        collaborator.setWorkPeriod(resolved);
                        migrated = true;
                        // Recalcula a pontuação com os parâmetros corretos pro período corrigido,
                        // exceto se a pontuação desse colaborador já foi editada manualmente.
                        if (!Boolean.TRUE.equals(collaborator.getWasEdited())) {
                            recalculateCollaboratorPoints(collaborator, project);
                        }
                    }
                }
            }
        }
        if (migrated) {
            projetoRepo.saveAll(projects);
        }
        return projects;
    }

    private ScoringParametersByPeriod parametersForNextProject(ProjetoEntity newProject) {
        return projetoRepo.findAll().stream()
                .max(Comparator.comparing(
                        project -> project.getUpdatedAt() != null
                                ? project.getUpdatedAt()
                                : project.getCreatedAt() != null ? project.getCreatedAt() : Instant.EPOCH
                ))
                .map(project -> normalizeScoringParameters(project.getScoringParameters(), project.getParameters()))
                .map(this::copyScoringParameters)
                .orElseGet(() -> normalizeScoringParameters(
                        newProject.getScoringParameters(), newProject.getParameters()));
    }

    private ScoringParametersByPeriod normalizeScoringParameters(
            ScoringParametersByPeriod current,
            NestedScoringParameters legacy
    ) {
        NestedScoringParameters fallback = legacy != null ? legacy : new NestedScoringParameters();
        if (current == null) {
            return ScoringParametersByPeriod.builder()
                    .diurno(copyParameters(fallback))
                    .noturno(copyParameters(fallback))
                    .h24(copyParameters(fallback))
                    .build();
        }
        if (current.getDiurno() == null) current.setDiurno(copyParameters(fallback));
        if (current.getNoturno() == null) current.setNoturno(copyParameters(current.getDiurno()));
        if (current.getH24() == null) current.setH24(copyParameters(current.getDiurno()));
        return current;
    }

    private ScoringParametersByPeriod copyScoringParameters(ScoringParametersByPeriod source) {
        return ScoringParametersByPeriod.builder()
                .diurno(copyParameters(source.getDiurno()))
                .noturno(copyParameters(source.getNoturno()))
                .h24(copyParameters(source.getH24()))
                .build();
    }

    private NestedScoringParameters copyParameters(NestedScoringParameters source) {
        return objectMapper.convertValue(
                source != null ? source : new NestedScoringParameters(),
                NestedScoringParameters.class
        );
    }

    public void deleteProject(String projectId) {
        projetoRepo.deleteById(projectId);
    }
}
