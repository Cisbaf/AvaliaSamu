package com.avaliadados.model;

import com.avaliadados.model.enums.WorkPeriod;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    // Equipe padrão do supervisor: IDs (CollaboratorEntity.id) de TARM/FROTA/MEDICO
    // que formam o time dele. Serve de modelo ao criar um novo projeto (o time é
    // copiado para ProjectCollaborator.equipeIds e pode ser ajustado só naquele projeto).
    // Só tem sentido para role = SUPERVISOR, mas fica disponível a todos por simplicidade.
    // fetch = EAGER: GET /api/collaborator (findAll) serializa a entidade direto,
    // sem passar por DTO — lazy aqui poderia estourar LazyInitializationException
    // fora da transação dependendo da config de open-in-view.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "colaborador_equipe", joinColumns = @JoinColumn(name = "colaborador_id"))
    @Column(name = "membro_id")
    private List<String> equipeIds = new ArrayList<>();

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
