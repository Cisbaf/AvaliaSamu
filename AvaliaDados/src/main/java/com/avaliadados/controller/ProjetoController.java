package com.avaliadados.controller;

import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.service.ProjetosService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

    private final ProjetosService projetoService;

    public ProjetoController(ProjetosService projetoService) {
        this.projetoService = projetoService;
    }

    @PostMapping
    public ProjetoEntity criarProjeto(@RequestBody ProjetoEntity projeto) {
        return projetoService.createProjeto(projeto);
    }
    @GetMapping
    public List<ProjetoEntity> listarTodos() {
        return projetoService.getAllProjetos();
    }

    @GetMapping("/{projectId}/collaborators")
    public List<?> listarColaboradores(@PathVariable String projectId) {
        return projetoService.getProjeto(projectId)
                .map(ProjetoEntity::getCollaborators)
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
    }

    @PostMapping("/{projectId}/collaborators")
    public ProjetoEntity addColaborador(
            @PathVariable String projectId,
            @RequestParam Long collaboratorId,
            @RequestParam String role
    ) {
        return projetoService.addCollaborator(projectId, collaboratorId, role);
    }

    @PutMapping("/{projectId}/collaborators/{collaboratorId}")
    public ProjetoEntity updateColaborador(
            @PathVariable String projectId,
            @PathVariable Long collaboratorId,
            @RequestParam String role
    ) {
        return projetoService.updateCollaboratorRole(projectId, collaboratorId, role);
    }

    @DeleteMapping("/{projectId}/collaborators/{collaboratorId}")
    public ProjetoEntity removeColaborador(
            @PathVariable String projectId,
            @PathVariable Long collaboratorId
    ) {
        return projetoService.removeCollaborator(projectId, collaboratorId);
    }
}

