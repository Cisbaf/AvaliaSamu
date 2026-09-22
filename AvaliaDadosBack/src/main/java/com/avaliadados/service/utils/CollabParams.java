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

        // Supervisor com equipe definida: a pontuação dele é a média da pontuação dos
        // colaboradores da equipe (não passa pelas regras normais de Colab/pausas).
        // Sem equipe, cai no cálculo antigo (seção Colab) mais abaixo.
        if ("SUPERVISOR".equals(pc.getRole())) {
            Integer mediaEquipe = calcularMediaEquipe(pc, project);
            if (mediaEquipe != null) {
                pc.setPoints(new HashMap<>(Map.of("MediaEquipe", mediaEquipe, "Total", mediaEquipe)));
                return mediaEquipe;
            }
        }

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
                project.parametersFor(pc.getWorkPeriod())
        );
        pc.setPoints(pontos);


        return pontos.get("Total");

    }

    // Retorna a média (arredondada) da pontuação dos membros da equipe do supervisor
    // NESTE projeto, ou null se o supervisor não tem equipe (ou nenhum membro da
    // equipe foi encontrado entre os colaboradores do projeto) — sinal para usar o
    // cálculo antigo em setParams.
    private Integer calcularMediaEquipe(ProjectCollaborator supervisor, ProjetoEntity project) {
        List<String> equipe = supervisor.getEquipeIds();
        if (equipe == null || equipe.isEmpty() || project.getCollaborators() == null) return null;

        Set<String> membrosIds = new HashSet<>(equipe);
        List<Integer> pontos = project.getCollaborators().stream()
                .filter(pc -> pc != supervisor)
                .filter(pc -> membrosIds.contains(pc.getCollaboratorId()))
                .map(pc -> Optional.ofNullable(pc.getPontuacao()).orElse(0))
                .toList();

        if (pontos.isEmpty()) return null;

        double media = pontos.stream().mapToInt(Integer::intValue).average().orElse(0);
        return (int) Math.round(media);
    }

    // Recalcula a pontuação de todos os supervisores com equipe definida neste
    // projeto. Deve ser chamado sempre que a pontuação de algum colaborador do
    // projeto mudar (import de planilha, edição manual, adição/remoção de membro),
    // pois a média do(s) supervisor(es) pode ter mudado junto.
    public void recalcularSupervisoresComEquipe(ProjetoEntity projeto) {
        if (projeto.getCollaborators() == null) return;
        projeto.getCollaborators().stream()
                .filter(pc -> "SUPERVISOR".equals(pc.getRole()))
                .filter(pc -> pc.getEquipeIds() != null && !pc.getEquipeIds().isEmpty())
                .forEach(supervisor -> {
                    int pontos = setParams(
                            supervisor,
                            projeto,
                            Optional.ofNullable(supervisor.getRemovidos()).orElse(0),
                            Optional.ofNullable(supervisor.getRemovidosLider()).orElse(0),
                            Optional.ofNullable(supervisor.getDurationSeconds()).orElse(0L),
                            Optional.ofNullable(supervisor.getCriticos()).orElse(0L),
                            Optional.ofNullable(supervisor.getPausaMensalSeconds()).orElse(0L),
                            Optional.ofNullable(supervisor.getSaidaVtrSeconds()).orElse(0L)
                    );
                    supervisor.setPontuacao(pontos);
                });
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

        Map<String, Map<String, EventDetails>> allData = fetchEventData(List.of("pause", "removed"), agentsCleaned, true, true, projeto);

        if (allData.isEmpty()) return;

        for (Map.Entry<ProjectCollaborator, String> entry : pcToIdMap.entrySet()) {
            ProjectCollaborator pc = entry.getKey();
            String rawId = entry.getValue();

            if (rawId == null) continue;

            String agentId = rawId.replaceAll("[^0-9]", "").replaceFirst("^0+(?!$)", "");

            Map<String, EventDetails> agentData = allData.get(agentId);
            if (agentData == null) {
                log.warn("[DADOS NÃO ENCONTRADOS] colaborador={}, agentId={}", pc.getNome(), agentId);
                continue;
            }

            EventDetails pauseDetails = agentData.get("pause");
            if (pauseDetails != null && pauseDetails.getHistory() != null) {
                long totalPauseSeconds = pauseDetails.getHistory().stream()
                        .filter(h -> !"CONFERENCE".equals(h.getType()))
                        .filter(h -> h.getDuration() != null && !h.getDuration().isBlank())
                        .filter(h -> !h.getDuration().trim().startsWith("-"))
                        .mapToLong(item -> parseTimeToSeconds(item.getDuration()))
                        .filter(s -> s > 0)
                        .sum();

                Integer plantao = pc.getPlantao();
                if (plantao != null && plantao > 0) {
                    pc.setPausaMensalSeconds(totalPauseSeconds / plantao);
                } else {
                    pc.setPausaMensalSeconds(0L);
                }
            }

            EventDetails removedDetails = agentData.get("removed");
            if (removedDetails != null && removedDetails.getTotal() != null) {
                if (pc.getMedicoRole() == MedicoRole.LIDER) {
                    pc.setRemovidosLider(removedDetails.getTotal());
                } else {
                    pc.setRemovidos(removedDetails.getTotal());
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

    private Map<String, Map<String, EventDetails>> fetchEventData(List<String> eventNames, List<String> agentIds, boolean history, boolean total, ProjetoEntity projeto) {
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

        int chunkSize = 50;
        List<List<String>> chunks = new ArrayList<>();
        for (int i = 0; i < agentIds.size(); i += chunkSize) {
            chunks.add(agentIds.subList(i, Math.min(i + chunkSize, agentIds.size())));
        }

        List<CompletableFuture<Map<String, Map<String, EventDetails>>>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> {
                    ApiRequest request = ApiRequest.builder()
                            .events(eventNames)
                            .agents_id(chunk)
                            .date_rage(DateRange.builder().start(initialData).end(endData).build())
                            .options(options)
                            .build();
                    try {
                        return apiColabData.consult(request);
                    } catch (Exception e) {
                        log.error("Erro ao buscar chunk da API", e);
                        return Collections.<String, Map<String, EventDetails>>emptyMap();
                    }
                }))
                .toList();

        Map<String, Map<String, EventDetails>> combinedResults = new HashMap<>();
        for (CompletableFuture<Map<String, Map<String, EventDetails>>> future : futures) {
            try {
                combinedResults.putAll(future.get());
            } catch (Exception e) {
                log.error("Erro ao combinar resultados da API", e);
            }
        }

        return combinedResults;
    }
}
