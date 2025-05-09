// src/main/java/com/avaliadados/controller/ProjectCollabController.java
package com.avaliadados.controller;

import com.avaliadados.model.DTO.ProjectCollabRequest;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.service.ProjectCollabService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projetos/{projectId}/collaborators")
@RequiredArgsConstructor
public class ProjectCollabController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectCollabController.class);
    private final ProjectCollabService service;

    @PostMapping
    public ResponseEntity<ProjetoEntity> add(
            @PathVariable String projectId,
            @RequestBody ProjectCollabRequest dto,
            @RequestParam(required = false) Map<String, Integer> parametros // Adicionando parâmetro para os thresholds
    ) {
        logger.info("Adicionando colaborador ao projeto ID: {}", projectId);
        logger.debug("Dados do colaborador recebidos: {}", dto);
        logger.debug("Parâmetros recebidos: {}", parametros);
        return ResponseEntity.ok(
                service.addCollaborator(
                        projectId,
                        dto.getCollaboratorId(),
                        dto.getRole(),
                        dto.getDurationSeconds(),
                        dto.getQuantity(),
                        dto.getPausaMensalSeconds(),
                        parametros != null ? parametros : Map.of() // Passando os parâmetros para o serviço
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<CollaboratorsResponse>> getAll(
            @PathVariable String projectId
    ) {
        List<CollaboratorsResponse> collaborators = service.getAllProjectCollaborators(projectId);
        logger.info("Buscando todos os colaboradores do projeto ID: {}", collaborators);
        return ResponseEntity.ok(collaborators);
    }

    @PutMapping("/{collaboratorId}")
    public ResponseEntity<ProjetoEntity> update(
            @PathVariable String projectId,
            @PathVariable String collaboratorId,
            @RequestBody ProjectCollabRequest dto,
            @RequestParam(required = false) Map<String, Integer> parametros
    ) {

        return ResponseEntity.ok(
                service.updatePjCollaborator(
                        projectId,
                        collaboratorId,
                        dto.getRole(),
                        dto.getDurationSeconds(),
                        dto.getQuantity(),
                        dto.getPausaMensalSeconds(),
                        parametros != null ? parametros : Map.of()
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