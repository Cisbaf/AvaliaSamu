package com.avaliadados.model.dto;

import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.io.Serializable;

/**
 * DTO for {@link CollaboratorRequest}
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CollaboratorsResponse implements Serializable {
    private String id;
    private String nome;
    private String cpf;
    private String idCallRote;
    private int pontuacao;
    private String role;
    @Enumerated(EnumType.STRING)
    private ShiftHours shiftHours;
    @Enumerated(EnumType.STRING)
    private MedicoRole medicoRole;

    private Long durationSeconds;
    private Long pausaMensalSeconds;
    private Long saidaVtr;
    private Integer quantity;

}