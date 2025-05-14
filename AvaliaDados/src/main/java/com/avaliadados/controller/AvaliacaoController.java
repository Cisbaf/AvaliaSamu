package com.avaliadados.controller;

import com.avaliadados.service.AvaliacaoServiceMedico;
import com.avaliadados.service.factory.AvaliacaoServiceFactory;
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
public class AvaliacaoController {

    private final AvaliacaoServiceMedico service;
    private final AvaliacaoServiceFactory factory;

    @PostMapping("/{projectId}/processar")
    public ResponseEntity<String> processarPlanilha(@RequestParam MultipartFile arquivo, @PathVariable String projectId) throws IOException {
            var processor = factory.getProcessor(arquivo);
            processor.processarPlanilha(arquivo, projectId);
            return ResponseEntity.ok("Processamento concluído.");
    }

}
