package com.avaliadados.controller;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.FrotaEntityDto;
import com.avaliadados.model.DTO.TarmEntityDto;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.TarmEntity;
import com.avaliadados.service.CollaboratorsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/collaborator")
public class ColaboratorController {
    private final CollaboratorsService service;

    @PostMapping("/tarm/save")
    public TarmEntity saveTarm(@RequestBody TarmEntityDto dto) {
        return ResponseEntity.ok(service.createTarm(dto)).getBody();
    }

    @PostMapping("/frota/save")
    public FrotaEntity saveFrota(@RequestBody FrotaEntityDto dto) {
        return ResponseEntity.ok(service.createFrota(dto)).getBody();
    }

    @GetMapping("/id/{id}")
    public CollaboratorsResponse findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findByid(id)).getBody();
    }

    @GetMapping
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<List<String>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/name/{nome}")
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    public ResponseEntity<List<CollaboratorEntity>> findByName(@PathVariable String nome) {
        return ResponseEntity.ok(service.findByName(nome));
    }
}
