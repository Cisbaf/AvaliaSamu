package com.avaliadados.model.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NestedScoringParameters {
    @Builder.Default
    private ScoringSectionParams colab = new ScoringSectionParams();
    @Builder.Default
    private ScoringSectionParams tarm = new ScoringSectionParams();
    @Builder.Default
    private ScoringSectionParams frota = new ScoringSectionParams();
    @Builder.Default
    private ScoringSectionParams medico = new ScoringSectionParams();
}