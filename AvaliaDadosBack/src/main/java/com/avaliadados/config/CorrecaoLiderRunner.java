package com.avaliadados.config;

import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
import com.avaliadados.model.enums.WorkPeriod;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.service.ProjetosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Correção de dados que roda toda vez que o sistema liga (dev e servidor).
 * Só mexe no que está errado, então rodar de novo não altera nada.
 *
 * Regra: médico LÍDER é sempre 24h (WorkPeriod.H24) e não tem plantão duplo.
 * Plantão duplo e Diurno/Noturno são só do REGULADOR.
 *
 *  - Cadastro global (Postgres): período H24 e plantão duplo desligado para todo Líder.
 *  - Projetos (Mongo): idem para o Líder de cada projeto. A pontuação é recalculada
 *    (parâmetros 24h) se o período estava errado e ela não foi editada à mão.
 *    Se o Líder estava com plantão duplo ligado, os valores dele podem ter sido
 *    divididos por 2 na importação: a nota NÃO é recalculada e o projeto precisa
 *    da planilha reimportada.
 *
 * Qualquer erro aqui é registrado no log e não impede o sistema de subir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorrecaoLiderRunner implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final ProjetoRepository projetoRepo;
    private final ProjetosService projetosService;

    @Override
    public void run(ApplicationArguments args) {
        corrigirCadastroGlobal();
        corrigirProjetos();
    }

    void corrigirCadastroGlobal() {
        try {
            int periodo = jdbc.update(
                    "update colaborador set work_period = 'H24' "
                            + "where id in (select id from medico_entity where medico_role = 'LIDER') "
                            + "and (work_period is null or work_period <> 'H24')");
            int flag = jdbc.update(
                    "update medico_entity set plantao_duplo = false "
                            + "where medico_role = 'LIDER' and plantao_duplo = true");
            int turno = jdbc.update(
                    "update medico_entity set shift_hours = 'H24' "
                            + "where medico_role = 'LIDER' and (shift_hours is null or shift_hours <> 'H24')");
            if (periodo > 0 || flag > 0 || turno > 0) {
                log.info("[CORREÇÃO LÍDER] Cadastro global: {} líder(es) passaram para período 24h, {} tiveram o plantão duplo desligado, {} passaram para turno H24.",
                        periodo, flag, turno);
            }
        } catch (Exception e) {
            log.error("[CORREÇÃO LÍDER] Falha ao corrigir o cadastro global (o sistema continua subindo).", e);
        }
    }

    void corrigirProjetos() {
        try {
            List<ProjetoEntity> alterados = new ArrayList<>();
            int colaboradores = 0;

            for (ProjetoEntity projeto : projetoRepo.findAll()) {
                if (projeto.getCollaborators() == null) continue;
                boolean mudou = false;

                for (ProjectCollaborator pc : projeto.getCollaborators()) {
                    if (!"MEDICO".equals(pc.getRole()) || pc.getMedicoRole() != MedicoRole.LIDER) continue;

                    boolean periodoErrado = pc.periodoGravado() != WorkPeriod.H24;
                    boolean flagLigada = Boolean.TRUE.equals(pc.getPlantaoDuplo());
                    boolean turnoErrado = pc.getShiftHours() != ShiftHours.H24;
                    if (!periodoErrado && !flagLigada && !turnoErrado) continue;

                    pc.setWorkPeriod(WorkPeriod.H24);
                    pc.setShiftHours(ShiftHours.H24);
                    pc.setPlantaoDuplo(false);
                    mudou = true;
                    colaboradores++;

                    if (flagLigada) {
                        log.warn("[CORREÇÃO LÍDER] Projeto '{}': {} estava com plantão duplo e pode ter valores divididos por 2. "
                                + "Reimporte a planilha desse projeto para recalcular.", projeto.getName(), pc.getNome());
                    } else if (periodoErrado && !Boolean.TRUE.equals(pc.getWasEdited())) {
                        try {
                            projetosService.recalculateCollaboratorPoints(pc, projeto);
                        } catch (Exception e) {
                            log.warn("[CORREÇÃO LÍDER] Não foi possível recalcular a nota de {} no projeto '{}'.",
                                    pc.getNome(), projeto.getName(), e);
                        }
                    }
                }

                if (mudou) alterados.add(projeto);
            }

            if (!alterados.isEmpty()) {
                projetoRepo.saveAll(alterados);
                log.info("[CORREÇÃO LÍDER] Projetos: {} líder(es) corrigidos em {} projeto(s).", colaboradores, alterados.size());
            }
        } catch (Exception e) {
            log.error("[CORREÇÃO LÍDER] Falha ao corrigir os projetos (o sistema continua subindo).", e);
        }
    }
}
