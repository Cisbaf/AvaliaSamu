package com.avaliadados.model.DTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

public class ProjectCollaborator {
    private String collaboratorId;
    private String nome;
    private String role;
    private Integer points;

}