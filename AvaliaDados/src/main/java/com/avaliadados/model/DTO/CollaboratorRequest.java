package com.avaliadados.model.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * DTO for {@link com.avaliadados.model.CollaboratorEntity}
 */
public record CollaboratorRequest(
        @NotBlank(message = "Campo nome é obrigatorio")
        String nome,
        @Pattern(message = "CPF fornecido não existe",
                regexp = "([0-9]{2}[.]?[0-9]{3}[.]?[0-9]{3}/?[0-9]{4}-?[0-9]{2})")
        @NotBlank(message = "Campo CPF é obrigatorio")
        String cpf,
        @NotBlank(message = "Campo ID Call Rote é obrigatorio")
        String idCallRote,
        int pontuacao,
        @NotBlank
        String role,
        LocalTime tempoRegulaco,
        LocalTime regulacaoMedica
)
        implements Serializable {
}