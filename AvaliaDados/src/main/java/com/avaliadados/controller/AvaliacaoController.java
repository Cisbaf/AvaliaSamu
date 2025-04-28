package com.avaliadados.controller;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.service.AvaliacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AvaliacaoController {

    private final AvaliacaoService service;

    @PostMapping("/processar")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<String> processarPlanilha(@RequestParam MultipartFile arquivo) {
        try {
            service.processarPlanilha(arquivo);
            return ResponseEntity.ok("Planilha processada com sucesso");
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Erro ao processar planilha: " + e.getMessage());
        }
    }

    @GetMapping
    @CrossOrigin(origins = "*", allowedHeaders = "*")

    public ResponseEntity<List<String>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{nome}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<List<CollaboratorEntity>> findByNome(@PathVariable String nome) {
        return ResponseEntity.ok(service.findByName(nome));
    }

}
