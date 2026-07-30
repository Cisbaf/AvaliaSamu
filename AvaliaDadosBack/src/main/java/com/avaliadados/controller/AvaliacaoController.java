package com.avaliadados.controller;

import com.avaliadados.service.factory.AvaliacaoServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Avaliação", description = "Operações relacionadas à avaliação de planilhas")
public class AvaliacaoController {

    private final AvaliacaoServiceFactory factory;

    @PostMapping("/{projectId}/processar")
    @Operation(summary = "Processa uma planilha de avaliação para o projeto especificado")
    public ResponseEntity<List<String>> processarPlanilha(@RequestParam MultipartFile arquivo, @PathVariable String projectId) {
        try {
            var processor = factory.getProcessor(arquivo);
            return ResponseEntity.ok(processor.processarPlanilha(arquivo, projectId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(List.of(ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.badRequest().body(List.of("Não foi possível ler o arquivo enviado."));
        }
    }

}
