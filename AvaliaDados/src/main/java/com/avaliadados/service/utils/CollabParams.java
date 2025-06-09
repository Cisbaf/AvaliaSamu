package com.avaliadados.service.utils;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.api.ApiRequest;
import com.avaliadados.model.api.ApiResponse;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.service.ScoringService;
import com.avaliadados.service.factory.ApiColabData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Month;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CollabParams {
    private final ScoringService scoringService;
    private final ApiColabData apiColabData;
    private final CollaboratorRepository collaboratorRepository;


    public int setParams(ProjectCollaborator pc, ProjetoEntity project, int removeds, long duration, long criticos, long pausaMensal, long saidaVtr) {
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
            default -> log.warn("Role não informada: {}", pc.getRole());
        }


        section.setPausas(List.of(ScoringRule.builder().duration(pausaMensal).build()));
        section.setRegulacao(List.of(ScoringRule.builder().duration(duration).build()));
        section.setRegulacaoLider(List.of(ScoringRule.builder().duration(criticos).build()));
        section.setRemovidos((List.of(ScoringRule.builder().quantity(removeds).build())));
        pc.setRemovidos(removeds);


        var pausas = section.getPausas().getLast().getDuration();
        var regulacao = section.getRegulacao().getLast().getDuration();
        var removidos = section.getRemovidos().getLast().getQuantity();
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
            log.warn("MedicoRole não informada para colaborador {}, definindo como NENHUM", pc.getNome());
        }
        Map<String, Integer> pontos = scoringService.calculateCollaboratorScore(
                pc.getRole(),
                pc.getMedicoRole().name(),
                pc.getShiftHours() != null ? pc.getShiftHours().name() : "H12",
                regulacao,
                regulacaoLider,
                removidos,
                pausas,
                saida,
                project.getParameters()
        );
        pc.setPoints(pontos);


        return pontos.get("Total");

    }

    public Map<String, Long> setDataFromApi(ProjectCollaborator pc, ProjetoEntity projeto, String idCallRout) {
        log.info("Buscando dados da API para o colaborador {} ({})", pc.getNome(), pc.getRole());
        if (pc.getRemovidos() != null && pc.getRemovidos() > 0 &&
                pc.getPausaMensalSeconds() != null && pc.getPausaMensalSeconds() > 0) {
            return Map.of(
                    "removeds", (long) pc.getRemovidos(),
                    "pauses", pc.getPausaMensalSeconds()
            );
        }
        var collab = collaboratorRepository.getReferenceByIdCallRote(idCallRout);

        ApiRequest request = getApiRequest(projeto, idCallRout);
        List<ApiResponse> removeds = apiColabData.getRemoveds(request);
        System.out.println(pc.getNome());

        Long avgPauseTime = 0L;
        if (pc.getPlantao() != null) {
            List<ApiResponse> pauses = apiColabData.getPauses(request);
            avgPauseTime = calcTime(pauses, pc.getPlantao());
        }

        for(CollaboratorEntity collaborator: collab){
            if (Objects.equals(collaborator.getId(), pc.getCollaboratorId())) {
                pc.setRemovidos(removeds.size());
                pc.setPausaMensalSeconds(avgPauseTime);
            }
        }



        return Map.of(
                "removeds", (long) removeds.size(),
                "pauses", avgPauseTime
        );
    }

    private static ApiRequest getApiRequest(ProjetoEntity projeto, String idCallRout) {
        String[] mesAno = projeto.getMonth().split("-");
        Month mesReal = Month.of(Integer.parseInt(mesAno[0]));
        Year anoReal = Year.of(Integer.parseInt(mesAno[1]));


        int dia = mesReal.length(anoReal.isLeap());
        String nomeMes = String.format("%02d", mesReal.getValue());
        String endData = String.format("%d/%s/%d", dia, nomeMes, anoReal.getValue());
        String initialData = String.format("01/%s/%d", nomeMes, anoReal.getValue());
        System.out.println("Inicial: " + initialData + ", Final: " + endData);

        return new ApiRequest(idCallRout, initialData, endData);
    }

    private Long calcTime(List<ApiResponse> pauses, int plantao) {

        //TODO: total do tempo de pausa / numero de plantoes e depois / 7
        var total = pauses.size();
        var somaTotal = pauses.stream().mapToLong(e -> {
            if (e.start() == null || e.end() == null) {
                return 0L;
            }
            Duration duration = Duration.between(e.start(), e.end());
            return duration.getSeconds();
        }).sum();

        System.out.println("Total de pausas: " + total + ", Soma total de segundos: " + somaTotal + ", Plantão: " + plantao);


        if (total != 0) {
            return  somaTotal / plantao;
        }
        return 0L;
    }
}
