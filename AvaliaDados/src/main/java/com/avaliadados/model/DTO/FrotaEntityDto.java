package com.avaliadados.model.DTO;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * DTO for {@link com.avaliadados.model.FrotaEntity}
 */
@Data
public class FrotaEntityDto implements Serializable {
    private String nome;
    private String cpf;
    private String idCallRote;
    private int pontuacao;
    private String role;
    private LocalTime regulacaoMedica;
}