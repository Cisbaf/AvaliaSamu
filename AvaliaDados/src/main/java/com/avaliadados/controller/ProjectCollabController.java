package com.avaliadados.controller;

import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.service.ProjetosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
public class ProjectCollabController {
    private final ProjetosService projetoService;

    @GetMapping("/{projectId}/collaborators")
    public ResponseEntity<List<CollaboratorsResponse>> listarColaboradores(@PathVariable String projectId) {
        return ResponseEntity.ok(projetoService.getAllProjectCollaborators(projectId));
    }

    @PostMapping("/{projectId}/collaborators")
    public ResponseEntity<ProjetoEntity> addColaborador(
            @PathVariable String projectId,
            @RequestParam String collaboratorId,
            @RequestParam String role
    ) {
        return ResponseEntity.ok(projetoService.addCollaborator(projectId, collaboratorId, role));
    }

    @PutMapping("/{projectId}/collaborators/{collaboratorId}")
    public ResponseEntity<ProjetoEntity> updateColaborador(
            @PathVariable String projectId,
            @PathVariable String collaboratorId,
            @RequestParam String role
    ) {
        return ResponseEntity.ok(projetoService.updateCollaboratorRole(projectId, collaboratorId, role));

    }

    @DeleteMapping("/{projectId}/collaborators/{collaboratorId}")
    public ResponseEntity<ProjetoEntity> removeColaborador(
            @PathVariable String projectId,
            @PathVariable String collaboratorId
    ) {
        return ResponseEntity.ok(projetoService.removeCollaborator(projectId, collaboratorId));

    }
}
