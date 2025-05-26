package com.avaliadados.service;

import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
            Long saidaVtrSeconds,
            NestedScoringParameters params
    ) {
        log.info("Iniciando cálculo de score para role={}, medicRole={}, shiftHour={}, duration={}s, quantity={}, pausa={}s",
                role, medicRole, shiftHour, durationSeconds, quantity, pausaMensalSeconds);

        if (params == null || role == null) {
            log.error("Parâmetros nulos: role={}, params={}", role, params);
            return 0;
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

        boolean shouldApplyMultiplier = "H24".equals(shiftHour) && ("MEDICO".equals(role));

        int totalScore = 0;

        switch (role) {
            case "TARM":
                if (sectionParams != null) {
                    totalScore += calculateTarmScore(durationSeconds, quantity, sectionParams);
                } else {
                    log.warn("Parâmetros TARM não encontrados para cálculo específico.");
                }
                break;
            case "FROTA":
                if (sectionParams != null) {
                    totalScore += calculateFrotaScore(durationSeconds, pausaMensalSeconds, saidaVtrSeconds, sectionParams);
                } else {
                    log.warn("Parâmetros FROTA não encontrados para cálculo específico.");
                }
                break;
            case "MEDICO":
                if (sectionParams != null) {
                    totalScore += calculateMedicoScore(medicRole, durationSeconds, quantity, sectionParams, shouldApplyMultiplier);
                } else {
                    log.warn("Parâmetros MEDICO não encontrados para cálculo específico.");
                }
                break;
            default:
                log.warn("Role desconhecido ou não pontuável especificamente: {}", role);
                break;
        }

        if (colabParams != null) {
            totalScore += calculateColabPausasScore(pausaMensalSeconds, colabParams, shouldApplyMultiplier);
        } else {
            log.warn("Parâmetros COLAB (pausas) não encontrados, pontuação de pausa será 0.");
        }

        log.info("Score final para role {}: {} (shiftHour={})", role, totalScore, shiftHour);
        System.out.println("---------------------------------------------------------------------------");

        return totalScore;
    }

    private int calculateTarmScore(Long duration, Integer quantity, ScoringSectionParams params) {
        log.info("Calculando TARM: duration={}s, quantity={}", duration, quantity);
        int score = 0;
        if (quantity != null && quantity > 0 && params.getRemovidos() != null && !params.getRemovidos().isEmpty()) {
            score += matchQuantityRule(quantity, params.getRemovidos());
        }
        if (duration != null && duration > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
            score += matchDurationRule(duration, params.getRegulacao(), false);
        }
        log.info("TARM specific score: {}", score);
        return score;
    }

    private int calculateFrotaScore(Long durationRegulacao,Long pausa, Long durationSaidaVtr, ScoringSectionParams params) {
        log.info("Calculando FROTA: durationRegulacao={}s, pausas={} durationSaidaVtr={}s", durationRegulacao, pausa, durationSaidaVtr);
        int score = 0;
        if (durationRegulacao != null && durationRegulacao > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
            score += matchDurationRule(durationRegulacao, params.getRegulacao(), false);
        }
        if (durationSaidaVtr != null && durationSaidaVtr > 0 && params.getSaidaVtr() != null && !params.getSaidaVtr().isEmpty()) {
            score += matchDurationRule(durationSaidaVtr, params.getSaidaVtr(), false);
        }
        if (pausa != null && pausa > 0 && params.getPausas() != null && !params.getPausas().isEmpty()) {
            score += matchDurationRule(pausa, params.getPausas(), false);
        }
        log.info("FROTA specific score: {}", score);
        return score;
    }

    private int calculateColabPausasScore(Long pausaSeconds, ScoringSectionParams colabParams, boolean applyMultiplier) {
        log.info("Calculando Pausas (COLAB): pausa={}s, applyMultiplier={}", pausaSeconds, applyMultiplier);
        int score = 0;
        if (pausaSeconds != null && pausaSeconds > 0 && colabParams.getPausas() != null && !colabParams.getPausas().isEmpty()) {
            score += matchDurationRule(pausaSeconds, colabParams.getPausas(), applyMultiplier);
        }
        log.info("COLAB Pausas score: {}", score);
        return score;
    }

    private int calculateMedicoScore(String medicRole, Long duration, Integer quantity, ScoringSectionParams params, boolean applyMultiplier) {
        log.info("calculateMedicoScore: medicRole=\"{}\", duration={}s, quantity={}, applyMultiplier={}",
                medicRole, duration, quantity, applyMultiplier);
        int score = 0;
        if (quantity != null && quantity > 0 && params.getRemovidos() != null && !params.getRemovidos().isEmpty()) {
            score += matchQuantityRule(quantity, params.getRemovidos());
        }
        List<ScoringRule> durationRules = null;
        if ("LIDER".equals(medicRole)) {
            if (params.getRegulacaoLider() != null && !params.getRegulacaoLider().isEmpty()) {
                durationRules = params.getRegulacaoLider();
                log.info("Usando regras de Regulação LIDER para médico {}", medicRole);
            } else {
                log.warn("Regras de Regulação LIDER não encontradas para médico {}", medicRole);
            }
        } else {
            if (params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
                durationRules = params.getRegulacao();
                log.info("Usando regras de Regulação padrão para médico {}", medicRole);
            } else {
                log.warn("Regras de Regulação padrão não encontradas para médico {}", medicRole);
            }
        }
        if (duration != null && duration > 0 && durationRules != null) {
            score += matchDurationRule(duration, durationRules, applyMultiplier);
        }
        log.info("MEDICO specific score result: {}", score);
        return score;
    }

    private int matchQuantityRule(Integer value, List<ScoringRule> rules) {
        if (value == null || value <= 0 || rules == null || rules.isEmpty()) {
            log.debug("matchQuantityRule: valor zero, nulo ou regras vazias. Retornando 0 pontos.");
            return 0;
        }

        List<ScoringRule> applicableRules = rules.stream()
                .filter(r -> r.getQuantity() != null && r.getPoints() != null && value <= r.getQuantity())
                .toList();

        if (applicableRules.isEmpty()) {
            log.info("matchQuantityRule: Nenhuma regra aplicável encontrada para quantidade={}. Retornando 0 pontos.", value);
            return 0;
        }

        int maxPoints = applicableRules.stream()
                .mapToInt(ScoringRule::getPoints)
                .max()
                .orElse(0);

        log.info("matchQuantityRule: Maior pontuação encontrada para quantidade={} entre {} regras aplicáveis: {} pontos",
                value, applicableRules.size(), maxPoints);
        return maxPoints;
    }

    private int matchDurationRule(Long seconds, List<ScoringRule> rules, boolean applyMultiplier) {
        if (seconds == null || seconds <= 0 || rules == null || rules.isEmpty()) {
            log.debug("matchDurationRule: valor zero, nulo ou regras vazias. Retornando 0 pontos.");
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