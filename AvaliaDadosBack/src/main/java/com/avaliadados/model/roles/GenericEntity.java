package com.avaliadados.model.roles;


import com.avaliadados.model.CollaboratorEntity;
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
@Table(name = "generic")
@PrimaryKeyJoinColumn(name = "colaborador_id")
public class GenericEntity extends CollaboratorEntity {

    public GenericEntity(
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
