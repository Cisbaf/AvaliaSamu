package com.avaliadados.model.params;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoringRule {
    private Long duration;
    private Integer quantity;
    private Integer points;

    public ScoringRule(Integer orDefault, String s, Integer points) {
    }
}