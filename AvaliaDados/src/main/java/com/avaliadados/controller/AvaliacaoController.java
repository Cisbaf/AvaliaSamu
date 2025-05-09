package com.avaliadados.controller;

import com.avaliadados.service.AvaliacaoService;
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

    private final AvaliacaoService service;

    @PostMapping("/{projectId}/processar")
    public ResponseEntity<String> processarPlanilha(@RequestParam MultipartFile arquivo, @PathVariable String projectId) {
        try {
            service.processarPlanilha(arquivo, projectId);
            return ResponseEntity.ok("Planilha processada com sucesso");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Erro ao processar planilha: " + e.getMessage());
        }
    }

}
