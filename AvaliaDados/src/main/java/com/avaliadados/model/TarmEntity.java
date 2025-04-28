package com.avaliadados.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@ToString
@Builder
@Table(name = "tarm")
@PrimaryKeyJoinColumn(name = "colaborador_id")
public class TarmEntity extends CollaboratorEntity {

    private LocalTime tempoRegulaco;

    public TarmEntity(String nome, String cpf, String idCallRote, int pontuacao, String role, LocalTime tempoRegulaco) {
        super(nome, cpf, idCallRote, pontuacao, role);
        this.tempoRegulaco = tempoRegulaco;
    }
}
