package com.avaliadados.model.DTO;

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
    private Long durationSeconds;
    private Integer quantity;
    private Integer pontuacao;
    private NestedScoringParameters parametros;
    private Long pausaMensalSeconds;
}