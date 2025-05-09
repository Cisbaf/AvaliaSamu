package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorRequest;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.MedicoEntity;
import com.avaliadados.model.TarmEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
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
        String role = request.role().toUpperCase();

        if (role.startsWith("MEDICO")) {
            String[] parts = role.split("_");
            MedicoRole medicoRole = MedicoRole.valueOf(parts[1]);
            ShiftHours shiftHours = ShiftHours.valueOf(parts[2]); // Converte "12H" para H12

            return new MedicoEntity(
                    request.nome(),
                    request.cpf(),
                    request.idCallRote(),
                    request.pontuacao(),
                    role, // Armazena o role completo
                    medicoRole,
                    shiftHours,
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
    }}

    }