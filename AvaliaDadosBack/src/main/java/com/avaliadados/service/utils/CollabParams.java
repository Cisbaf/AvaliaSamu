package com.avaliadados.service.utils;

import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.api.ApiOptions;
import com.avaliadados.model.api.ApiRequest;
import com.avaliadados.model.api.DateRange;
import com.avaliadados.model.api.EventDetails;
import com.avaliadados.model.api.HistoryItem;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.WorkPeriod;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.service.ScoringService;
import com.avaliadados.service.factory.ApiColabData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
//            log.info("History pausa = {}", pauseDetails.getHistory() );
            if (pauseDetails != null && pauseDetails.getHistory() != null) {
                // Só médico é separado por diurno/noturno. Os outros papéis mantêm o cálculo geral.
                // Líder é 24h e tem parâmetros próprios: não filtra por período nem conta plantões
                // pelos turnos da API (usa o plantão da planilha, como os demais papéis).
                boolean medico = "MEDICO".equals(pc.getRole()) && pc.getMedicoRole() != MedicoRole.LIDER;
                // Médico não existe mais como 24h: cadastro antigo com H24 vira 12h diurno.
                if (medico && (pc.getWorkPeriod() == null || pc.getWorkPeriod() == WorkPeriod.H24)) {
                    pc.setWorkPeriod(WorkPeriod.DIURNO);
                }
                WorkPeriod periodo = pc.getWorkPeriod();

                // Médico: só os itens do período (diurno 07h-19h, noturno 19h-07h).
                List<HistoryItem> itens = pauseDetails.getHistory().stream()
                        .filter(h -> !"CONFERENCE".equals(h.getType()))
                        .filter(h -> h.getDuration() != null && !h.getDuration().isBlank())
                        .filter(h -> !h.getDuration().trim().startsWith("-"))
                        .filter(h -> parseTimeToSeconds(h.getDuration()) > 0)
                        .filter(h -> !medico || pertenceAoPeriodo(h, periodo))
                        .toList();

                long totalPauseSeconds = itens.stream()
                        .mapToLong(h -> parseTimeToSeconds(h.getDuration()))
                        .sum();

                int divisor;
                if (medico) {
                    // Médico: conta os turnos com pausa na API e compara com a planilha; usa o maior.
                    int plantoesApi = contarPlantoes(itens);
                    int plantoesPlanilha = Optional.ofNullable(pc.getPlantao()).orElse(0);
                    divisor = Math.max(plantoesApi, plantoesPlanilha);
                } else {
                    divisor = Optional.ofNullable(pc.getPlantao()).orElse(0);
                }

                pc.setPausaMensalSeconds(pausaPorPlantao(pc, periodo, totalPauseSeconds, divisor));
            }

            EventDetails removedDetails = agentData.get("removed");
//            log.info("History removed = {}", removedDetails );
            if (removedDetails != null && (removedDetails.getTotal() != null || removedDetails.getHistory() != null)) {
                int quantidade = contarRemovidos(pc, removedDetails);
                if (pc.getMedicoRole() == MedicoRole.LIDER) {
                    // Líder tem parâmetros próprios: plantão duplo não se aplica.
                    pc.setRemovidosLider(quantidade);
                } else {
                    pc.setRemovidos(aplicarPlantaoDuplo(pc, quantidade));
                }
            }
        }
    }

    // Plantão duplo: o que o médico recebe (planilha e API) é dividido por 2.
    // Tempos em segundos: divisão inteira. Quantidades (removidos): arredonda para cima no .5
    // (1 removido não vira 0). Depois disso a pontuação é calculada igual à de um médico comum.
    public static long aplicarPlantaoDuplo(ProjectCollaborator pc, long segundos) {
        return pc.plantaoDuploAtivo() ? segundos / 2 : segundos;
    }

    public static int aplicarPlantaoDuplo(ProjectCollaborator pc, int quantidade) {
        return pc.plantaoDuploAtivo() ? (quantidade + 1) / 2 : quantidade;
    }

    // Pausa por plantão. totalSeconds é o TOTAL BRUTO de pausas da API (sem dividir).
    // Médico comum: total / plantões.
    // Plantão duplo: total de pausas ÷ 2 (como todos os valores) e número de plantões
    // ÷ 2 (diurno) ou ÷ 4 (noturno). Sem nenhuma divisão extra sobre o resultado.
    //   = (total / 2) / (plantões / fator) = total * fator / (2 * plantões), fator 2 ou 4.
    // Fazemos a conta numa fração só para não truncar o divisor (ex.: 7 / 2 = 3,5).
    public static long pausaPorPlantao(ProjectCollaborator pc, WorkPeriod periodo, long totalSeconds, int plantoes) {
        if (plantoes <= 0) return 0L;
        if (pc.plantaoDuploAtivo()) {
            long fator = periodo == WorkPeriod.NOTURNO ? 4 : 2;
            return totalSeconds * fator / (2L * plantoes);
        }
        return totalSeconds / plantoes;
    }

    public void setDataFromApi(List<ProjectCollaborator> pcs, ProjetoEntity projeto, List<String> ids) {
        Map<ProjectCollaborator, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pcs.size(); i++) {
            map.put(pcs.get(i), i < ids.size() ? ids.get(i) : null);
        }
        setDataFromApi(map, projeto);
    }

    private static final Pattern DATA_HORA = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})\\D*(\\d{2}:\\d{2}(?::\\d{2})?)");
    private static final LocalTime INICIO_DIURNO = LocalTime.of(7, 0);
    private static final LocalTime FIM_DIURNO = LocalTime.of(19, 0);

    // Diurno: 07h até 18h59:59. Noturno: 19h até 06h59:59.
    static boolean isDiurno(LocalTime t) {
        return !t.isBefore(INICIO_DIURNO) && t.isBefore(FIM_DIURNO);
    }

    // Classifica pelo início do evento. H24 (legado) não filtra.
    static boolean pertenceAoPeriodo(HistoryItem h, WorkPeriod periodo) {
        if (periodo != WorkPeriod.DIURNO && periodo != WorkPeriod.NOTURNO) return true;
        LocalDateTime dt = parseDataHora(h.getDate());
        if (dt == null) return false;
        boolean diurno = isDiurno(dt.toLocalTime());
        return periodo == WorkPeriod.DIURNO ? diurno : !diurno;
    }

    // Removidos: médico conta só os eventos do seu período (diurno/noturno), pela data
    // de cada evento do histórico, igual à pausa. Se a API não trouxer datas válidas,
    // usa o total geral. Os demais papéis continuam usando o total.
    static int contarRemovidos(ProjectCollaborator pc, EventDetails details) {
        int total = Optional.ofNullable(details.getTotal()).orElse(0);
        // Líder (24h) usa sempre o total, sem filtrar por período.
        if (!"MEDICO".equals(pc.getRole()) || pc.getMedicoRole() == MedicoRole.LIDER
                || details.getHistory() == null) return total;

        boolean temData = details.getHistory().stream()
                .anyMatch(h -> parseDataHora(h.getDate()) != null);
        if (!temData) return total;

        WorkPeriod periodo = pc.getWorkPeriod();
        return (int) details.getHistory().stream()
                .filter(h -> pertenceAoPeriodo(h, periodo))
                .count();
    }

    // Lê "dd/MM/yyyy, HH:mm:ss" (ou só a data/hora) do campo date do histórico.
    static LocalDateTime parseDataHora(String s) {
        if (s == null) return null;
        Matcher m = DATA_HORA.matcher(s);
        if (!m.find()) return null;
        try {
            LocalDate d = LocalDate.parse(m.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            LocalTime t = LocalTime.parse(m.group(2), DateTimeFormatter.ofPattern("HH:mm[:ss]"));
            return LocalDateTime.of(d, t);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Cada turno vale um plantão. Madrugada (00h-06h59) pertence ao noturno do dia anterior.
    static int contarPlantoes(List<HistoryItem> itens) {
        Set<String> turnos = new HashSet<>();
        for (HistoryItem h : itens) {
            LocalDateTime dt = parseDataHora(h.getDate());
            if (dt == null) continue;
            LocalTime t = dt.toLocalTime();
            if (isDiurno(t)) {
                turnos.add(dt.toLocalDate() + "-DIURNO");
            } else {
                LocalDate inicio = t.isBefore(INICIO_DIURNO) ? dt.toLocalDate().minusDays(1) : dt.toLocalDate();
                turnos.add(inicio + "-NOTURNO");
            }
        }
        return turnos.size();
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
