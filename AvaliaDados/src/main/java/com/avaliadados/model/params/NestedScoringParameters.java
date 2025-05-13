package com.avaliadados.model.params;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NestedScoringParameters {
    private ScoringSectionParams colab;
    private ScoringSectionParams tarm;
    private ScoringSectionParams frota;
    private ScoringSectionParams medico;
}