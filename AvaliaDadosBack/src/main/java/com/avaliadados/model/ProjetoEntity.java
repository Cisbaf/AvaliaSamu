package com.avaliadados.model;

import com.avaliadados.model.params.NestedScoringParameters;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjetoEntity {
    @Id
    private String id;
    private String name;
    private String month;

    private List<ProjectCollaborator> collaborators;

    private NestedScoringParameters parameters;

    private Instant createdAt;
    private Instant updatedAt;

}
