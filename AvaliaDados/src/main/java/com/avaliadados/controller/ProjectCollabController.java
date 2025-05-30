package com.avaliadados.controller;

import com.avaliadados.model.dto.CollaboratorsResponse;
import com.avaliadados.model.dto.ProjectCollabRequest;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.service.ProjectCollabService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projetos/{projectId}/collaborator")
@RequiredArgsConstructor
@Tag(name = "Collaboradores do Projeto", description = "Operações relacionadas aos colaboradores de um projeto")
public class ProjectCollabController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectCollabController.class);
    private final ProjectCollabService service;

    @PostMapping
    @Operation(summary = "Adiciona um colaborador ao projeto")
    public ResponseEntity<ProjetoEntity> add(
            @PathVariable String projectId, @RequestBody ProjectCollabRequest dto,
            @RequestParam(required = false) NestedScoringParameters parametros) {

        logger.info("Adicionando colaborador ao projeto ID: {}", projectId);
        logger.debug("Dados do colaborador recebidos: {}", dto);
        logger.debug("Parâmetros recebidos: {}", parametros);
        return ResponseEntity.ok(service.addCollaborator(projectId, dto));
    }

    @GetMapping
    @Operation(summary = "Busca todos os colaboradores do projeto")
    public ResponseEntity<List<CollaboratorsResponse>> getAll(@PathVariable String projectId) {

        List<CollaboratorsResponse> collaborators = service.getAllProjectCollaborators(projectId);
        logger.info("Buscando todos os colaboradores do projeto ID: {}", collaborators);
        return ResponseEntity.ok(collaborators);
    }

    @PutMapping("/{collaboratorId}")
    @Operation(summary = "Atualiza um colaborador do projeto")
    public ResponseEntity<ProjetoEntity> update(@PathVariable String projectId, @PathVariable String collaboratorId, @RequestBody ProjectCollabRequest dto, @RequestParam  Boolean wasEdited) {

        return ResponseEntity.ok(service.updateProjectCollaborator(
                        projectId,
                        collaboratorId,
                        dto,
                        wasEdited
                )
        );
    }

    @DeleteMapping("/{collaboratorId}")
    @Operation(summary = "Remove um colaborador do projeto")
    public ResponseEntity<Void> delete(
            @PathVariable String projectId,
            @PathVariable String collaboratorId
    ) {
        service.removeCollaborator(projectId, collaboratorId);
        return ResponseEntity.noContent().build();
    }
}