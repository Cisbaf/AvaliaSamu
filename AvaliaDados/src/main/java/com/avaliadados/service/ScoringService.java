package com.avaliadados.service;

import com.avaliadados.model.RoleCriteriaEntity;
import com.avaliadados.model.CriterionValue;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScoringService {
    private final RoleCriteriaService criteriaService;

    public ScoringService(RoleCriteriaService criteriaService) {
        this.criteriaService = criteriaService;
    }

    /** Pontua por DURAÇÃO (threshold em segundos) */
    public int scoreByRoleDuration(String role, Duration duration) {
        RoleCriteriaEntity crit = criteriaService.getCriteria(role, "DURATION");
        if (crit == null || crit.getValues().isEmpty()) return 0;
        Map<Duration,Integer> map = crit.getValues().stream()
                .collect(Collectors.toMap(
                        v -> Duration.ofSeconds(v.getThreshold()),
                        CriterionValue::getScore
                ));
        return map.entrySet().stream()
                .filter(e -> !e.getKey().minus(duration).isNegative()) // key <= duration
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElse(0);
    }

    /** Pontua por QUANTIDADE (threshold valor mínimo) */
    public int scoreByRoleQuantity(String role, int amount) {
        RoleCriteriaEntity crit = criteriaService.getCriteria(role, "QUANTITY");
        if (crit == null || crit.getValues().isEmpty()) return 0;
        return crit.getValues().stream()
                .filter(v -> amount >= v.getThreshold())
                .max((a,b) -> Long.compare(a.getThreshold(), b.getThreshold()))
                .map(CriterionValue::getScore)
                .orElse(0);
    }
}