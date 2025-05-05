package com.avaliadados.controller;

import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.service.ProjectCollabService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
public class ProjectCollabController {
    private final ProjectCollabService service;

    @GetMapping("/{projectId}/collaborators")
    public ResponseEntity<List<CollaboratorsResponse>> getCollaborators(@PathVariable String projectId) {
        return ResponseEntity.ok(service.getAllProjectCollaborators(projectId));
    }

    @PostMapping("/{projectId}/collaborators")
    public ResponseEntity<ProjetoEntity> addCollaborator(
            @PathVariable String projectId,
            @RequestParam String collaboratorId,
            @RequestParam String role
    ) {
        return ResponseEntity.ok(service.addCollaborator(projectId, collaboratorId, role));
    }

    @PutMapping("/{projectId}/collaborators/{collaboratorId}")
    public ResponseEntity<ProjetoEntity> updateCollaborator(
            @PathVariable String projectId,
            @PathVariable String collaboratorId,
            @RequestParam String role
    ) {
        return ResponseEntity.ok(service.updateCollaboratorRole(projectId, collaboratorId, role));

    }

    @DeleteMapping("/{projectId}/collaborators/{collaboratorId}")
    public ResponseEntity<ProjetoEntity> removeCollaborator(
            @PathVariable String projectId,
            @PathVariable String collaboratorId
    ) {
        return ResponseEntity.ok(service.removeCollaborator(projectId, collaboratorId));

    }
}
