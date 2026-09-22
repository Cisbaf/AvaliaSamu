package com.avaliadados.model;

import com.avaliadados.model.enums.WorkPeriod;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringParametersByPeriod;
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

    private ScoringParametersByPeriod scoringParameters;

    private Instant createdAt;
    private Instant updatedAt;

    public NestedScoringParameters parametersFor(WorkPeriod workPeriod) {
        if (scoringParameters != null) {
            NestedScoringParameters selected = scoringParameters.forPeriod(
                    workPeriod != null ? workPeriod : WorkPeriod.DIURNO
            );
            if (selected != null) {
                return selected;
            }
        }
        return parameters != null ? parameters : new NestedScoringParameters();
    }

}
