package com.avaliadados.model.DTO;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Document
public class ProjectCollaborator {
    @Id
    private String collaboratorId;
    private String nome;
    private String role;
    private Long durationSeconds;
    private Integer quantity;
    private Integer pontuacao;
    private Map<String, Integer> parametros;
    private Long pausaMensalSeconds;


}