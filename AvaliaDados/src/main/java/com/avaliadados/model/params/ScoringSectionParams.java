package com.avaliadados.model.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoringSectionParams {
   @Builder.Default private List<ScoringRule> removidos = new ArrayList<>();
    @Builder.Default private List<ScoringRule> regulacao = new ArrayList<>();
    @Builder.Default private List<ScoringRule> pausas = new ArrayList<>();
    @Builder.Default private List<ScoringRule> saidaVtr = new ArrayList<>();
    @Builder.Default private List<ScoringRule> regulacaoLider = new ArrayList<>();
}