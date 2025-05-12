package com.avaliadados.service;

import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Slf4j
@Service
public class ScoringService {

    public int calculateCollaboratorScore(
            String role,
            Long durationSeconds,
            Integer quantity,
            Long pausaMensalSeconds,
            NestedScoringParameters params
    ) {
        log.info("Parametros: {}", params);
        if (params == null) {
            log.error("calculateCollaboratorScore chamado com NestedScoringParameters nulos! Role: {}, Duration: {}, Quantity: {}, Pausa: {}",
                    role, durationSeconds, quantity, pausaMensalSeconds);

            return 0;
        }

        if (role == null) {
            log.error("calculateCollaboratorScore chamado com role nulo.");
            return 0; // Ou lançar exceção
        }
        String roleType = role.split("_")[0].toUpperCase();

        int total = 0;

        // Adicionar verificações para as seções internas também (params.getTarm(), etc.)
        switch (roleType) {
            case "TARM":
                if (params.getTarm() == null) { // Verifica se a seção TARM existe
                    log.warn("Seção TARM dos parâmetros é nula para role {}. Pulando cálculo TARM.", role);
                    break; // Não calcula pontos TARM
                }
                total += calculateTarmScore(
                        durationSeconds, quantity, pausaMensalSeconds, params.getTarm()
                );
                break;

            case "FROTA":
                if (params.getFrota() == null) { // Verifica se a seção FROTA existe
                    log.warn("Seção FROTA dos parâmetros é nula para role {}. Pulando cálculo FROTA.", role);
                    break; // Não calcula pontos FROTA
                }
                total += calculateFrotaScore(durationSeconds, params.getFrota());
                break;

            case "MEDICO":
                if (params.getMedico() == null) { // Verifica se a seção MEDICO existe
                    log.warn("Seção MEDICO dos parâmetros é nula para role {}. Pulando cálculo MEDICO.", role);
                    break; // Não calcula pontos MEDICO
                }
                total += calculateMedicoScore(role, durationSeconds, quantity, params.getMedico());
                break;
            default:
                log.warn("Role type '{}' não reconhecido para cálculo de pontuação.", roleType);
        }

        return total;
    }

    private int calculateTarmScore(Long duration, Integer quantity, Long pausa, ScoringSectionParams params) {
        if (params == null) {
            log.warn("calculateTarmScore chamado com ScoringSectionParams nulos.");
            return 0;
        }
        int score = 0;

        if (quantity != null && params.getRemovidos() != null) {
        }
        if (duration != null && params.getRegulacao() != null) {
            score += findMatchingRule(duration, params.getRegulacao());
        }
        if (pausa != null && params.getPausas() != null) {
            score += findMatchingRule(pausa, params.getPausas());
        }
        log.info("Tamr Score {}", score);
        return score;
    }

    private int calculateFrotaScore(Long duration, ScoringSectionParams params) {
        int score = 0;

        if (duration != null) {
            score += findMatchingRule(duration, params.getSaidaVtr());
            score += findMatchingRule(duration, params.getRegulacao());
        }


        return score;
    }

    private int calculateMedicoScore(String role, Long duration, Integer quantity, ScoringSectionParams params) {
        int score = 0;

        if (quantity != null) {
            score += findMatchingRule(quantity, params.getRemovidos());
        }

        if (duration != null) {
            if (role.contains("LIDER")) {
                score += findMatchingRule(duration, params.getRegulacaoLider());
            } else {
                score += findMatchingRule(duration, params.getRegulacao());
            }
        }

        return score;
    }

    private int findMatchingRule(Integer value, List<ScoringRule> rules) {
        if (value == null || rules == null || rules.isEmpty()) {
            return 0;
        }
        return rules.stream()
                .filter(rule -> rule != null && rule.getQuantity() != null && value <= rule.getQuantity())
                .findFirst()
                .map(ScoringRule::getPoints)
                .orElse(0);
    }


    private int findMatchingRule(Long seconds, List<ScoringRule> rules) {
        if (seconds == null || rules == null || rules.isEmpty()) {
            return 0;
        }
        return rules.stream()
                .filter(rule -> rule != null && rule.getDuration() != null && !rule.getDuration().isEmpty())
                .filter(rule -> {
                    try {
                        LocalTime time = LocalTime.parse(rule.getDuration());
                        long ruleSeconds = time.toSecondOfDay();
                        return seconds <= ruleSeconds;
                    } catch (DateTimeParseException e) {
                        log.error("Erro ao parsear duração da regra '{}' para a regra: {}", rule.getDuration(), rule, e);
                        return false;
                    }
                })
                .findFirst()
                .map(ScoringRule::getPoints)
                .orElse(0);
    }
}