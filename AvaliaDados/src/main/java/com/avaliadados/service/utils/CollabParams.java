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
import org.springframework.stereotype.Service;

import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.avaliadados.service.utils.SheetsUtils.parseTimeToSeconds;

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

    public void setDataFromApi(
            List<ProjectCollaborator> projectCollaborators,
            ProjetoEntity projeto,
            List<String> idCallroutList) {

        if (projectCollaborators == null || projectCollaborators.isEmpty()) {
            return;
        }

        // Verifica se as listas têm o mesmo tamanho
        if (projectCollaborators.size() != idCallroutList.size()) {

            throw new IllegalArgumentException("Tamanho das listas de colaboradores e IDs não corresponde." +
                    " Colaboradores: " + projectCollaborators.size() + ", IDs: " + idCallroutList.size());
        }

        // Filtra apenas IDs válidos
        List<String> agentIds = idCallroutList.stream()
                .filter(Objects::nonNull)
                .toList();

        if (agentIds.isEmpty()) {
            return;
        }
        List<String> agentsCleaned = agentIds.stream()
                .filter(Objects::nonNull)
                .map(id -> id.replaceAll("-", "").replaceAll("\\.", ""))
                .map(id -> id.startsWith("0") ? id.substring(1) : id)
                .collect(Collectors.toList());

        CompletableFuture<Map<String, Map<String, EventDetails>>> pausesFuture =
                CompletableFuture.supplyAsync(() -> fetchEventData("pause", agentsCleaned, true, false, projeto));
        CompletableFuture<Map<String, Map<String, EventDetails>>> removedsFuture =
                CompletableFuture.supplyAsync(() -> fetchEventData("removed", agentsCleaned, false, true, projeto));

        Map<String, Map<String, EventDetails>> pausesData;
        Map<String, Map<String, EventDetails>> removedsData;

        try {
            pausesData = pausesFuture.get();
            removedsData = removedsFuture.get();
        } catch (Exception e) {
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<CompletableFuture<Void>> futures = new java.util.ArrayList<>();

        // Processa cada colaborador usando o índice em paralelo
        for (int i = 0; i < projectCollaborators.size(); i++) {
            final int index = i; // Variável final para uso na lambda
            futures.add(CompletableFuture.runAsync(() -> {
                ProjectCollaborator pc = projectCollaborators.get(index);
                String agentId = idCallroutList.get(index);
                agentId = agentId != null ? agentId.replaceAll("-", "").replaceAll("\\.", "") : null;
                agentId = agentId != null && agentId.startsWith("0") ? agentId.substring(1) : agentId;

                if (agentId == null) {
                    return; // Continua para o próximo colaborador
                }

                // Processa pausas
                Optional.ofNullable(pausesData.get(agentId))
                        .map(eventMap -> eventMap.get("pause"))
                        .ifPresent(pauseDetails -> {
                            if (pauseDetails.getHistory() != null) {
                                long totalPauseSeconds = pauseDetails.getHistory().stream()
                                        .mapToLong(item -> parseTimeToSeconds(item.getDuration()))
                                        .sum();

                                long total = 0L;
                                if (pc.getPlantao() != null && pc.getPlantao() > 0) {
                                    total = totalPauseSeconds / pc.getPlantao();
                                }
                                pc.setPausaMensalSeconds(total);
                            }
                        });

                // Processa removidos
                Optional.ofNullable(removedsData.get(agentId))
                        .map(eventMap -> eventMap.get("removed"))
                        .ifPresent(removedDetails -> {
                            if (removedDetails.getTotal() != null) {
                                if (pc.getMedicoRole() == MedicoRole.LIDER) {
                                    pc.setRemovidosLider(removedDetails.getTotal());
                                } else {
                                    pc.setRemovidos(removedDetails.getTotal());
                                }
                            }
                        });
            }, executor));
        }

        // Espera todas as tarefas paralelas serem concluídas
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Desliga o executor de threads
        executor.shutdown();
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

