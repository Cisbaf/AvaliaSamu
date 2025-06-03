package com.avaliadados.service.utils;

import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.params.NestedScoringParameters;
import com.avaliadados.model.params.ScoringRule;
import com.avaliadados.model.params.ScoringSectionParams;
import com.avaliadados.repository.SheetRowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.avaliadados.service.utils.SheetsUtils.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SheetProcessingService {

    private final SheetRowRepository rowRepository;

    /**
     * Tenta buscar a SheetRow para esse colaborador:
     * 1) procura por (collaboratorId, projectId)
     * 2) se não achar, varre todas as linhas do projeto e tenta associar por similaridade de nome
     */
    public Optional<SheetRow> findAndAssociateSheetRow(String collaboratorId, String projectId, String collaboratorName) {
        // 1) busca direta por ID
        SheetRow sheetColab = rowRepository.findByCollaboratorIdAndProjectId(collaboratorId, projectId);
        if (sheetColab != null) {
            return Optional.of(sheetColab);
        }

        // 2) se não encontrou, tenta por similaridade de nome
        String nomeNormalizado = normalizeName(collaboratorName);
        List<SheetRow> todasLinhas = rowRepository.findByProjectId(projectId);

        return todasLinhas.stream().filter(row -> {
            // pega "MEDICO.REGULADOR" ou, se nulo, "MEDICO.LIDER"
            String nomeMedico = Optional.ofNullable(row.getData().get("MEDICO.REGULADOR")).orElse(row.getData().get("MEDICO.LIDER"));
            if (nomeMedico == null) {
                return false;
            }
            return similarity(normalizeName(nomeMedico), nomeNormalizado) >= 0.85;
        }).findFirst().map(row -> {
            // associa o collaboratorId na linha para persistir a “ligação”
            row.setCollaboratorId(collaboratorId);
            rowRepository.save(row);
            log.info("Associado colaborador [{}] à linha da planilha por similaridade de nome", collaboratorId);
            return row;
        });
    }

    /**
     * Dado um ProjectCollaborator e a linha de planilha correspondente,
     * carrega os campos (durationSeconds, NestedScoringParameters, criticos etc.), dependendo da role e médicoRole.
     */
    public void populateFromSheet(ProjectCollaborator pc, SheetRow sheetRow) {
        log.info("Dados da planilha encontrados para o colaborador [{}]", pc.getCollaboratorId());
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
                // nenhuma ação específica
                break;
        }
    }

    // ----- Métodos auxiliares (copiados e adaptados do ProjectCollabService original) -----

    private void processTarm(ProjectCollaborator pc, Map<String, String> data) {
        String tempo = data.get("TEMPO.REGULACAO.TARM");
        if (tempo != null) {
            Long segundos = parseTimeToSeconds(tempo);
            pc.setDurationSeconds(segundos);
            pc.setParametros(NestedScoringParameters.builder().tarm(ScoringSectionParams.builder().regulacao(List.of(ScoringRule.builder().duration(segundos).build())).build()).build());
            log.debug("Tempo de regulação TARM definido: {} segundos", segundos);
        }
    }

    private void processFrota(ProjectCollaborator pc, Map<String, String> data) {
        String tempo = data.get("TEMPO.REGULACAO.FROTA");
        if (tempo != null) {
            Long segundos = parseTimeToSeconds(tempo);
            pc.setDurationSeconds(segundos);
            pc.setParametros(NestedScoringParameters.builder().frota(ScoringSectionParams.builder().regulacao(List.of(ScoringRule.builder().duration(segundos).build())).build()).build());
            log.debug("Tempo de regulação FROTA definido: {} segundos", segundos);
        }
    }

    private void processMedico(ProjectCollaborator pc, Map<String, String> data) {
        MedicoRole role = Optional.ofNullable(pc.getMedicoRole()).orElse(MedicoRole.NENHUM);

        switch (role) {
            case REGULADOR:
                String tempoReg = data.get("TEMPO.REGULACAO");
                if (tempoReg != null) {
                    Long segundos = parseTimeToSeconds(tempoReg);
                    pc.setDurationSeconds(segundos);
                    pc.setParametros(NestedScoringParameters.builder().medico(ScoringSectionParams.builder().regulacao(List.of(ScoringRule.builder().duration(segundos).build())).build()).build());
                    log.debug("Tempo de regulação MÉDICO REGULADOR definido: {} segundos", segundos);
                }
                break;

            case LIDER:
                String criticos = data.get("CRITICOS");
                if (criticos != null) {
                    Long segundos = parseTimeToSeconds(criticos);
                    pc.setDurationSeconds(segundos);
                    pc.setParametros(NestedScoringParameters.builder().medico(ScoringSectionParams.builder().regulacaoLider(List.of(ScoringRule.builder().duration(segundos).build())).build()).build());
                    log.debug("Tempo de críticos MÉDICO LÍDER definido: {} segundos", segundos);
                }
                break;

            default:
                // nenhum processamento adicional
                break;
        }
    }
}
