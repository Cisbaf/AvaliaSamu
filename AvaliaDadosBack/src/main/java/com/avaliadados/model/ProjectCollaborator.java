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
    @Builder.Default
    private Long durationSeconds = 0L;
    @Builder.Default
    private Long criticos = 0L;
    @Builder.Default
    private Integer removidos = 0;
    @Builder.Default
    private Integer removidosLider = 0;
    @Builder.Default
    private Integer pontuacao = 0;
    @Builder.Default
    private Long pausaMensalSeconds = 0L;
    @Builder.Default
    private Long saidaVtrSeconds = 0L;
    private String idCallRote;
    Map<String, Integer> points;
    private NestedScoringParameters parametros;
    @Builder.Default
    private Boolean wasEdited = false;

}
