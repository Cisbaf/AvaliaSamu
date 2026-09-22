package com.avaliadados.service;

import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringParametersByPeriod;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.MedicoRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectParameterInheritanceTest {

    @Test
    void nextProjectReceivesAnIndependentCopyOfLatestParameters() {
        ProjetoRepository projectRepository = mock(ProjetoRepository.class);
        CollaboratorRepository collaboratorRepository = mock(CollaboratorRepository.class);
        MedicoRepository medicoRepository = mock(MedicoRepository.class);

        NestedScoringParameters daytime = parametersWithTarmPoints(11);
        NestedScoringParameters nighttime = parametersWithTarmPoints(22);
        ProjetoEntity latest = ProjetoEntity.builder()
                .id("latest")
                .updatedAt(Instant.parse("2026-08-01T00:00:00Z"))
                .scoringParameters(ScoringParametersByPeriod.builder()
                        .diurno(daytime)
                        .noturno(nighttime)
                        .build())
                .build();

        when(projectRepository.findAll()).thenReturn(List.of(latest));
        when(projectRepository.save(any(ProjetoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(collaboratorRepository.findAll()).thenReturn(List.of());
        when(medicoRepository.findAll()).thenReturn(List.of());

        ProjetosService service = new ProjetosService(
                projectRepository,
                collaboratorRepository,
                medicoRepository,
                new ScoringService(),
                new ObjectMapper()
        );

        ProjetoEntity created = service.createProjetoWithCollaborators(
                ProjetoEntity.builder().name("Próximo").month("09-2026").build()
        );

        assertEquals(11, firstTarmPoint(created.getScoringParameters().getDiurno()));
        assertEquals(22, firstTarmPoint(created.getScoringParameters().getNoturno()));
        assertNotSame(latest.getScoringParameters(), created.getScoringParameters());
        assertNotSame(daytime, created.getScoringParameters().getDiurno());

        created.getScoringParameters().getDiurno().getTarm().getRegulacao().getFirst().setPoints(99);
        assertEquals(11, firstTarmPoint(latest.getScoringParameters().getDiurno()));
    }

    private NestedScoringParameters parametersWithTarmPoints(int points) {
        return NestedScoringParameters.builder()
                .tarm(ScoringSectionParams.builder()
                        .regulacao(List.of(ScoringRule.builder().duration(100L).points(points).build()))
                        .build())
                .build();
    }

    private int firstTarmPoint(NestedScoringParameters parameters) {
        return parameters.getTarm().getRegulacao().getFirst().getPoints();
    }
}
