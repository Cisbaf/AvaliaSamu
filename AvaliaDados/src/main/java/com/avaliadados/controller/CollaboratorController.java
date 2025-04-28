package com.avaliadados.controller;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorRequest;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.service.CollaboratorsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/collaborator")
public class CollaboratorController {
    private final CollaboratorsService service;

    @PostMapping
    public ResponseEntity<Object> createCollaborator(@RequestBody CollaboratorRequest request) {
        return switch (request.role().toUpperCase()) {
            case "TARM" -> ResponseEntity.ok(service.createTarm(request));
            case "FROTA" -> ResponseEntity.ok(service.createFrota(request));
            default -> throw new IllegalArgumentException("Função inválida");
        };
    }
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateCollaborator(@RequestBody CollaboratorRequest request, @PathVariable Long id) {
        return ResponseEntity.ok(service.updateColaborador(request, id));
    }

    @GetMapping("/id/{id}")
    public CollaboratorsResponse findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findByid(id)).getBody();
    }

    @GetMapping
    public ResponseEntity<List<CollaboratorEntity>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/name/{nome}")
    public ResponseEntity<List<CollaboratorEntity>> findByName(@PathVariable String nome) {
        return ResponseEntity.ok(service.findByName(nome));
    }
}
