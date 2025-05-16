package com.avaliadados.model.DTO;

import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCollabRequest {
    private String collaboratorId;
    private String nome;
    private String role;
    private Long durationSeconds;
    private Integer quantity;
    private Integer pontuacao;
    private Map<String, Integer> parametros;
    private Long pausaMensalSeconds;
    private MedicoRole medicoRole;
    private ShiftHours shiftHours;}