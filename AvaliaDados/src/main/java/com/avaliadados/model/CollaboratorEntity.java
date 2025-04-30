package com.avaliadados.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "colaborador")
@ToString
@Inheritance(strategy = InheritanceType.JOINED)
public class CollaboratorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private String id;
    private String nome;
    private String cpf;
    private String idCallRote;
    private int pontuacao;
    private String role;


    public CollaboratorEntity(String nome, String cpf, String idCallRote, int pontuacao, String role) {
        this.nome = nome;
        this.cpf = cpf;
        this.idCallRote = idCallRote;
        this.pontuacao = pontuacao;
        this.role = role;
    }
}
