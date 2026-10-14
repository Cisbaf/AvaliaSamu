package com.avaliadados.config;

import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.enums.WorkPeriod;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.service.ProjetosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrecaoLiderRunnerTest {

    private JdbcTemplate jdbc;
    private ProjetoRepository repo;
    private ProjetosService projetosService;
    private CorrecaoLiderRunner runner;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        repo = mock(ProjetoRepository.class);
        projetosService = mock(ProjetosService.class);
        runner = new CorrecaoLiderRunner(jdbc, repo, projetosService);
    }

    private ProjectCollaborator colab(MedicoRole papel, WorkPeriod periodo, boolean duplo, boolean editado) {
        return ProjectCollaborator.builder()
                .nome(papel + "-" + periodo + "-" + duplo)
                .role("MEDICO")
                .medicoRole(papel)
                .workPeriod(periodo)
                .shiftHours(papel == MedicoRole.LIDER ? ShiftHours.H24 : ShiftHours.H12)
                .plantaoDuplo(duplo)
                .wasEdited(editado)
                .build();
    }

    private ProjetoEntity projeto(ProjectCollaborator... colaboradores) {
        ProjetoEntity p = new ProjetoEntity();
        p.setName("P");
        p.setCollaborators(new ArrayList<>(List.of(colaboradores)));
        return p;
    }

    @Test
    void corrigeOCadastroGlobalComTresAtualizacoes() {
        runner.corrigirCadastroGlobal();

        verify(jdbc, times(3)).update(anyString());
        verify(jdbc).update(contains("set work_period = 'H24'"));
        verify(jdbc).update(contains("set plantao_duplo = false"));
        verify(jdbc).update(contains("set shift_hours = 'H24'"));
    }

    @Test
    void liderComTurnoH12ViraH24SemRecalcularANota() {
        ProjectCollaborator lider = colab(MedicoRole.LIDER, WorkPeriod.H24, false, false);
        lider.setShiftHours(ShiftHours.H12);
        ProjetoEntity p = projeto(lider);
        when(repo.findAll()).thenReturn(List.of(p));

        runner.corrigirProjetos();

        assertEquals(ShiftHours.H24, lider.getShiftHours());
        verify(projetosService, never()).recalculateCollaboratorPoints(any(), any());
        verify(repo).saveAll(List.of(p));
    }

    @Test
    void liderComPeriodoErradoViraH24ERecalculaANota() {
        ProjectCollaborator lider = colab(MedicoRole.LIDER, WorkPeriod.NOTURNO, false, false);
        ProjetoEntity p = projeto(lider);
        when(repo.findAll()).thenReturn(List.of(p));

        runner.corrigirProjetos();

        assertEquals(WorkPeriod.H24, lider.periodoGravado());
        verify(projetosService).recalculateCollaboratorPoints(lider, p);
        verify(repo).saveAll(List.of(p));
    }

    @Test
    void liderComPlantaoDuploDesligaAFlagMasNaoRecalculaANota() {
        ProjectCollaborator lider = colab(MedicoRole.LIDER, WorkPeriod.DIURNO, true, false);
        ProjetoEntity p = projeto(lider);
        when(repo.findAll()).thenReturn(List.of(p));

        runner.corrigirProjetos();

        assertEquals(WorkPeriod.H24, lider.periodoGravado());
        assertFalse(lider.getPlantaoDuplo());
        verify(projetosService, never()).recalculateCollaboratorPoints(any(), any());
        verify(repo).saveAll(List.of(p));
    }

    @Test
    void notaEditadaAMaoNaoEhRecalculada() {
        ProjectCollaborator lider = colab(MedicoRole.LIDER, WorkPeriod.DIURNO, false, true);
        ProjetoEntity p = projeto(lider);
        when(repo.findAll()).thenReturn(List.of(p));

        runner.corrigirProjetos();

        assertEquals(WorkPeriod.H24, lider.periodoGravado());
        verify(projetosService, never()).recalculateCollaboratorPoints(any(), any());
    }

    @Test
    void reguladorNaoEhAlterado() {
        ProjectCollaborator regulador = colab(MedicoRole.REGULADOR, WorkPeriod.NOTURNO, true, false);
        ProjetoEntity p = projeto(regulador);
        when(repo.findAll()).thenReturn(List.of(p));

        runner.corrigirProjetos();

        assertEquals(WorkPeriod.NOTURNO, regulador.periodoGravado());
        assertEquals(true, regulador.getPlantaoDuplo());
        verify(repo, never()).saveAll(any());
    }

    @Test
    void jaCorretoNaoGravaNada() {
        ProjectCollaborator lider = colab(MedicoRole.LIDER, WorkPeriod.H24, false, false);
        ProjetoEntity p = projeto(lider);
        when(repo.findAll()).thenReturn(List.of(p));

        runner.corrigirProjetos();

        verify(repo, never()).saveAll(any());
        verify(projetosService, never()).recalculateCollaboratorPoints(any(), any());
    }

    @Test
    void erroNoBancoNaoDerrubaASubida() {
        when(jdbc.update(anyString())).thenThrow(new RuntimeException("sem tabela"));
        when(repo.findAll()).thenThrow(new RuntimeException("sem mongo"));

        runner.corrigirCadastroGlobal();
        runner.corrigirProjetos();
    }
}
