package com.avaliadados.service.utils;

import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class CollabParams {

    public static ScoringSectionParams getSection(String role, NestedScoringParameters params) {
        if (role == null) return null;

        return switch (role) {
            case "TARM" -> params.getTarm();
            case "FROTA" -> params.getFrota();
            case "MEDICO" -> params.getMedico();
            default -> {
                log.warn("Role desconhecida: {}", role);
                yield null;
            }
        };
    }

    public static String determinarTipoRegulacao(ProjectCollaborator pc) {
        if ("MEDICO".equals(pc.getRole())) {
            return pc.getMedicoRole() == MedicoRole.LIDER ? "regulacaoLider" : "regulacao";
        }
        return "regulacao";
    }

    public static long getLastDuration(ScoringSectionParams section, String tipo) {
        if (section == null) return 0L;

        List<ScoringRule> rules = switch (tipo) {
            case "regulacao" -> section.getRegulacao();
            case "regulacaoLider" -> section.getRegulacaoLider();
            case "pausas" -> section.getPausas();
            default -> Collections.emptyList();
        };

        return !rules.isEmpty() ? rules.getLast().getDuration() : 0L;
    }

    public static int getLastQuantity(ScoringSectionParams section, String tipo) {
        if (section == null) return 0;

        List<ScoringRule> rules = switch (tipo) {
            case "removidos" -> section.getRemovidos();
            default -> Collections.emptyList();
        };

        return !rules.isEmpty() ? rules.getLast().getQuantity() : 0;
    }
}
