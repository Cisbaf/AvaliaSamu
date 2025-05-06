package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorRequest;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.TarmEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollaboratorsMapper {

    protected CollaboratorsResponse toCollaboratorsResponse(CollaboratorEntity entity) {
        return new CollaboratorsResponse(
                entity.getId(),
                entity.getNome(),
                entity.getCpf(),
                entity.getIdCallRote(),
                entity.getPontuacao(),
                entity.getRole()
        );
    }
    protected CollaboratorEntity createByRole(CollaboratorRequest request) {
        return switch (request.role().toUpperCase()) {
            case "TARM" -> new TarmEntity(
                    request.nome(),
                    request.cpf(),
                    request.idCallRote(),
                    request.pontuacao(),
                    request.role(),
                    request.tempoRegulaco(),
                    null
            );
            case "FROTA" -> new FrotaEntity(
                    request.nome(),
                    request.cpf(),
                    request.idCallRote(),
                    request.pontuacao(),
                    request.role(),
                    request.regulacaoMedica(),
                    null
            );
            default -> throw new IllegalArgumentException("Role inválido: " + request.role());
        };
    }

}
