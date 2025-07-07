package com.avaliadados.controller;

import com.avaliadados.service.utils.CollabParams;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/colab-data")
@RequiredArgsConstructor
public class ColabDataController {
    private final CollabParams collabParams;

    @PostMapping("/{projectId}")
    public Map<String, String> getColabData(@PathVariable String projectId, @RequestBody List<String> idCallrout) {
        return null;
    }
}