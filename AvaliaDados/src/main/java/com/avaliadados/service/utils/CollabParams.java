package com.avaliadados.service.utils;

import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.service.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollabParams {
    private final ScoringService scoringService;


    public int setParams(ProjectCollaborator pc, ProjetoEntity project, long duration, long criticos, int quantity, long pausaMensal, long saidaVtr) {
        if (pc.getRole() == null) return 0;

        NestedScoringParameters params = Optional.ofNullable(pc.getParametros())
                .orElseGet(() -> {
                    pc.setParametros(new NestedScoringParameters());
                    return pc.getParametros();
                });
        assert params != null;
        var section = params.getColab();

        switch (pc.getRole()) {
            case "TARM" -> section = params.getTarm();
            case "FROTA" -> section = params.getFrota();
            case "MEDICO" -> section = params.getMedico();
            default -> log.warn("Role não informada: {}", pc.getRole());
        }

        section.setPausas(List.of(ScoringRule.builder().duration(pausaMensal).build()));
        section.setRegulacao(List.of(ScoringRule.builder().duration(duration).build()));
        section.setRegulacaoLider(List.of(ScoringRule.builder().duration(criticos).build()));
        section.setRemovidos((List.of(ScoringRule.builder().quantity(quantity).build())));

        var pausas = section.getPausas().getLast().getDuration();
        var regulacao = section.getRegulacao().getLast().getDuration();
        var removidos = section.getRemovidos().getLast().getQuantity();
        Long saida = 0L;
        var regulacaoLider = section.getRegulacaoLider().getLast().getDuration();

        if (pc.getMedicoRole() != null) {
            if (pc.getMedicoRole().equals(MedicoRole.LIDER)) {
                section.setRegulacaoLider((List.of(ScoringRule.builder().duration(criticos).build())));
                regulacaoLider = section.getRegulacaoLider().getLast().getDuration();
            }
        }
        if (pc.getRole().equals("FROTA")) {
            section.setSaidaVtr((List.of(ScoringRule.builder().duration(saidaVtr).build())));
             saida = section.getSaidaVtr().getLast().getDuration();
        }

        if (pc.getMedicoRole() == null){
            pc.setMedicoRole(MedicoRole.NENHUM);
            log.warn("MedicoRole não informada para colaborador {}, definindo como NENHUM", pc.getNome());
        }
        Map<String, Integer> pontos = scoringService.calculateCollaboratorScore(
                pc.getRole(),
                pc.getMedicoRole().name(),
                pc.getShiftHours() != null ? pc.getShiftHours().name() : "H12",
                regulacao,
                regulacaoLider,
                removidos,
                pausas,
                saida,
                project.getParameters()
        );
        pc.setPoints(pontos);

       return pontos.get("Total");

    }

}
