package com.avaliadados.controller;

import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.service.ProjectCollabService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
public class ProjectCollabController {
    private final ProjectCollabService service;

    @GetMapping("/{projectId}/collaborators")
    public ResponseEntity<List<CollaboratorsResponse>> getCollaborators(@PathVariable String projectId) {
        var all = service.getAllProjectCollaborators(projectId);
        return ResponseEntity.ok(all);
    }

    @PostMapping(
            path = "/{projectId}/collaborators",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjetoEntity> addCollaborator(
            @PathVariable String projectId,
            @RequestBody Map<String, String> body
    ) {
        String collaboratorId = body.get("collaboratorId");
        String role = body.get("role");
        return ResponseEntity.ok(service.addCollaborator(projectId, collaboratorId, role));
    }

    @PutMapping(
            path = "/{projectId}/collaborators/{collaboratorId}",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjetoEntity> updateCollaborator(
            @PathVariable String projectId,
            @PathVariable String collaboratorId,
            @RequestBody Map<String, String> body
    ) {
        String role = body.get("role");
        return ResponseEntity.ok(service.updateCollaboratorRole(projectId, collaboratorId, role));

    }

    @DeleteMapping("/{projectId}/collaborators/{collaboratorId}")
    public ResponseEntity<Void> removeCollaborator(
            @PathVariable String projectId,
            @PathVariable String collaboratorId
    ) {
        service.removeCollaborator(projectId, collaboratorId);
        return ResponseEntity.noContent().build();

    }
}
