package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.dto.CollaboratorsResponse;
import com.avaliadados.model.dto.ProjectCollabRequest;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.service.utils.CollabParams;
import com.avaliadados.service.utils.SheetProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCollabService {

    private final ProjetoRepository projetoRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final CollabParams collabParams;
    private final SheetProcessingService sheetProcessingService;

    @Transactional
    public ProjetoEntity addCollaborator(String projectId, ProjectCollabRequest dto) {
        log.info("Adicionando colaborador [{}] ao projeto [{}] com role [{}]",
                dto.getCollaboratorId(), projectId, dto.getRole());

        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        CollaboratorEntity collab = collaboratorRepo.findById(dto.getCollaboratorId())
                .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));

        // Se dto.getMedicoRole() vier nulo, define NENHUM
        var medicoRole = Optional.ofNullable(dto.getMedicoRole()).orElse(com.avaliadados.model.enums.MedicoRole.NENHUM);

        ProjectCollaborator pc = ProjectCollaborator.builder()
                .collaboratorId(dto.getCollaboratorId())
                .nome(collab.getNome())
                .role(dto.getRole())
                .durationSeconds(dto.getDurationSeconds())
                .quantity(dto.getQuantity())
                .pausaMensalSeconds(dto.getPausaMensalSeconds())
                .parametros(new com.avaliadados.model.params.NestedScoringParameters())
                .medicoRole(medicoRole)
                .shiftHours(dto.getShiftHours())
                .build();

        // DELEGA: tenta encontrar a SheetRow e, se existir, popula o pc
        sheetProcessingService
                .findAndAssociateSheetRow(dto.getCollaboratorId(), projectId, collab.getNome())
                .ifPresent(sheetRow -> {
                    sheetProcessingService.populateFromSheet(pc, sheetRow);

                    // Depois de popular o PC, calcula pontuação
                    long duration = Optional.ofNullable(pc.getDurationSeconds()).orElse(0L);
                    int quantity = Optional.ofNullable(pc.getQuantity()).orElse(0);
                    long pausaMensal = Optional.ofNullable(pc.getPausaMensalSeconds()).orElse(0L);
                    long saidaVtr = Optional.ofNullable(pc.getSaidaVtrSeconds()).orElse(0L);
                    long criticos = Optional.ofNullable(pc.getCriticos()).orElse(0L);

                    int pontos = collabParams.setParams(pc, projeto, duration, criticos, quantity, pausaMensal, saidaVtr);
                    pc.setPontuacao(pontos);
                    log.debug("Pontuação calculada para o colaborador: {}", pontos);
                });

        // Substitui qualquer colaborador já existente com esse ID, e adiciona o novo
        projeto.getCollaborators().removeIf(p -> p.getCollaboratorId().equals(dto.getCollaboratorId()));
        projeto.getCollaborators().add(pc);
        log.debug("Dados do Collaborador adicionados/atualizados: {}", pc);
        return projetoRepo.save(projeto);
    }

    public List<CollaboratorsResponse> getAllProjectCollaborators(String projectId) {
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        return projeto.getCollaborators().stream()
                .map(pc -> collaboratorRepo.findById(pc.getCollaboratorId())
                        .map(collab -> {
                            String cpf = collab.getCpf() != null ? collab.getCpf() : "000.000.000-00";
                            String idCallRote = collab.getIdCallRote() != null ? collab.getIdCallRote() : "000";
                            int pontuacao = Optional.ofNullable(pc.getPontuacao()).orElse(0);

                            return new CollaboratorsResponse(
                                    pc.getCollaboratorId(),
                                    pc.getNome(),
                                    cpf,
                                    idCallRote,
                                    pc.getRole(),
                                    pc.getShiftHours(),
                                    pc.getMedicoRole(),
                                    pc.getDurationSeconds(),
                                    pc.getPausaMensalSeconds(),
                                    pc.getSaidaVtrSeconds(),
                                    pc.getQuantity(),
                                    pc.getCriticos(),
                                    pontuacao,
                                    pc.getPoints()
                            );
                        })
                        .orElseGet(() -> new CollaboratorsResponse(
                                pc.getCollaboratorId(),
                                pc.getNome(),
                                "000.000.000-00",
                                "000",
                                pc.getRole(),
                                pc.getShiftHours(),
                                pc.getMedicoRole(),
                                pc.getDurationSeconds(),
                                pc.getPausaMensalSeconds(),
                                pc.getSaidaVtrSeconds(),
                                pc.getQuantity(),
                                pc.getCriticos(),
                                Optional.ofNullable(pc.getPontuacao()).orElse(0),
                                pc.getPoints()
                        ))
                )
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjetoEntity updateProjectCollaborator(
            String projectId,
            String collaboratorId,
            ProjectCollabRequest dto,
            boolean wasEdited
    ) {
        log.info("Atualizando colaborador [{}] no projeto [{}] (wasEdited = {})", dto.getCollaboratorId(), projectId, wasEdited);

        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        collaboratorRepo.findById(collaboratorId)
                .orElseThrow(() -> new RuntimeException("Colaborador não encontrado"));

        boolean existsNoProjeto = projeto.getCollaborators().stream()
                .anyMatch(c ->
                        !c.getCollaboratorId().equals(collaboratorId) &&
                                c.getNome().equals(dto.getNome()) &&
                                c.getMedicoRole().equals(dto.getMedicoRole())
                );

        if (existsNoProjeto) {
            log.warn("Colaborador com nome [{}] e médicoRole [{}] já existe no projeto [{}].",
                    dto.getNome(), dto.getMedicoRole(), projectId);
            throw new RuntimeException("Colaborador com mesmo nome e médicoRole já existe no projeto.");
        }

        projeto.getCollaborators().stream()
                .filter(pc -> pc.getCollaboratorId().equals(collaboratorId))
                .findFirst()
                .ifPresent(pc -> {
                    // Se ainda não foi editado manualmente, podemos reprojetar dados da planilha
                    if (!wasEdited && !pc.getWasEdited()) {
                        sheetProcessingService
                                .findAndAssociateSheetRow(collaboratorId, projectId, pc.getNome())
                                .ifPresent(sheetRow -> sheetProcessingService.populateFromSheet(pc, sheetRow));
                    }

                    // Atualiza somente campos passados no DTO
                    Optional.ofNullable(dto.getNome())
                            .ifPresent(pc::setNome);
                    Optional.ofNullable(dto.getRole())
                            .ifPresent(pc::setRole);
                    Optional.ofNullable(dto.getCriticos())
                            .ifPresent(pc::setCriticos);
                    pc.setMedicoRole(Optional.ofNullable(dto.getMedicoRole()).orElse(com.avaliadados.model.enums.MedicoRole.NENHUM));
                    Optional.ofNullable(dto.getShiftHours())
                            .ifPresent(pc::setShiftHours);

                    pc.setWasEdited(wasEdited || pc.getWasEdited());
                    Optional.ofNullable(dto.getSaidaVtr())
                            .ifPresent(pc::setSaidaVtrSeconds);
                    Optional.ofNullable(dto.getDurationSeconds())
                            .ifPresent(pc::setDurationSeconds);
                    Optional.ofNullable(dto.getQuantity())
                            .ifPresent(pc::setQuantity);
                    Optional.ofNullable(dto.getPausaMensalSeconds())
                            .ifPresent(pc::setPausaMensalSeconds);

                    log.info("Colaborador após atualizações de campos: {}", pc);

                    // Recalcula pontuação se houver durationSeconds
                    if (pc.getDurationSeconds() != null) {
                        long duration = pc.getDurationSeconds();
                        long criticosVal = Optional.ofNullable(pc.getCriticos()).orElse(0L);
                        int quantityVal = Optional.ofNullable(pc.getQuantity()).orElse(0);
                        long pausaMensal = Optional.ofNullable(pc.getPausaMensalSeconds()).orElse(0L);
                        long saidaVtr = Optional.ofNullable(pc.getSaidaVtrSeconds()).orElse(0L);

                        int pontos = collabParams.setParams(pc, projeto, duration, criticosVal, quantityVal, pausaMensal, saidaVtr);
                        pc.setPontuacao(pontos);
                    }

                    syncCollaboratorData(collaboratorId);
                });

        return projetoRepo.save(projeto);
    }

    @Transactional
    public void removeCollaborator(String projectId, String collaboratorId) {
        log.warn("Removendo colaborador [{}] do projeto [{}]", collaboratorId, projectId);
        ProjetoEntity projeto = projetoRepo.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        projeto.getCollaborators().removeIf(pc -> pc.getCollaboratorId().equals(collaboratorId));
        projetoRepo.save(projeto);
    }

    @Transactional
    public void syncCollaboratorData(String collaboratorId) {
        log.info("Sincronizando (apenas) IDs do colaborador [{}]", collaboratorId);
        List<ProjetoEntity> projetos = projetoRepo.findByCollaboratorsCollaboratorId(collaboratorId);
        projetoRepo.saveAll(projetos);
    }
}
