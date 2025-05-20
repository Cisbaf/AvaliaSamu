package com.avaliadados.controller;

import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.ProjectCollabRequest;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.service.ProjectCollabService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projetos/{projectId}/collaborator")
@RequiredArgsConstructor
public class ProjectCollabController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectCollabController.class);
    private final ProjectCollabService service;

    @PostMapping
    public ResponseEntity<ProjetoEntity> add(
            @PathVariable String projectId, @RequestBody ProjectCollabRequest dto,
            @RequestParam(required = false) NestedScoringParameters parametros) {

        logger.info("Adicionando colaborador ao projeto ID: {}", projectId);
        logger.debug("Dados do colaborador recebidos: {}", dto);
        logger.debug("Parâmetros recebidos: {}", parametros);
        return ResponseEntity.ok(service.addCollaborator(projectId, dto));
    }

    @GetMapping
    public ResponseEntity<List<CollaboratorsResponse>> getAll(@PathVariable String projectId) {

        List<CollaboratorsResponse> collaborators = service.getAllProjectCollaborators(projectId);
        logger.info("Buscando todos os colaboradores do projeto ID: {}", collaborators);
        return ResponseEntity.ok(collaborators);
    }

    @PutMapping("/{collaboratorId}")
    public ResponseEntity<ProjetoEntity> update(@PathVariable String projectId, @PathVariable String collaboratorId, @RequestBody ProjectCollabRequest dto) {

        return ResponseEntity.ok(service.updateProjectCollaborator(
                        projectId,
                        collaboratorId,
                        dto
                )
        );
    }

    @DeleteMapping("/{collaboratorId}")
    public ResponseEntity<Void> delete(
            @PathVariable String projectId,
            @PathVariable String collaboratorId
    ) {
        service.removeCollaborator(projectId, collaboratorId);
        return ResponseEntity.noContent().build();
    }
}