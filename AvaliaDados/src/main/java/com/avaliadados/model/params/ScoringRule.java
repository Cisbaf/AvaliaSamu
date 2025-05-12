package com.avaliadados.model.params;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoringRule {
    private String duration;     // formato HH:mm:ss
    private Integer quantity;
    private Integer points;

    public ScoringRule(Integer orDefault, String s, Integer points) {
    }
}