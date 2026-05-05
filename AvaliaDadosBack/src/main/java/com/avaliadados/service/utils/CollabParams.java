package com.avaliadados.service.utils;

import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.api.ApiOptions;
import com.avaliadados.model.api.ApiRequest;
import com.avaliadados.model.api.DateRange;
import com.avaliadados.model.api.EventDetails;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.service.ScoringService;
import com.avaliadados.service.factory.ApiColabData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.avaliadados.service.utils.SheetsUtils.parseTimeToSeconds;

@Slf4j
@RequiredArgsConstructor
@Service
public class CollabParams {
    private final ScoringService scoringService;
    private final ApiColabData apiColabData;

    public int setParams(ProjectCollaborator pc, ProjetoEntity project, int removeds, int removedsLider, long duration, long criticos, long pausaMensal, long saidaVtr) {
        if (pc.getRole() == null) return 0;

        NestedScoringParameters params = Optional.ofNullable(pc.getParametros())
                .orElseGet(() -> {
                    pc.setParametros(new NestedScoringParameters());
                    return pc.getParametros();
                });
        assert params != null;
        var section = params.getColab();


        switch (pc.getRole()) {
            case "TARM" -> section = params.getTarm();
            case "FROTA" -> section = params.getFrota();
            case "MEDICO" -> section = params.getMedico();
        }


        section.setPausas(List.of(ScoringRule.builder().duration(pausaMensal).build()));
        section.setRegulacao(List.of(ScoringRule.builder().duration(duration).build()));
        section.setRegulacaoLider(List.of(ScoringRule.builder().duration(criticos).build()));
        section.setRemovidos((List.of(ScoringRule.builder().quantity(removeds).build())));
        section.setRemovidosLider((List.of(ScoringRule.builder().quantity(removedsLider).build())));
        pc.setRemovidos(removeds);


        var pausas = section.getPausas().getLast().getDuration();
        var regulacao = section.getRegulacao().getLast().getDuration();
        var removidos = section.getRemovidos().getLast().getQuantity();
        var removidosLider = section.getRemovidosLider().getLast().getQuantity();
        Long saida = 0L;
        var regulacaoLider = section.getRegulacaoLider().getLast().getDuration();

        if (pc.getMedicoRole() != null) {
            if (pc.getMedicoRole().equals(MedicoRole.LIDER)) {
                section.setRegulacaoLider((List.of(ScoringRule.builder().duration(criticos).build())));
                regulacaoLider = section.getRegulacaoLider().getLast().getDuration();
            }
        }
        if (pc.getRole().equals("FROTA")) {
            section.setSaidaVtr((List.of(ScoringRule.builder().duration(saidaVtr).build())));
            saida = section.getSaidaVtr().getLast().getDuration();
        }

        if (pc.getMedicoRole() == null) {
            pc.setMedicoRole(MedicoRole.NENHUM);
        }
        Map<String, Integer> pontos = scoringService.calculateCollaboratorScore(
                pc.getRole(),
                pc.getMedicoRole().name(),
                regulacao,
                regulacaoLider,
                removidos,
                removidosLider,
                pausas,
                saida,
                project.getParameters()
        );
        pc.setPoints(pontos);


        return pontos.get("Total");

    }

    public void setDataFromApi(Map<ProjectCollaborator, String> pcToIdMap, ProjetoEntity projeto) {

        if (pcToIdMap == null || pcToIdMap.isEmpty()) return;

        List<String> agentsCleaned = pcToIdMap.values().stream()
                .filter(Objects::nonNull)
                .map(id -> id.replaceAll("[^0-9]", "").replaceFirst("^0+(?!$)", ""))
                .filter(id -> !id.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (agentsCleaned.isEmpty()) return;

        CompletableFuture<Map<String, Map<String, EventDetails>>> pausesFuture =
                CompletableFuture.supplyAsync(() -> fetchEventData("pause", agentsCleaned, true, false, projeto));
        CompletableFuture<Map<String, Map<String, EventDetails>>> removedsFuture =
                CompletableFuture.supplyAsync(() -> fetchEventData("removed", agentsCleaned, false, true, projeto));

        Map<String, Map<String, EventDetails>> pausesData;
        Map<String, Map<String, EventDetails>> removedsData;

        try {
            CompletableFuture.allOf(pausesFuture, removedsFuture).join();
            pausesData = pausesFuture.get();
            removedsData = removedsFuture.get();
        } catch (Exception e) {
            log.error("Erro ao buscar dados da API", e);
            return;
        }

        for (Map.Entry<ProjectCollaborator, String> entry : pcToIdMap.entrySet()) {
            ProjectCollaborator pc = entry.getKey();
            String rawId = entry.getValue();

            if (rawId == null) continue;

            String agentId = rawId.replaceAll("[^0-9]", "").replaceFirst("^0+(?!$)", "");

            log.info("[MATCH] colaborador={}, agentId={}, found={}",
                    pc.getNome(), agentId, pausesData.containsKey(agentId));

            if (pausesData.containsKey(agentId)) {
                EventDetails pauseDetails = pausesData.get(agentId).get("pause");
                if (pauseDetails != null && pauseDetails.getHistory() != null) {

                    // ✅ Log todos os itens brutos antes de qualquer filtro
                    log.info("[HISTORICO BRUTO] colaborador={}, totalItens={}",
                            pc.getNome(), pauseDetails.getHistory().size());
                    pauseDetails.getHistory().forEach(h ->
                            log.info("[ITEM BRUTO] type={}, duration='{}'", h.getType(), h.getDuration())
                    );

                    long totalPauseSeconds = pauseDetails.getHistory().stream()
                            .filter(h -> !"CONFERENCE".equals(h.getType()))
                            .filter(h -> h.getDuration() != null && !h.getDuration().isBlank())
                            .filter(h -> !h.getDuration().trim().startsWith("-")) // ✅ ignora negativos
                            .mapToLong(item -> parseTimeToSeconds(item.getDuration()))
                            .filter(s -> s > 0)
                            .sum();
                    Integer plantao = pc.getPlantao();
                    if (plantao != null && plantao > 0) {
                        log.info("[PAUSA] Colaborador={}, plantao={}, totalPauseSeconds={}, resultado={}s",
                                pc.getNome(), plantao, totalPauseSeconds, totalPauseSeconds / plantao);
                        pc.setPausaMensalSeconds(totalPauseSeconds / plantao);
                    } else {
                        pc.setPausaMensalSeconds(0L);
                    }
                }
            } else {
                log.warn("[PAUSA NÃO ENCONTRADA] colaborador={}, agentId={}", pc.getNome(), agentId);
            }

            if (removedsData.containsKey(agentId)) {
                EventDetails removedDetails = removedsData.get(agentId).get("removed");
                if (removedDetails != null && removedDetails.getTotal() != null) {
                    if (pc.getMedicoRole() == MedicoRole.LIDER) {
                        pc.setRemovidosLider(removedDetails.getTotal());
                    } else {
                        pc.setRemovidos(removedDetails.getTotal());
                    }
                }
            }
        }
    }

    public void setDataFromApi(List<ProjectCollaborator> pcs, ProjetoEntity projeto, List<String> ids) {
        Map<ProjectCollaborator, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pcs.size(); i++) {
            map.put(pcs.get(i), i < ids.size() ? ids.get(i) : null);
        }
        setDataFromApi(map, projeto);
    }

    private Map<String, Map<String, EventDetails>> fetchEventData(String eventName, List<String> agentIds, boolean history, boolean total, ProjetoEntity projeto) {
        String[] mesAno = projeto.getMonth().split("-");
        Month mesReal = Month.of(Integer.parseInt(mesAno[0]));
        Year anoReal = Year.of(Integer.parseInt(mesAno[1]));

        int dia = mesReal.length(anoReal.isLeap());
        String nomeMes = String.format("%02d", mesReal.getValue());
        String endData = String.format("%d/%s/%d", dia, nomeMes, anoReal.getValue());
        String initialData = String.format("01/%s/%d", nomeMes, anoReal.getValue());

        ApiOptions options = ApiOptions.builder()
                .history(history)
                .total(total)
                .duration_average(false)
                .build();

        ApiRequest request = ApiRequest.builder()
                .events(List.of(eventName))
                .agents_id(agentIds)
                .date_rage(DateRange.builder().start(initialData).end(endData).build())
                .options(options)
                .build();

        try {
            Map<String, Map<String, EventDetails>> response = apiColabData.consult(request);
            if (response.isEmpty()) {
                return Collections.emptyMap();
            }
            return response;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}

