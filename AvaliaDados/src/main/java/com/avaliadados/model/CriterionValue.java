package com.avaliadados.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "criterion_value")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CriterionValue {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private long threshold;

    private int score;
}