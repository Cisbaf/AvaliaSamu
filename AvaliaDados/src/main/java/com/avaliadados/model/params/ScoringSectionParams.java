package com.avaliadados.model.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoringSectionParams {
    private List<ScoringRule> removidos;
    private List<ScoringRule> regulacao;
    private List<ScoringRule> pausas;
    private List<ScoringRule> saidaVtr;
    private List<ScoringRule> regulacaoLider;
}