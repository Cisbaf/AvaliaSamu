package com.avaliadados.model;


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
            @NotBlank(message = "Campo nome é obrigatorio") String nome,
            @Pattern(message = "CPF fornecido não existe",
                    regexp = "([0-9]{2}[.]?[0-9]{3}[.]?[0-9]{3}/?[0-9]{4}-?[0-9]{2})")
            @NotBlank(message = "Campo CPF é obrigatorio") String cpf,
            @NotBlank(message = "Campo ID Call Rote é obrigatorio") String idCallRote,
            int pontuacao,
            @NotBlank String role,
            Long version
    ) {
        super(nome, cpf, idCallRote, pontuacao, role, version);
    }
}
