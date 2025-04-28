package com.avaliadados.model.DTO;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * DTO for {@link com.avaliadados.model.TarmEntity}
 */
@Data
public class TarmEntityDto implements Serializable {
    private String nome;
    private String cpf;
    private String idCallRote;
    private int pontuacao;
    private String role;
    private LocalTime tempoRegulaco;
}