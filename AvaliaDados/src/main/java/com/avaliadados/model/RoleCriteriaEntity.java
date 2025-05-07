package com.avaliadados.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "role_criteria")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleCriteriaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /** Nome exato do role, ex: "MEDICO_REGULADOR_12H", "FROTA", "TARM" */
    private String role;

    /** "DURATION" ou "QUANTITY" */
    private String criterionType;

    /** thresholds e scores */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "criteria_id")
    private List<CriterionValue> values;
}
