package com.avaliadados.model;

import com.avaliadados.controller.JsonMapConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "colaborador")
@ToString
public class ColaboradorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String nome;
    private String cpf;
    private String idCallRote;
    private int pontuacao;
    private String role;

    @Column(columnDefinition = "json")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> parameters;

}
