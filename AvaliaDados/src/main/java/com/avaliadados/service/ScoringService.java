package com.avaliadados.service;

import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ScoringService {
    private final ConcurrentHashMap<String, Integer> ruleCache = new ConcurrentHashMap<>();

    public Map<String, Integer> calculateCollaboratorScore(
            String role,
            String medicRole,
            String shiftHour,
            Long durationSeconds,
            Long criticos,
            Integer removidos,
            Long pausaMensalSeconds,
            Long saidaVtrSeconds,
            NestedScoringParameters params
    ) {
        if (log.isDebugEnabled()) {
            log.debug("Iniciando cálculo de score para role={}, medicRole={}, shiftHour={}, duration={}s, removidos={}, pausa={}s",
                    role, medicRole, shiftHour, durationSeconds, removidos, pausaMensalSeconds);
        }

        Map<String, Integer> points = new HashMap<>();
        if (params == null || role == null) {
            log.error("Parâmetros nulos: role={}, params={}", role, params);
            return points;
        }

        ScoringSectionParams sectionParams = switch (role) {
            case "TARM" -> params.getTarm();
            case "FROTA" -> params.getFrota();
            case "MEDICO" -> params.getMedico();
            default -> null;
        };
        ScoringSectionParams colabParams = params.getColab();

        boolean applyMultiplier = "H24".equals(shiftHour) && "MEDICO".equals(role);
        int totalScore = 0;

        switch (role) {
            case "TARM" -> {
                if (sectionParams != null) {
                    totalScore += calculateTarmScore(durationSeconds, removidos, sectionParams, points);
                }
            }
            case "FROTA" -> {
                if (sectionParams != null) {
                    totalScore += calculateFrotaScore(durationSeconds, pausaMensalSeconds, saidaVtrSeconds, sectionParams, points);
                }
            }
            case "MEDICO" -> {
                if (sectionParams != null) {
                    totalScore += calculateMedicoScore(medicRole, durationSeconds, criticos, removidos, sectionParams, applyMultiplier, points);
                }
            }
        }

        if (colabParams != null) {
            totalScore += calculateColabPausasScore(pausaMensalSeconds, colabParams, applyMultiplier, points);
        }

        points.put("Total", totalScore);
        if (log.isDebugEnabled()) {
            log.debug("Score final para role {}: {} (shiftHour={})", role, totalScore, shiftHour);
            log.debug("Pontos calculados: {}", points);
        }

        return points;
    }

    private int calculateTarmScore(Long duration, Integer removidos, ScoringSectionParams params, Map<String, Integer> points) {
       int score =  calculateRemovidos(removidos, params, points);
        if (duration != null && duration > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
            int pt = matchDurationRule(duration, params.getRegulacao(), false);
            score += pt;
            points.put("Regulacao", pt);
        }
        return score;
    }



    private int calculateFrotaScore(Long durationRegulacao, Long pausa, Long durationSaidaVtr, ScoringSectionParams params, Map<String, Integer> points) {
        int score = 0;
        if (durationRegulacao != null && durationRegulacao > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
            int pt = matchDurationRule(durationRegulacao, params.getRegulacao(), false);
            score += pt;
            points.put("Regulacao", pt);
        }
        if (durationSaidaVtr != null && durationSaidaVtr > 0 && params.getSaidaVtr() != null && !params.getSaidaVtr().isEmpty()) {
            int pt = matchDurationRule(durationSaidaVtr, params.getSaidaVtr(), false);
            score += pt;
            points.put("SaidaVTR", pt);
        }
        if (pausa != null && pausa > 0 && params.getPausas() != null && !params.getPausas().isEmpty()) {
            int pt = matchDurationRule(pausa, params.getPausas(), false);
            score += pt;
            points.put("Pausas", pt);
        }
        return score;
    }

    private int calculateColabPausasScore(Long pausaSeconds, ScoringSectionParams colabParams, boolean applyMultiplier, Map<String, Integer> points) {
        if (pausaSeconds == null || pausaSeconds <= 0 || colabParams.getPausas() == null || colabParams.getPausas().isEmpty()) {
            return 0;
        }
        int pt = matchDurationRule(pausaSeconds, colabParams.getPausas(), applyMultiplier);
        points.put("Pausas", pt);
        return pt;
    }

    private int calculateMedicoScore(String medicRole, Long duration, Long criticos, Integer removidos, ScoringSectionParams params, boolean applyMultiplier, Map<String, Integer> points) {
        int score = calculateRemovidos(removidos, params, points);
        switch (medicRole) {
            case "LIDER" -> {
                if (criticos != null && criticos > 0 && params.getRegulacaoLider() != null && !params.getRegulacaoLider().isEmpty()) {
                    int pt = matchDurationRule(criticos, params.getRegulacaoLider(), applyMultiplier);
                    score += pt;
                    points.put("Criticos", pt);
                }
            }
            case "REGULADOR" -> {
                if (duration != null && duration > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
                    int pt = matchDurationRule(duration, params.getRegulacao(), applyMultiplier);
                    score += pt;
                    points.put("Regulacao", pt);
                }
            }
            case "LIDER_REGULADOR" -> {
                if (criticos != null && criticos > 0 && params.getRegulacaoLider() != null && !params.getRegulacaoLider().isEmpty()) {
                    int pt = matchDurationRule(criticos, params.getRegulacaoLider(), applyMultiplier);
                    score += pt;
                    points.put("Criticos", pt);
                }
                if (duration != null && duration > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
                    int pt = matchDurationRule(duration, params.getRegulacao(), applyMultiplier);
                    score += pt;
                    points.put("Regulacao", pt);
                }
            }
        }
        return score;
    }
    private int calculateRemovidos(Integer removidos, ScoringSectionParams params, Map<String, Integer> points) {
        int score = 0;
        if (removidos != null && removidos > 0 && params.getRemovidos() != null && !params.getRemovidos().isEmpty()) {
            int pt = matchRemovidosRule(removidos, params.getRemovidos());

            points.put("Removidos", pt);
            return score + pt;
        }
        return score;
    }

    private int matchRemovidosRule(Integer value, List<ScoringRule> rules) {
        if (value == null || value <= 0 || rules == null || rules.isEmpty()) return 0;
        String cacheKey = "removidos_" + value + "_" + rules.hashCode();
        return ruleCache.computeIfAbsent(cacheKey, k ->
                rules.stream()
                        .filter(r -> r.getQuantity() != null && r.getPoints() != null && value <= r.getQuantity())
                        .mapToInt(ScoringRule::getPoints)
                        .max()
                        .orElse(0)
        );
    }

    private int matchDurationRule(Long seconds, List<ScoringRule> rules, boolean applyMultiplier) {
        if (seconds == null || seconds <= 0 || rules == null || rules.isEmpty()) {
            return 0;
        }
        String cacheKey = "duration_" + seconds + "_" + rules.hashCode() + "_" + applyMultiplier;
        return ruleCache.computeIfAbsent(cacheKey, k -> {
            List<ScoringRule> applicableRules = rules.stream()
                    .filter(r -> r.getDuration() != null && r.getPoints() != null)
                    .map(originalRule -> {
                        ScoringRule adjustedRule = new ScoringRule();
                        adjustedRule.setPoints(originalRule.getPoints());
                        long duration = originalRule.getDuration();
                        if (applyMultiplier) {
                            // dobrar o limiar para plantão de 24h
                            duration = duration * 2;
                        }
                        adjustedRule.setDuration(duration);
                        return adjustedRule;
                    })
                    .filter(adjustedRule -> seconds <= adjustedRule.getDuration())
                    .toList();

            if (applicableRules.isEmpty()) {
                return 0;
            }
            return applicableRules.stream()
                    .mapToInt(ScoringRule::getPoints)
                    .max()
                    .orElse(0);
        });
    }
}
