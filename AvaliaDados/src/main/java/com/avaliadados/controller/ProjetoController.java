package com.avaliadados.controller;

import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.service.ProjetosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
public class ProjetoController {

    private final ProjetosService projetoService;

    @PostMapping
    public ProjetoEntity criarProjeto(@RequestBody ProjetoEntity projeto) {
        return projetoService.createProjetoWithCollaborators(projeto);
    }
    @GetMapping
    public List<ProjetoEntity> listarTodos() {
        return projetoService.getAllProjeto();
    }

    @PutMapping("/{id}")
    public ProjetoEntity atualizarProjeto(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates
    ) {
        return projetoService.updateProjeto(id, updates);
    }

    @GetMapping("/{projectId}/collaborators")
    public ResponseEntity<List<CollaboratorsResponse>> listarColaboradores(@PathVariable String projectId) {
        return ResponseEntity.ok(projetoService.getAllProjectCollaborators(projectId));
    }

    @PostMapping("/{projectId}/collaborators")
    public ProjetoEntity addColaborador(
            @PathVariable String projectId,
            @RequestParam String collaboratorId,
            @RequestParam String role
    ) {
        System.out.println("Colab id;" + collaboratorId);
        return projetoService.addCollaborator(projectId, collaboratorId, role);
    }

    @PutMapping("/{projectId}/collaborators/{collaboratorId}")
    public ProjetoEntity updateColaborador(
            @PathVariable String projectId,
            @PathVariable String collaboratorId,
            @RequestParam String role
    ) {
        return projetoService.updateCollaboratorRole(projectId, collaboratorId, role);
    }

    @DeleteMapping("/{projectId}/collaborators/{collaboratorId}")
    public ProjetoEntity removeColaborador(
            @PathVariable String projectId,
            @PathVariable String collaboratorId
    ) {
        return projetoService.removeCollaborator(projectId, collaboratorId);
    }
}

