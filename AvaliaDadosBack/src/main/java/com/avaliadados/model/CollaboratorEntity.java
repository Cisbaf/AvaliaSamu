package com.avaliadados.model;

import com.avaliadados.model.enums.WorkPeriod;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String nome;
    private String cpf;
    private String idCallRote;
    private int pontuacao;
    private String role;
    @Enumerated(EnumType.STRING)
    private WorkPeriod workPeriod = WorkPeriod.DIURNO;
    @Version
    private Long version;


    public CollaboratorEntity(String nome, String cpf, String idCallRote, int pontuacao, String role, Long version) {
        this.nome = nome;
        this.cpf = cpf;
        this.idCallRote = idCallRote;
        this.pontuacao = pontuacao;
        this.role = role;
        this.version = version;
    }
}
