package com.avaliadados.controller;

import com.avaliadados.service.factory.AvaliacaoServiceFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Avaliação", description = "Operações relacionadas à avaliação de planilhas")
public class AvaliacaoController {

    private final AvaliacaoServiceFactory factory;

    @PostMapping("/{projectId}/processar")
    @Operation(summary = "Processa uma planilha de avaliação para o projeto especificado")
    public ResponseEntity<String> processarPlanilha(@RequestParam MultipartFile arquivo, @PathVariable String projectId) throws IOException {
        var processor = factory.getProcessor(arquivo);
        processor.processarPlanilha(arquivo, projectId);
        return ResponseEntity.ok("Processamento concluído.");
    }

}
