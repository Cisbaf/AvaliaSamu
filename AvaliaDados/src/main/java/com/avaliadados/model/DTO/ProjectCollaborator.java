package com.avaliadados.model.DTO;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCollaborator {
    private String collaboratorId;
    private String role;
    private Integer points;

}