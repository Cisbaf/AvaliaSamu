package com.avaliadados.model;

import com.avaliadados.model.DTO.ProjectCollaborator;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<String, Integer> parameters = new HashMap<>();

    private Instant createdAt;
    private Instant updatedAt;
}
