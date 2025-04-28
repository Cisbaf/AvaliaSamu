package com.avaliadados.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@ToString
@Builder
@Table(name = "frota")
public class FrotaEntity extends CollaboratorEntity {

    private LocalTime regulacaoMedica;

    public FrotaEntity(String nome, String cpf, String idCallRote, int pontuacao, String role, LocalTime regulacaoMedica) {
        super(nome, cpf, idCallRote, pontuacao, role);
        this.regulacaoMedica = regulacaoMedica;
    }

}
