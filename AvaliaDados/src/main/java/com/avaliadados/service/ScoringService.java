package com.avaliadados.service;

import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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
        log.info("Iniciando cálculo de score para role={}, duration={}s, quantity={}, pausa={}s",
                role, durationSeconds, quantity, pausaMensalSeconds);

        if (params == null || role == null) {
            log.error("Parâmetros nulos: role={}, params={}", role, params);
            return 0;
        }

        String roleType = role.split("_")[0].toUpperCase();
        ScoringSectionParams sectionParams = switch (roleType) {
            case "TARM" -> params.getTarm();
            case "FROTA" -> params.getFrota();
            case "MEDICO" -> params.getMedico();
            case "COLAB" -> params.getColab();
            default -> null;
        };

        if (sectionParams == null) {
            log.warn("Nenhuma configuração encontrada para roleType: {}", roleType);
            return 0;
        }

        int total = switch (roleType) {
            case "TARM" -> {
                var tarm = calculateTarmScore(durationSeconds, quantity, pausaMensalSeconds, sectionParams);
                var colab = calculateColb(pausaMensalSeconds, sectionParams);
                yield tarm + colab;
            }
            case "FROTA" -> {
                var frota = matchDurationRule(durationSeconds, sectionParams.getRegulacao());
                var colab = calculateColb(pausaMensalSeconds, sectionParams);
                yield frota + colab;

            }
            case "MEDICO" -> {
                var medico = calculateMedicoScore(role, durationSeconds, quantity, sectionParams);
                var colab = calculateColb(pausaMensalSeconds, sectionParams);
                yield medico + colab;
            }
            default -> 0;
        };

        log.info("Score final para role {}: {}", roleType, total);
        return total;
    }

    private int calculateTarmScore(Long duration, Integer quantity, Long pausa, ScoringSectionParams params) {
        log.info("Calculando TARM: duration={}s, quantity={}, pausa={}s", duration, quantity, pausa);
        int score = 0;
        score += matchQuantityRule(quantity, params.getRemovidos());
        score += matchDurationRule(duration, params.getRegulacao());
        score += matchDurationRule(pausa, params.getPausas());
        log.info("TARM partial score: {} | Removidos, Regulacao, Pausas: [{}], [{}], [{}]",
                score,
                params.getRemovidos(),
                params.getRegulacao(),
                params.getPausas());
        return score;
    }

    private int calculateColb(Long pausa, ScoringSectionParams params) {
        log.info("Calculando Colab:  pausa={}s", pausa);
        int score = 0;

        score += matchDurationRule(pausa, params.getPausas());
        log.info("Colab partial score: {} |, Pausas: [{}]",
                score,
                params.getPausas());
        return score;
    }

    private int calculateMedicoScore(String role, Long duration, Integer quantity, ScoringSectionParams params) {
        log.info(">> calculateMedicoScore ENTRY: role='{}', duration={}s, quantity={}, params={}",
                role, duration, quantity, params);

        int score = 0;

        int qtyPoints = matchQuantityRule(quantity, params.getRemovidos());
        log.info("   matchQuantityRule: quantity={} ➔ pointsFromQty={} (rules={})",
                quantity, qtyPoints, params.getRemovidos());
        score += qtyPoints;

        List<ScoringRule> durationRules = role.contains("LIDER")
                ? params.getRegulacaoLider()
                : params.getRegulacao();
        log.info("   Selected durationRules for role='{}': {}", role, durationRules);

        // 3) Regra de duração
        int durPoints = matchDurationRule(duration, durationRules);
        log.info("   matchDurationRule: duration={}s ➔ pointsFromDuration={} (rules={})",
                duration, durPoints, durationRules);
        score += durPoints;

        // 4) Resultado parcial e final
        log.info("<< calculateMedicoScore EXIT: totalScore={} (qty={}, duration={})",
                score, qtyPoints, durPoints);
        return score;
    }

    private int matchQuantityRule(Integer value, List<ScoringRule> rules) {
        if (value == null || rules == null || rules.isEmpty()) {
            log.info("matchQuantityRule: valor nulo ou sem regras (value={}, rules={})", value, rules);
            return 0;
        }
        log.info("matchQuantityRule: valor={} contra regras {}", value, rules);
        int points = rules.stream()
                .filter(r -> r.getQuantity() != null && value <= r.getQuantity())
                .findFirst()
                .map(r -> {
                    log.info("Quantidade casada: value={} <= {} → points={} ", value, r.getQuantity(), r.getPoints());
                    return r.getPoints();
                })
                .orElse(0);
        log.info("matchQuantityRule result: {}", points);
        return points;
    }

    private int matchDurationRule(Long seconds, List<ScoringRule> rules) {
        if (seconds == null || rules == null || rules.isEmpty()) {
            log.info("matchDurationRule: valor nulo ou sem regras (seconds={}, rules={})", seconds, rules);
            return 0;
        }
        log.info("matchDurationRule ENTRY: seconds={}s | incoming rules:", seconds);
        rules.forEach(r ->
                log.info("  → rule(duration={}, points={})", r.getDuration(), r.getPoints())
        );

        List<ScoringRule> sorted = rules.stream()
                .filter(r -> r.getDuration() != null)
                .sorted(Comparator.comparingLong(ScoringRule::getDuration))
                .toList();

        log.info("Sorted rules by duration ascending:");
        sorted.forEach(r ->
                log.info("  → sorted rule(duration={}, points={})", r.getDuration(), r.getPoints())
        );

        for (ScoringRule r : sorted) {
            boolean match = seconds <= r.getDuration();
            log.info("Comparing seconds={} <= {}? → {}", seconds, r.getDuration(), match);
            if (match) {
                log.info("  → CASA rule(duration={}, points={}), returning {}",
                        r.getDuration(), r.getPoints(), r.getPoints());
                if (r.getPoints() != null) {
                    return r.getPoints();
                }
                return 0;
            }
        }

        // 3) Se não casar nenhuma
        log.info("Nenhuma regra satisfeita para seconds={}, retornando 0", seconds);
        return 0;
    }
}