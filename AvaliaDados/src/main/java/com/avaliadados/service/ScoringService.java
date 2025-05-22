package com.avaliadados.service;

import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class ScoringService {

    public int calculateCollaboratorScore(
            String role,
            String medicRole,
            String shiftHour,
            Long durationSeconds,
            Integer quantity,
            Long pausaMensalSeconds,
            NestedScoringParameters params
    ) {
        log.info("Iniciando cálculo de score para role={}, shiftHour={}, duration={}s, quantity={}, pausa={}s",
                role, shiftHour, durationSeconds, quantity, pausaMensalSeconds);

        if (params == null || role == null) {
            log.error("Parâmetros nulos: role={}, params={}", role, params);
            return 0;
        }

        ScoringSectionParams sectionParams = switch (role) {
            case "TARM" -> params.getTarm();
            case "FROTA" -> params.getFrota();
            case "MEDICO" -> params.getMedico();
            case "COLAB" -> params.getColab();
            default -> null;
        };

        if (sectionParams == null) {
            log.warn("Nenhuma configuração encontrada para roleType: {}", role);
            return 0;
        }

        boolean shouldApplyMultiplier = "H24".equals(shiftHour) &&
                ("MEDICO".equals(role) || "COLAB".equals(role));

        int total = switch (role) {
            case "TARM" -> {
                var tarm = calculateTarmScore(durationSeconds, quantity, pausaMensalSeconds, sectionParams);
                var colab = calculateColb(pausaMensalSeconds, sectionParams, false);
                yield tarm + colab;
            }
            case "FROTA" -> {
                var frota = matchDurationRule(durationSeconds, sectionParams.getRegulacao(), false);
                var colab = calculateColb(pausaMensalSeconds, sectionParams, false);
                yield frota + colab;
            }
            case "MEDICO" -> {
                var medico = calculateMedicoScore(medicRole, durationSeconds, quantity, sectionParams, shouldApplyMultiplier);
                var colab = calculateColb(pausaMensalSeconds, sectionParams, shouldApplyMultiplier);
                yield medico + colab;
            }
            default -> 0;
        };

        log.info("Score final para role {}: {} (shiftHour={})", role, total, shiftHour);
        return total;
    }

    private int calculateTarmScore(Long duration, Integer quantity, Long pausa, ScoringSectionParams params) {
        log.info("Calculando TARM: duration={}s, quantity={}, pausa={}s", duration, quantity, pausa);
        int score = 0;

        if (quantity != null && quantity > 0 && params.getRemovidos() != null && !params.getRemovidos().isEmpty()) {
            score += matchQuantityRule(quantity, params.getRemovidos());
        }

        if (duration != null && duration > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
            score += matchDurationRule(duration, params.getRegulacao(), false);
        }

        if (pausa != null && pausa > 0 && params.getPausas() != null && !params.getPausas().isEmpty()) {
            score += matchDurationRule(pausa, params.getPausas(), false);
        }

        log.info("TARM partial score: {}", score);
        return score;
    }

    private int calculateColb(Long pausa, ScoringSectionParams params, boolean applyMultiplier) {
        log.info("Calculando Colab: pausa={}s, applyMultiplier={}", pausa, applyMultiplier);
        int score = 0;

        if (pausa != null && pausa > 0 && params.getPausas() != null && !params.getPausas().isEmpty()) {
            score += matchDurationRule(pausa, params.getPausas(), applyMultiplier);
        }

        return score;
    }

    private int calculateMedicoScore(String role, Long duration, Integer quantity, ScoringSectionParams params, boolean applyMultiplier) {
        log.info("calculateMedicoScore: role='{}', duration={}s, quantity={}, applyMultiplier={}",
                role, duration, quantity, applyMultiplier);

        int score = 0;

        if (quantity != null && quantity > 0 && params.getRemovidos() != null && !params.getRemovidos().isEmpty()) {
            int qtyPoints = matchQuantityRule(quantity, params.getRemovidos());
            score += qtyPoints;
        }

        if (duration != null && duration > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty() && "REGULADOR".equals(role)) {
            int durPoints = matchDurationRule(duration, params.getRegulacao(), applyMultiplier);
            score += durPoints;
        }else{
            int durPoints = matchDurationRule(duration, params.getRegulacaoLider(), applyMultiplier);
            score += durPoints;
        }

        log.info("calculateMedicoScore result: {}", score);
        return score;
    }

    private int matchQuantityRule(Integer value, List<ScoringRule> rules) {
        if (value == null || value == 0 || rules == null || rules.isEmpty()) {
            log.info("matchQuantityRule: valor zero ou nulo, retornando 0 pontos");
            return 0;
        }

        List<ScoringRule> validRules = rules.stream()
                .filter(r -> r.getQuantity() != null && r.getPoints() != null)
                .toList();

        if (validRules.isEmpty()) {
            return 0;
        }

        return validRules.stream()
                .filter(r -> value <= r.getQuantity())
                .findFirst()
                .map(ScoringRule::getPoints)
                .orElse(0);
    }

    private int matchDurationRule(Long seconds, List<ScoringRule> rules, boolean applyMultiplier) {
        if (seconds == null || seconds == 0 || rules == null || rules.isEmpty()) {
            log.info("matchDurationRule: valor zero ou nulo, retornando 0 pontos");
            return 0;
        }

        log.info("matchDurationRule: seconds={}s, applyMultiplier={}", seconds, applyMultiplier);

        List<ScoringRule> validRules = rules.stream()
                .filter(r -> r.getDuration() != null && r.getPoints() != null)
                .toList();

        if (validRules.isEmpty()) {
            return 0;
        }

        List<ScoringRule> adjustedRules = new ArrayList<>();

        for (ScoringRule originalRule : validRules) {
            ScoringRule adjustedRule = new ScoringRule();
            adjustedRule.setPoints(originalRule.getPoints());


            if (applyMultiplier) {
                long adjustedDuration = originalRule.getDuration() / 2;
                adjustedRule.setDuration(adjustedDuration);
                log.info("Regra ajustada para H24: {}s -> {}s", originalRule.getDuration(), adjustedDuration);
            } else {
                adjustedRule.setDuration(originalRule.getDuration());
            }

            adjustedRules.add(adjustedRule);
        }

        List<ScoringRule> sorted = adjustedRules.stream()
                .sorted(Comparator.comparingLong(ScoringRule::getDuration))
                .toList();

        for (ScoringRule rule : sorted) {
            if (seconds <= rule.getDuration()) {
                log.info("Regra encontrada: {}s <= {}s -> {} pontos",
                        seconds, rule.getDuration(), rule.getPoints());
                return rule.getPoints();
            }
        }

        log.info("Nenhuma regra satisfeita para seconds={}", seconds);
        return 0;
    }
}
