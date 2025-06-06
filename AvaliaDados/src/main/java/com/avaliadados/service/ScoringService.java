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
        // Reduzir o nível de log para debug para evitar sobrecarga de I/O
        if (log.isDebugEnabled()) {
            log.debug("Iniciando cálculo de score para role={}, medicRole={}, shiftHour={}, duration={}s, removidos={}, pausa={}s",
                    role, medicRole, shiftHour, durationSeconds, removidos, pausaMensalSeconds);
        }

        Map<String, Integer> points = new HashMap<>();

        if (params == null || role == null) {
            log.error("Parâmetros nulos: role={}, params={}", role, params);
            return points;
        }

        // Obter parâmetros de seção de forma mais eficiente
        ScoringSectionParams sectionParams = null;
        switch (role) {
            case "TARM" -> sectionParams = params.getTarm();
            case "FROTA" -> sectionParams = params.getFrota();
            case "MEDICO" -> sectionParams = params.getMedico();
        }

        ScoringSectionParams colabParams = params.getColab();

        // Logs apenas em nível debug
        if (log.isDebugEnabled()) {
            if (sectionParams == null && !"COLAB".equals(role)) {
                log.debug("Nenhuma configuração específica encontrada para roleType: {}", role);
            }
            if (colabParams == null) {
                log.debug("Nenhuma configuração de COLAB (pausas) encontrada.");
            }
        }

        boolean shouldApplyMultiplier = "H24".equals(shiftHour) && "MEDICO".equals(role);

        int totalScore = 0;

        // Calcular pontuação com base no papel
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
                    totalScore += calculateMedicoScore(medicRole, durationSeconds, criticos, removidos, sectionParams, shouldApplyMultiplier, points);
                }
            }
        }

        // Calcular pontuação de pausas para todos os papéis
        if (colabParams != null) {
            totalScore += calculateColabPausasScore(pausaMensalSeconds, colabParams, shouldApplyMultiplier, points);
        }

        // Adicionar pontuação total ao mapa de pontos
        points.put("Total", totalScore);

        // Log apenas em nível debug
        if (log.isDebugEnabled()) {
            log.debug("Score final para role {}: {} (shiftHour={})", role, totalScore, shiftHour);
            log.debug("Pontos calculados: {}", points);
        }

        return points;
    }

    private int calculateTarmScore(Long duration, Integer removidos, ScoringSectionParams params, Map<String, Integer> points) {
        int score = 0;
        int point;

        // Verificações rápidas para evitar processamento desnecessário
        if (removidos != null && removidos > 0 && params.getRemovidos() != null && !params.getRemovidos().isEmpty()) {
            point = matchRemovidosRule(removidos, params.getRemovidos());
            score += point;
            points.put("Removidos", point);
        }

        if (duration != null && duration > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
            point = matchDurationRule(duration, params.getRegulacao(), false);
            score += point;
            points.put("Regulacao", point);
        }

        return score;
    }

    private int calculateFrotaScore(Long durationRegulacao, Long pausa, Long durationSaidaVtr, ScoringSectionParams params, Map<String, Integer> points) {
        int score = 0;
        int point;

        // Verificações rápidas para evitar processamento desnecessário
        if (durationRegulacao != null && durationRegulacao > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
            point = matchDurationRule(durationRegulacao, params.getRegulacao(), false);
            score += point;
            points.put("Regulacao", point);
        }

        if (durationSaidaVtr != null && durationSaidaVtr > 0 && params.getSaidaVtr() != null && !params.getSaidaVtr().isEmpty()) {
            point = matchDurationRule(durationSaidaVtr, params.getSaidaVtr(), false);
            score += point;
            points.put("SaidaVTR", point);
        }

        if (pausa != null && pausa > 0 && params.getPausas() != null && !params.getPausas().isEmpty()) {
            point = matchDurationRule(pausa, params.getPausas(), false);
            score += point;
            points.put("Pausas", point);
        }

        return score;
    }

    private int calculateColabPausasScore(Long pausaSeconds, ScoringSectionParams colabParams, boolean applyMultiplier, Map<String, Integer> points) {
        int score = 0;

        // Verificação rápida para evitar processamento desnecessário
        if (pausaSeconds != null && pausaSeconds > 0 && colabParams.getPausas() != null && !colabParams.getPausas().isEmpty()) {
            int point = matchDurationRule(pausaSeconds, colabParams.getPausas(), applyMultiplier);
            score += point;
            points.put("Pausas", point);
        }

        return score;
    }

    private int calculateMedicoScore(String medicRole, Long duration, Long criticos, Integer removidos, ScoringSectionParams params, boolean applyMultiplier, Map<String, Integer> points) {
        int score = 0;

        // Verificação rápida para evitar processamento desnecessário
        if (removidos != null && removidos > 0 && params.getRemovidos() != null && !params.getRemovidos().isEmpty()) {
            int point = matchRemovidosRule(removidos, params.getRemovidos());
            score += point;
            points.put("Removidos", point);
        }

        // Usar switch expression para código mais limpo e eficiente
        switch (medicRole) {
            case "LIDER" -> {
                if (criticos != null && criticos > 0 && params.getRegulacaoLider() != null && !params.getRegulacaoLider().isEmpty()) {
                    int point = matchDurationRule(criticos, params.getRegulacaoLider(), applyMultiplier);
                    score += point;
                    points.put("Criticos", point);
                }
            }
            case "REGULADOR" -> {
                if (duration != null && duration > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
                    int point = matchDurationRule(duration, params.getRegulacao(), applyMultiplier);
                    score += point;
                    points.put("Regulacao", point);
                }
            }
            case "LIDER_REGULADOR" -> {
                if (criticos != null && criticos > 0 && params.getRegulacaoLider() != null && !params.getRegulacaoLider().isEmpty()) {
                    int point = matchDurationRule(criticos, params.getRegulacaoLider(), applyMultiplier);
                    score += point;
                    points.put("Criticos", point);
                }
                if (duration != null && duration > 0 && params.getRegulacao() != null && !params.getRegulacao().isEmpty()) {
                    int point = matchDurationRule(duration, params.getRegulacao(), applyMultiplier);
                    score += point;
                    points.put("Regulacao", point);
                }
            }
            default -> {
                if (log.isDebugEnabled()) {
                    log.debug("Papel médico desconhecido: {}", medicRole);
                }
            }
        }

        return score;
    }

    private int matchRemovidosRule(Integer value, List<ScoringRule> rules) {
        if (value == null || value <= 0 || rules == null || rules.isEmpty()) return 0;

        // Usar cache para evitar recálculos
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

        // Usar cache para evitar recálculos
        String cacheKey = "duration_" + seconds + "_" + rules.hashCode() + "_" + applyMultiplier;
        return ruleCache.computeIfAbsent(cacheKey, k -> {
            // Pré-processar regras aplicáveis para evitar stream repetido
            List<ScoringRule> applicableRules = rules.stream()
                    .filter(r -> r.getDuration() != null && r.getPoints() != null)
                    .map(originalRule -> {
                        ScoringRule adjustedRule = new ScoringRule();
                        adjustedRule.setPoints(originalRule.getPoints());
                        long duration = originalRule.getDuration();
                        if (applyMultiplier) {
                            duration = duration / 2;
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

    // Método para limpar o cache quando necessário (por exemplo, quando as regras são atualizadas)
    public void clearCache() {
        ruleCache.clear();
    }
}

