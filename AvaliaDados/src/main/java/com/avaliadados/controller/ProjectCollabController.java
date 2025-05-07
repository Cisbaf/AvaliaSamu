// src/main/java/com/avaliadados/controller/ProjectCollabController.java
package com.avaliadados.controller;

import com.avaliadados.model.DTO.ProjectCollabRequest;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.service.ProjectCollabService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projetos/{projectId}/collaborators")
@RequiredArgsConstructor
public class ProjectCollabController {

    private final ProjectCollabService service;

    @PostMapping
    public ResponseEntity<ProjetoEntity> add(
            @PathVariable String projectId,
            @RequestBody ProjectCollabRequest dto
    ) {
        return ResponseEntity.ok(
                service.addCollaborator(
                        projectId,
                        dto.getCollaboratorId(),
                        dto.getRole(),
                        dto.getDurationSeconds(),
                        dto.getQuantity()
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<CollaboratorsResponse>> getAll(
            @PathVariable String projectId
    ) {
        return ResponseEntity.ok(service.getAllProjectCollaborators(projectId));
    }

    @PutMapping("/{collaboratorId}")
    public ResponseEntity<ProjetoEntity> update(
            @PathVariable String projectId,
            @PathVariable String collaboratorId,
            @RequestBody ProjectCollabRequest dto
    ) {
        return ResponseEntity.ok(
                service.updateCollaboratorRole(
                        projectId,
                        collaboratorId,
                        dto.getRole(),
                        dto.getDurationSeconds(),
                        dto.getQuantity()
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
