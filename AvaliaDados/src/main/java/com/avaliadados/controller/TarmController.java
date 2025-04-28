package com.avaliadados.controller;

import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.FrotaEntityDto;
import com.avaliadados.model.DTO.TarmEntityDto;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.TarmEntity;
import com.avaliadados.service.CollaboratorsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/collaborator")
public class TarmController {
    private final CollaboratorsService service;

    @PostMapping("/tarm/save")
    public TarmEntity saveTarm(@RequestBody TarmEntityDto dto) {
        return ResponseEntity.ok(service.createTarm(dto)).getBody();
    }

    @PostMapping("/frota/save")
    public FrotaEntity saveFrota(@RequestBody FrotaEntityDto dto) {
        return ResponseEntity.ok(service.createFrota(dto)).getBody();
    }

    @GetMapping("/{id}?{role}")
    public CollaboratorsResponse buscarPorId(@PathVariable Long id, @PathVariable String role) {
        return ResponseEntity.ok(service.findByid(id, role)).getBody();
    }
}
