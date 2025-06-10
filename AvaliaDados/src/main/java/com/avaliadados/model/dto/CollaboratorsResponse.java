package com.avaliadados.model.dto;

import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;
import java.util.Map;

/**
 * DTO for {@link CollaboratorRequest}
 */
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
    private Integer plantao;

    private Long durationSeconds;
    private Long pausaMensalSeconds;
    private Long saidaVtr;
    private Integer removidos;
    private Long criticos;
    private int pontuacao;

    Map<String, Integer> points;

}