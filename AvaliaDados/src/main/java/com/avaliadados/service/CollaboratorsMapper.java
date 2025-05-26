package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.dto.CollaboratorRequest;
import com.avaliadados.model.dto.CollaboratorsResponse;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.MedicoEntity;
import com.avaliadados.model.TarmEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollaboratorsMapper {

    public CollaboratorsResponse toCollaboratorsResponse(CollaboratorEntity entity) {
        return new CollaboratorsResponse(
                entity.getId(),
                entity.getNome(),
                entity.getCpf(),
                entity.getIdCallRote(),
                entity.getPontuacao(),
                entity.getRole(),
                null,
                null,
                null,
                null,
                null,
                null

        );
    }

    public CollaboratorEntity createByRole(CollaboratorRequest request) {
        String role = request.role().toUpperCase();

        if (role.startsWith("MEDICO")) {
            return new MedicoEntity(
                    request.nome(),
                    request.cpf(),
                    request.idCallRote(),
                    request.pontuacao(),
                    request.role(),
                    request.medicoRole(),
                    request.shiftHours(),
                    0L
            );
        } else {
            return switch (request.role().toUpperCase()) {
                case "TARM" -> new TarmEntity(
                        request.nome(),
                        request.cpf(),
                        request.idCallRote(),
                        request.pontuacao(),
                        request.role(),
                        null
                );
                case "FROTA" -> new FrotaEntity(
                        request.nome(),
                        request.cpf(),
                        request.idCallRote(),
                        request.pontuacao(),
                        request.role(),
                        null
                );
                case "MEDICO", "MEDICO_SUPERVISOR" -> new MedicoEntity(
                        request.nome(),
                        request.cpf(),
                        request.idCallRote(),
                        request.pontuacao(),
                        request.role(),
                        request.medicoRole(),
                        request.shiftHours(),
                        null
                );
                default -> throw new IllegalArgumentException("Role inválido: " + request.role());
            };
        }
    }

}