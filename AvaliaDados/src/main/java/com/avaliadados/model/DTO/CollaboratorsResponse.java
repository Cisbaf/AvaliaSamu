package com.avaliadados.model.DTO;

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
}