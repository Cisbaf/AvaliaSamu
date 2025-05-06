package com.avaliadados.model;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
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
@PrimaryKeyJoinColumn(name = "colaborador_id")
@DiscriminatorValue("FROTA")
public class FrotaEntity extends CollaboratorEntity {

    private LocalTime regulacaoMedica;

    public FrotaEntity(String nome, String cpf, String idCallRote, int pontuacao, String role, LocalTime regulacaoMedica, Long version) {
        super(nome, cpf, idCallRote, pontuacao, role,version);
        this.regulacaoMedica = regulacaoMedica;
    }

}
