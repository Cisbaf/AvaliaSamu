package com.avaliadados.model.DTO;

import java.io.Serializable;

/**
 * DTO for {@link ColaboradorRequest}
 */
public record CollaboratorsResponse(
        String nome,
        String cpf,
        String idCallRote,
        int pontuacao,
        String role) implements Serializable {
}