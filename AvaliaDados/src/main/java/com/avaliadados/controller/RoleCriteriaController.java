package com.avaliadados.controller;

import com.avaliadados.model.RoleCriteriaEntity;
import com.avaliadados.model.CriterionValue;
import com.avaliadados.service.RoleCriteriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/criteria")
public class RoleCriteriaController {
    private final RoleCriteriaService service;

    public RoleCriteriaController(RoleCriteriaService service) {
        this.service = service;
    }

    /** GET /api/criteria/{role}/{type} */
    @GetMapping("/{role}/{type}")
    public ResponseEntity<RoleCriteriaEntity> get(
            @PathVariable String role,
            @PathVariable String type) {
        return ResponseEntity.ok(service.getCriteria(role, type));
    }

    /** PUT /api/criteria/{role}/{type} */
    @PutMapping("/{role}/{type}")
    public ResponseEntity<RoleCriteriaEntity> update(
            @PathVariable String role,
            @PathVariable String type,
            @RequestBody List<CriterionValue> values) {
        return ResponseEntity.ok(service.updateCriteria(role, type, values));
    }
}