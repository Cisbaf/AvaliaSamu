package com.avaliadados.model;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@ToString
@Builder
@Table(name = "frota")
@PrimaryKeyJoinColumn(name = "colaborador_id")
@DiscriminatorValue("FROTA")
public class FrotaEntity extends CollaboratorEntity {

    public FrotaEntity(
            String nome,
            String cpf,
            String idCallRote,
            int pontuacao,
            String role,
            Long version
    ) {
        super(nome, cpf, idCallRote, pontuacao, role, version);
    }
}
