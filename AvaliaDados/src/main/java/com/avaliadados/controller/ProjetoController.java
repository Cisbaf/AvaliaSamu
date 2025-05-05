package com.avaliadados.controller;

import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.service.ProjetosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<ProjetoEntity> createProject(@RequestBody ProjetoEntity projeto) {

        return ResponseEntity.status(HttpStatus.CREATED).body(projetoService.createProjetoWithCollaborators(projeto));
    }

    @GetMapping
    public ResponseEntity<List<ProjetoEntity>> getAllProjects() {
        return ResponseEntity.ok(projetoService.getAllProjeto());

    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjetoEntity> updateProject(@PathVariable String id, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(projetoService.updateProjeto(id, updates));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable String id) {
        projetoService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}

