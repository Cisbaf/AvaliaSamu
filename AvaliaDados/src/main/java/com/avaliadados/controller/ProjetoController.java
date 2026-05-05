package com.avaliadados.controller;

import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.service.ProjetosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
@Tag(name = "Projetos", description = "Operações relacionadas a projetos")
public class ProjetoController {

    private final ProjetosService projetoService;

    @PostMapping
    @Operation(summary = "Cria um novo projeto com todos os colaboradores globais")
    public ResponseEntity<ProjetoEntity> createProject(@RequestBody ProjetoEntity projeto) {

        return ResponseEntity.status(HttpStatus.CREATED).body(projetoService.createProjetoWithCollaborators(projeto));
    }

    @GetMapping
    @Operation(summary = "Busca todos os projetos")
    public ResponseEntity<List<ProjetoEntity>> getAllProjects() {
        return ResponseEntity.ok(projetoService.getAllProjeto());

    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um projeto existente")
    public ResponseEntity<ProjetoEntity> updateProject(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(projetoService.updateProjeto(id, updates));
    }
    @DeleteMapping("/{id}")
    @Operation(summary = "Deleta um projeto pelo ID")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projetoService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}

