package com.avaliadados.service;

import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ScoringService {

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
        log.info("Iniciando cálculo de score para role={}, medicRole={}, shiftHour={}, duration={}s, removidos={}, pausa={}s",
                role, medicRole, shiftHour, durationSeconds, removidos, pausaMensalSeconds);

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

        if (sectionParams == null && !"COLAB".equals(role)) {
            log.warn("Nenhuma configuração específica encontrada para roleType: {}", role);
        }
        if (colabParams == null) {
            log.warn("Nenhuma configuração de COLAB (pausas) encontrada.");
        }

        boolean shouldApplyMultiplier = "H24".equals(shiftHour) && "MEDICO".equals(role);

        int totalScore = 0;

        switch (role) {
            case "TARM":
                if (sectionParams != null) {
                    totalScore += calculateTarmScore(durationSeconds, removidos, sectionParams, points);
                }
                break;
            case "FROTA":
                if (sectionParams != null) {
                    totalScore += calculateFrotaScore(durationSeconds, pausaMensalSeconds, saidaVtrSeconds, sectionParams, points);
                }
                break;
            case "MEDICO":
                if (sectionParams != null) {
                    totalScore += calculateMedicoScore(medicRole, durationSeconds, criticos, removidos, sectionParams, shouldApplyMultiplier, points);
                }
                break;
        }

        if (colabParams != null) {
            totalScore += calculateColabPausasScore(pausaMensalSeconds, colabParams, shouldApplyMultiplier, points);
        }

        log.info("Score final para role {}: {} (shiftHour={})", role, totalScore, shiftHour);
        points.put("Total", totalScore);
        log.info("Pontos calculados: {}", points);

        return points;
    }

    private int calculateTarmScore(Long duration, Integer removidos, ScoringSectionParams params, Map<String, Integer> points) {
        int score = 0;
        int point;

        if (removidos != null && removidos > 0 && params.getRemovidos() != null) {
            point = matchremovidosRule(removidos, params.getRemovidos());
            score += point;
            points.put("Removidos", point);
        }

        if (duration != null && duration > 0 && params.getRegulacao() != null) {
            point = matchDurationRule(duration, params.getRegulacao(), false);
            score += point;
            points.put("Regulacao", point);
        }

        return score;
    }

    private int calculateFrotaScore(Long durationRegulacao, Long pausa, Long durationSaidaVtr, ScoringSectionParams params, Map<String, Integer> points) {
        int score = 0;
        int point;

        if (durationRegulacao != null && durationRegulacao > 0 && params.getRegulacao() != null) {
            point = matchDurationRule(durationRegulacao, params.getRegulacao(), false);
            score += point;
            points.put("Regulacao", point);
        }

        if (durationSaidaVtr != null && durationSaidaVtr > 0 && params.getSaidaVtr() != null) {
            point = matchDurationRule(durationSaidaVtr, params.getSaidaVtr(), false);
            score += point;
            points.put("SaidaVTR", point);
        }

        if (pausa != null && pausa > 0 && params.getPausas() != null) {
            point = matchDurationRule(pausa, params.getPausas(), false);
            score += point;
            points.put("Pausas", point);
        }

        return score;
    }

    private int calculateColabPausasScore(Long pausaSeconds, ScoringSectionParams colabParams, boolean applyMultiplier, Map<String, Integer> points) {
        int score = 0;

        if (pausaSeconds != null && pausaSeconds > 0 && colabParams.getPausas() != null) {
            int point = matchDurationRule(pausaSeconds, colabParams.getPausas(), applyMultiplier);
            score += point;
            points.put("Pausas", point);
        }

        return score;
    }

    private int calculateMedicoScore(String medicRole, Long duration, Long criticos, Integer removidos, ScoringSectionParams params, boolean applyMultiplier, Map<String, Integer> points) {
        int score = 0;

        if (removidos != null && removidos > 0 && params.getRemovidos() != null) {
            int point = matchremovidosRule(removidos, params.getRemovidos());
            score += point;
            points.put("Removidos", point);
        }

        switch (medicRole) {
            case "LIDER":
                if (criticos != null && criticos > 0 && params.getRegulacaoLider() != null) {
                    int point = matchDurationRule(criticos, params.getRegulacaoLider(), applyMultiplier);
                    score += point;
                    points.put("Criticos", point);
                }
                break;

            case "REGULADOR":
                if (duration != null && duration > 0 && params.getRegulacao() != null) {
                    int point = matchDurationRule(duration, params.getRegulacao(), applyMultiplier);
                    score += point;
                    points.put("Regulacao", point);
                }
                break;

            case "LIDER_REGULADOR":
                if (criticos != null && criticos > 0 && params.getRegulacaoLider() != null) {
                    int point = matchDurationRule(criticos, params.getRegulacaoLider(), applyMultiplier);
                    score += point;
                    points.put("Criticos", point);
                }
                if (duration != null && duration > 0 && params.getRegulacao() != null) {
                    int point = matchDurationRule(duration, params.getRegulacao(), applyMultiplier);
                    score += point;
                    points.put("Regulacao", point);
                }
                break;

            default:
                log.warn("Papel médico desconhecido: {}", medicRole);
        }

        return score;
    }

    private int matchremovidosRule(Integer value, List<ScoringRule> rules) {
        if (value == null || value <= 0 || rules == null || rules.isEmpty()) return 0;

        return rules.stream()
                .filter(r -> r.getQuantity() != null && r.getPoints() != null && value <= r.getQuantity())
                .mapToInt(ScoringRule::getPoints)
                .max()
                .orElse(0);
    }

    private int matchDurationRule(Long seconds, List<ScoringRule> rules, boolean applyMultiplier) {
        if (seconds == null || seconds <= 0 || rules == null || rules.isEmpty()) {
            log.info("matchDurationRule: Nenhuma regra aplicável encontrada para seconds={}. Retornando 0 pontos.", seconds);
            return 0;
        }

        log.debug("matchDurationRule: seconds={}s, applyMultiplier={}", seconds, applyMultiplier);

        List<ScoringRule> applicableRules = rules.stream()
                .filter(r -> r.getDuration() != null && r.getPoints() != null)
                .map(originalRule -> {
                    ScoringRule adjustedRule = new ScoringRule();
                    adjustedRule.setPoints(originalRule.getPoints());
                    long duration = originalRule.getDuration();
                    if (applyMultiplier) {
                        duration = duration / 2;
                        log.debug("Regra ajustada para H24: {}s -> {}s (Pontos: {})", originalRule.getDuration(), duration, adjustedRule.getPoints());
                    }
                    adjustedRule.setDuration(duration);
                    return adjustedRule;
                })
                .filter(adjustedRule -> seconds <= adjustedRule.getDuration())
                .toList();

        if (applicableRules.isEmpty()) {
            log.info("matchDurationRule: Nenhuma regra aplicável encontrada para seconds={}. Retornando 0 pontos.", seconds);
            return 0;
        }

        int maxPoints = applicableRules.stream()
                .mapToInt(ScoringRule::getPoints)
                .max()
                .orElse(0);

        log.info("matchDurationRule: Maior pontuação encontrada para seconds={} entre {} regras aplicáveis: {} pontos",
                seconds, applicableRules.size(), maxPoints);
        return maxPoints;
    }
}
