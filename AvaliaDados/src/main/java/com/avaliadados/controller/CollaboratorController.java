package com.avaliadados.controller;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.dto.CollaboratorRequest;
import com.avaliadados.model.dto.CollaboratorsResponse;
import com.avaliadados.service.CollaboratorsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collaborator")
@Tag(name = "Colaboradores", description = "Operações relacionadas aos colaboradores globais")
public class CollaboratorController {
    private final CollaboratorsService service;

    @PostMapping
    @Operation(summary = "Cria um novo colaborador global")
    public ResponseEntity<Object> createCollaborator(@RequestBody CollaboratorRequest request) {
        return  ResponseEntity.ok(service.createCollaborator(request));

    }
    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um colaborador global")
    public ResponseEntity<?> updateCollaborator(@RequestBody CollaboratorRequest request,@PathVariable String id) {
        try {
            return ResponseEntity.ok(service.updateCollaborator(request, id));
        } catch (OptimisticLockException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Erro de concorrência: O registro foi modificado por outra operação");
        } catch (NumberFormatException ex) {
            return ResponseEntity.badRequest()
                    .body("ETag inválido");
        }
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "Busca um colaborador global pelo ID")
    public ResponseEntity<CollaboratorsResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    @Operation(summary = "Busca todos os colaboradores globais")
    public ResponseEntity<List<CollaboratorEntity>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/name/{nome}")
    @Operation(summary = "Busca colaboradores globais pelo nome")
    public ResponseEntity<List<CollaboratorEntity>> findByName(@PathVariable String nome) {
        return ResponseEntity.ok(service.findByName(nome));
    }
    @DeleteMapping("{id}")
    @Operation(summary = "Deleta um colaborador global pelo ID")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
