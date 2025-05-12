package com.avaliadados.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "colaborador")
@ToString
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role")
public class CollaboratorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String nome;
    private String cpf;
    private String idCallRote;
    private int pontuacao;
    @Column(insertable=false, updatable=false)
    private String role;
    @Version
    private Long version;
    @ElementCollection
    @CollectionTable(name = "collaborator_parametros", joinColumns = @JoinColumn(name = "collaborator_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, Integer> parametros;


    public CollaboratorEntity(String nome, String cpf, String idCallRote, int pontuacao, String role, Long version) {
        this.nome = nome;
        this.cpf = cpf;
        this.idCallRote = idCallRote;
        this.pontuacao = pontuacao;
        this.role = role;
        this.version = version;
    }
}
