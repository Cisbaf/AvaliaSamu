package com.avaliadados.service;

import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.enums.WorkPeriod;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringParametersByPeriod;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoringByPeriodTest {

    private final ScoringService scoringService = new ScoringService();

    @Test
    void usesTheParametersForTheCollaboratorPeriod() {
        ProjetoEntity project = ProjetoEntity.builder()
                .scoringParameters(ScoringParametersByPeriod.builder()
                        .diurno(parametersWithTarmPoints(10))
                        .noturno(parametersWithTarmPoints(20))
                        .build())
                .build();

        int daytimeScore = score(project, WorkPeriod.DIURNO);
        int nighttimeScore = score(project, WorkPeriod.NOTURNO);

        assertEquals(10, daytimeScore);
        assertEquals(20, nighttimeScore);
    }

    @Test
    void defaultsLegacyCollaboratorsToDaytime() {
        ProjetoEntity project = ProjetoEntity.builder()
                .scoringParameters(ScoringParametersByPeriod.builder()
                        .diurno(parametersWithTarmPoints(7))
                        .noturno(parametersWithTarmPoints(30))
                        .build())
                .build();

        assertEquals(7, score(project, null));
    }

    private int score(ProjetoEntity project, WorkPeriod period) {
        return scoringService.calculateCollaboratorScore(
                "TARM", "NENHUM", 50L, 0L, 0, 0, 0L, 0L,
                project.parametersFor(period)
        ).get("Total");
    }

    private NestedScoringParameters parametersWithTarmPoints(int points) {
        return NestedScoringParameters.builder()
                .tarm(ScoringSectionParams.builder()
                        .regulacao(List.of(ScoringRule.builder().duration(100L).points(points).build()))
                        .build())
                .build();
    }
}
