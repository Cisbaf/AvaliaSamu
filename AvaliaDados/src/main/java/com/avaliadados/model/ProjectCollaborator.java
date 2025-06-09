package com.avaliadados.model;

import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.params.NestedScoringParameters;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProjectCollaborator {
    private String collaboratorId;
    private String nome;
    private String role;
    private Integer plantao;
    private ShiftHours shiftHours;
    private MedicoRole medicoRole;
    private Long durationSeconds = 0L;
    private Long criticos = 0L;
    private Integer removidos = 0;
    private Integer pontuacao = 0;
    private Long pausaMensalSeconds = 0L;
    private Long saidaVtrSeconds = 0L;
    Map<String, Integer> points;
    private NestedScoringParameters parametros;
    private Boolean wasEdited = false;
    private boolean wasSearched = false;
    private boolean fristAdd = true;

}