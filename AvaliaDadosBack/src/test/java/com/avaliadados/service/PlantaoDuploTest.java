package com.avaliadados.service;

import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.WorkPeriod;
import com.avaliadados.service.utils.CollabParams;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlantaoDuploTest {

    private ProjectCollaborator medico(boolean plantaoDuplo) {
        return ProjectCollaborator.builder().role("MEDICO").medicoRole(MedicoRole.REGULADOR).plantaoDuplo(plantaoDuplo).build();
    }

    @Test
    void semPlantaoDuploNaoAlteraOsValores() {
        ProjectCollaborator pc = medico(false);
        assertEquals(200L, CollabParams.aplicarPlantaoDuplo(pc, 200L));
        assertEquals(7, CollabParams.aplicarPlantaoDuplo(pc, 7));
    }

    @Test
    void comPlantaoDuploDivideTemposPorDois() {
        ProjectCollaborator pc = medico(true);
        assertEquals(100L, CollabParams.aplicarPlantaoDuplo(pc, 200L));
        assertEquals(1800L, CollabParams.aplicarPlantaoDuplo(pc, 3600L));
    }

    @Test
    void comPlantaoDuploDivideQuantidadesPorDoisArredondandoParaCima() {
        ProjectCollaborator pc = medico(true);
        assertEquals(10, CollabParams.aplicarPlantaoDuplo(pc, 20));
        assertEquals(4, CollabParams.aplicarPlantaoDuplo(pc, 7));
        assertEquals(1, CollabParams.aplicarPlantaoDuplo(pc, 1));
        assertEquals(0, CollabParams.aplicarPlantaoDuplo(pc, 0));
    }

    // Total de pausa bruto: 36000s em 10 plantões => médico comum 3600s por plantão.
    @Test
    void pausaMedicoComumDivideTotalPeloNumeroDePlantoes() {
        ProjectCollaborator pc = medico(false);
        assertEquals(3600L, CollabParams.pausaPorPlantao(pc, WorkPeriod.DIURNO, 36000L, 10));
        assertEquals(3600L, CollabParams.pausaPorPlantao(pc, WorkPeriod.NOTURNO, 36000L, 10));
    }

    // Plantão duplo: total de pausas ÷ 2 e plantões ÷ 2 (diurno) / ÷ 4 (noturno). Nada extra no resultado.
    @Test
    void pausaPlantaoDuploDiurnoDividePlantoesPorDois() {
        ProjectCollaborator pc = medico(true);
        // total bruto 36000, 10 plantões: (36000/2) / (10/2) = 3600
        assertEquals(3600L, CollabParams.pausaPorPlantao(pc, WorkPeriod.DIURNO, 36000L, 10));
        // 7 plantões: 18000 / 3,5 = 5142 (sem truncar 7/2)
        assertEquals(5142L, CollabParams.pausaPorPlantao(pc, WorkPeriod.DIURNO, 36000L, 7));
    }

    @Test
    void pausaPlantaoDuploNoturnoDividePlantoesPorQuatro() {
        ProjectCollaborator pc = medico(true);
        // (36000/2) / (10/4) = 7200
        assertEquals(7200L, CollabParams.pausaPorPlantao(pc, WorkPeriod.NOTURNO, 36000L, 10));
        // Estefânia: 761 por plantão em 16 plantões => total 12176; (12176/2) / (16/4) = 1522
        assertEquals(1522L, CollabParams.pausaPorPlantao(pc, WorkPeriod.NOTURNO, 12176L, 16));
        assertEquals(0L, CollabParams.pausaPorPlantao(pc, WorkPeriod.NOTURNO, 36000L, 0));
    }

    @Test
    void liderNuncaEhAfetadoMesmoComAFlagMarcada() {
        ProjectCollaborator lider = ProjectCollaborator.builder()
                .role("MEDICO").medicoRole(MedicoRole.LIDER).plantaoDuplo(true).build();
        assertFalse(lider.plantaoDuploAtivo());
        assertEquals(200L, CollabParams.aplicarPlantaoDuplo(lider, 200L));
        assertEquals(7, CollabParams.aplicarPlantaoDuplo(lider, 7));
        assertEquals(3600L, CollabParams.pausaPorPlantao(lider, WorkPeriod.NOTURNO, 36000L, 10));
    }

    @Test
    void liderEhSempre24hSemDiurnoNoturno() {
        ProjectCollaborator lider = ProjectCollaborator.builder()
                .role("MEDICO").medicoRole(MedicoRole.LIDER).workPeriod(WorkPeriod.NOTURNO).build();
        assertEquals(WorkPeriod.H24, lider.getWorkPeriod());
        assertEquals(WorkPeriod.H24, WorkPeriod.resolve("MEDICO", MedicoRole.LIDER, null, WorkPeriod.NOTURNO));
        // Regulador continua só diurno/noturno
        assertEquals(WorkPeriod.DIURNO, WorkPeriod.resolve("MEDICO", MedicoRole.REGULADOR, null, WorkPeriod.H24));
        assertEquals(WorkPeriod.NOTURNO, WorkPeriod.resolve("MEDICO", MedicoRole.REGULADOR, null, WorkPeriod.NOTURNO));
    }

    @Test
    void flagSoValeParaMedicoEPadraoENaoMarcado() {
        assertFalse(ProjectCollaborator.builder().role("MEDICO").build().plantaoDuploAtivo());
        assertFalse(ProjectCollaborator.builder().role("TARM").plantaoDuplo(true).build().plantaoDuploAtivo());
        assertTrue(medico(true).plantaoDuploAtivo());
    }
}
