package com.avaliadados.model;

import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.params.NestedScoringParameters;
import lombok.*;

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
    private ShiftHours shiftHours;
    private MedicoRole medicoRole;
    private Long durationSeconds = 0L;
    private Integer quantity = 0;
    private Integer pontuacao = 0;
    private NestedScoringParameters parametros;
    private Long pausaMensalSeconds = 0L;
    private Long saidaVtrSeconds = 0L;
    private Boolean wasEdited = false;
}