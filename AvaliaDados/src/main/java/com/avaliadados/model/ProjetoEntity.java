package com.avaliadados.model;

import com.avaliadados.model.DTO.ProjectCollaborator;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetoEntity {
    @Id
    private String id;
    private String name;
    private String month;

    private List<ProjectCollaborator> collaborators;

    private Instant createdAt;
    private Instant updatedAt;
}
