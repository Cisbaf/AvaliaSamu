package com.avaliadados.model.dto;

import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.enums.WorkPeriod;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class CollaboratorsResponse implements Serializable {
    private String id;
    private String nome;
    private String cpf;
    private String idCallRote;
    private String role;
    @Enumerated(EnumType.STRING)
    private ShiftHours shiftHours;
    @Enumerated(EnumType.STRING)
    private MedicoRole medicoRole;
    @Enumerated(EnumType.STRING)
    private WorkPeriod workPeriod;
    private Integer plantao;

    private Long durationSeconds;
    private Long pausaMensalSeconds;
    private Long saidaVtr;
    private Integer removidos;
    private Integer removidosLider;
    private Long criticos;
    private int pontuacao;

    Map<String, Integer> points;

    // Equipe do supervisor. No retorno do cadastro global (CollaboratorEntity) são IDs
    // globais de colaboradores; no retorno de dentro de um projeto são collaboratorId
    // de outros colaboradores daquele mesmo projeto.
    private java.util.List<String> equipeIds;

}
