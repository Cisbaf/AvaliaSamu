package com.avaliadados.model.DTO;

import com.avaliadados.model.CollaboratorEntity;
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
    private Instant addedAt;
    private Instant updatedAt;
}