package com.avaliadados.model.dto;

import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCollabRequest {
    @NotBlank(message = "O ID do colaborador é obrigatório")
    private String collaboratorId;

    @NotBlank(message = "O nome é obrigatório")
    private String nome;

    @NotBlank(message = "O cargo é obrigatório")
    private String role;

    @NotNull(message = "A duração é obrigatória")
    @Positive(message = "A duração deve ser positiva")
    private Long durationSeconds;

    @NotNull(message = "A quantidade é obrigatória")
    private Integer quantity;

    @NotNull(message = "O tempo de críticos é obrigatório")
    private Long criticos;

    @NotNull(message = "A pontuação é obrigatória")
    private Integer pontuacao;

    @NotNull(message = "Os parâmetros são obrigatórios")
    private Map<String, Integer> parametros;

    @NotNull(message = "A pausa mensal é obrigatória")
    private Long pausaMensalSeconds;

    @NotNull(message = "O campo saída VTR é obrigatório")
    private Long saidaVtr;

    @NotNull(message = "O papel do médico é obrigatório")
    private MedicoRole medicoRole;

    @NotNull(message = "O turno é obrigatório")
    private ShiftHours shiftHours;
}