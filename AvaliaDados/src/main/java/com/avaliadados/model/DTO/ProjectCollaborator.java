package com.avaliadados.model.DTO;

import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.params.NestedScoringParameters;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Document
public class ProjectCollaborator {
    @Id
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