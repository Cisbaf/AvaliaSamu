package com.avaliadados.model;

import com.avaliadados.model.DTO.ProjectCollaborator;
import com.avaliadados.model.params.NestedScoringParameters;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.ArrayList;
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

    private List<String> processedSpreadsheetHashes = new ArrayList<>();

}
