package com.avaliadados.service.utils;

import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.SheetRowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static com.avaliadados.service.utils.SheetsUtils.*;

@Service
@RequiredArgsConstructor
public class SheetProcessingService {

    private final SheetRowRepository rowRepository;

    public Optional<SheetRow> findAndAssociateSheetRow(String collaboratorId, String projectId, String collaboratorName) {
        SheetRow sheetColab = rowRepository.findByCollaboratorIdAndProjectId(collaboratorId, projectId);
        if (sheetColab != null) {
            return Optional.of(sheetColab);
        }

        String nomeNormalizado = normalizeName(collaboratorName);
        List<SheetRow> todasLinhas = rowRepository.findByProjectId(projectId);

        return todasLinhas.stream().filter(row -> {
            String nomeMedico = Stream.of(
                            row.getData().get("MEDICO.REGULADOR"),
                            row.getData().get("MEDICO.LIDER"),
                            row.getData().get("COLABORADOR")

                    )
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            return nomeMedico != null &&
                    similarity(normalizeName(nomeMedico), nomeNormalizado) >= 0.85;
        }).findFirst().map(row -> {
            row.setCollaboratorId(collaboratorId);
            rowRepository.save(row);
            return row;
        });
    }

    public void populateFromSheet(ProjectCollaborator pc, SheetRow sheetRow) {
        Map<String, String> data = sheetRow.getData();

        switch (pc.getRole()) {
            case "TARM":
                processTarm(pc, data);
                break;

            case "FROTA":
                processFrota(pc, data);
                break;

            case "MEDICO":
                processMedico(pc, data);
                break;

            default:
                break;
        }
    }

    private void processTarm(ProjectCollaborator pc, Map<String, String> data) {
        String tempo = data.get("TEMPO.REGULACAO.TARM");
        String plantao = data.get("PLANTAO");
        int plantaotemp = (int) Math.round(Double.parseDouble(Objects.equals(plantao, "00:00:00") ? "0" : plantao));

        if (tempo != null) {
            Long segundos = parseTimeToSeconds(tempo);
            pc.setDurationSeconds(segundos);
            pc.setParametros(NestedScoringParameters.builder().tarm(ScoringSectionParams.builder().regulacao(List.of(ScoringRule.builder().duration(segundos).build())).build()).build());
            pc.setPlantao(plantaotemp);
        }
    }

    private void processFrota(ProjectCollaborator pc, Map<String, String> data) {
        String tempo = data.get("TEMPO.REGULACAO.FROTA");
        String plantao = data.get("PLANTAO");
        int plantaotemp = (int) Math.round(Double.parseDouble(Objects.equals(plantao, "00:00:00") ? "0" : plantao));

        if (tempo != null) {
            Long segundos = parseTimeToSeconds(tempo);
            pc.setDurationSeconds(segundos);
            pc.setParametros(NestedScoringParameters.builder().frota(ScoringSectionParams.builder().regulacao(List.of(ScoringRule.builder().duration(segundos).build())).build()).build());
            pc.setPlantao(plantaotemp);
        }
    }

    private void processMedico(ProjectCollaborator pc, Map<String, String> data) {
        MedicoRole role = Optional.ofNullable(pc.getMedicoRole()).orElse(MedicoRole.NENHUM);
        String plantao = data.get("PLANTAO");
        int plantaotemp = (int) Math.round(Double.parseDouble(Objects.equals(plantao, "00:00:00") ? "0" : plantao));

        switch (role) {
            case REGULADOR:
                String tempoReg = data.get("TEMPO.REGULACAO");
                if (tempoReg != null) {
                    Long segundos = parseTimeToSeconds(tempoReg);
                    pc.setDurationSeconds(segundos);
                    pc.setParametros(NestedScoringParameters.builder().medico(ScoringSectionParams.builder().regulacao(List.of(ScoringRule.builder().duration(segundos).build())).build()).build());
                    pc.setPlantao(plantaotemp);
                }
                break;

            case LIDER:
                String criticos = data.get("CRITICOS");
                if (criticos != null) {
                    Long segundos = parseTimeToSeconds(criticos);
                    pc.setDurationSeconds(segundos);
                    pc.setParametros(NestedScoringParameters.builder().medico(ScoringSectionParams.builder().regulacaoLider(List.of(ScoringRule.builder().duration(segundos).build())).build()).build());
                    pc.setPlantao(plantaotemp);
                }
                break;

            default:
                break;
        }
    }
}
