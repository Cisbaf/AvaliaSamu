package com.avaliadados.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class ScoringService {

    public int calculateScore(
            String role,
            Duration regulacaoDur,
            Integer quantity,
            Duration pausaMensalDur,
            Map<String, Integer> params
    ) {
        int total = 0;
        String roleKey = role.toUpperCase();

        log.info("Calculando score para role [{}]", roleKey);

        if (regulacaoDur != null) {
            long secs = regulacaoDur.getSeconds();
            switch (roleKey) {
                case "TARM":
                    total += calculateDynamicScore("Regulação TARM", secs, params.get("TARM_tempoRegulacao"));
                    break;
                case "FROTA":
                    total += calculateDynamicScore("Regulação FROTA", secs, params.get("FROTA_tempoRegulacaoFrota"));
                    break;
                case "MEDICO_REGULADOR_12H":
                case "MEDICO_REGULADOR_24H":
                    total += calculateDynamicScore("Regulação MÉDICO", secs, params.get("MEDICO_REGULADOR_tempoRegulacaoMedica"));
                    break;
                case "MEDICO_LIDER_12H":
                case "MEDICO_LIDER_24H":
                    total += calculateDynamicScore("Regulação LÍDER", secs, params.get("MEDICO_LIDER_tempoRegulacaoLider"));
                    break;
            }
        }

        if (quantity != null) {
            switch (roleKey) {
                case "TARM":
                    int limite = params.getOrDefault("TARM_removidos", 1);
                    int pontos = params.getOrDefault("TARM_pontosRemovidos", 6);
                    int tarmScore = quantity <= limite ? pontos : 0;
                    log.info("Removidos TARM: quantidade={}, limite={}, score={}", quantity, limite, tarmScore);
                    total += tarmScore;
                    break;
                case "MEDICO_REGULADOR_12H":
                case "MEDICO_REGULADOR_24H":
                    int medicoScore = quantity <= 20 ? 6 : quantity <= 30 ? 4 : quantity <= 45 ? 2 : 0;
                    log.info("Removidos MÉDICO: quantidade={}, score={}", quantity, medicoScore);
                    total += medicoScore;
                    break;
            }
        }

        if (pausaMensalDur != null) {
            long secs = pausaMensalDur.getSeconds();
            int basePausa = params.getOrDefault(roleKey + "_pausasMensal", 120); // Base padrão 2 minutos
            total += calculateDynamicScore("Pausa mensal", secs, basePausa);
        }

        log.info("Pontuação final para [{}]: {}", roleKey, total);
        return total;
    }

    private int calculateDynamicScore(String context, long secs, Integer base) {
        if (base == null) {
            log.warn("Base nula para {}", context);
            return 0;
        }

        // Intervalos fixos de 15 segundos a partir do base
        long t1 = base + 15;
        long t2 = base + 30;
        long t3 = base + 45;
        long t4 = base + 60;

        int score;
        if (secs <= base) {
            score = 10;
        } else if (secs <= t1) {
            score = 7;
        } else if (secs <= t2) {
            score = 4;
        } else if (secs <= t3) {
            score = 1;
        } else {
            score = 0;
        }

        log.info("{}: base={}s, t1={}s, t2={}s, t3={}s, t4={}s, valor={}s, score={}",
                context, base, t1, t2, t3, t4, secs, score);

        return score;
    }
}