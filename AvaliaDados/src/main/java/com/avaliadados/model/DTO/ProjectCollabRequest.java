package com.avaliadados.model.DTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectCollabRequest {
    private String collaboratorId;
    private String role;
    private Long durationSeconds;   // ou null
    private Integer quantity;       // ou null
}