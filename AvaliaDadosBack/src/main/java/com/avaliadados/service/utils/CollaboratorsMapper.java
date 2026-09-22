package com.avaliadados.service.utils;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.dto.CollaboratorRequest;
import com.avaliadados.model.dto.CollaboratorsResponse;
import com.avaliadados.model.roles.FrotaEntity;
import com.avaliadados.model.roles.GenericEntity;
import com.avaliadados.model.roles.MedicoEntity;
import com.avaliadados.model.roles.TarmEntity;
import com.avaliadados.model.enums.WorkPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollaboratorsMapper {

    public CollaboratorsResponse toCollaboratorsResponse(CollaboratorEntity entity) {
        var idCallRote = entity.getIdCallRote().startsWith("0") ? entity.getIdCallRote().substring(1) : entity.getIdCallRote();

        CollaboratorsResponse.CollaboratorsResponseBuilder builder = CollaboratorsResponse.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .cpf(entity.getCpf().replace(".", "").replace("-", ""))
                .idCallRote(idCallRote.replace("-", "").replace(".", ""))
                .role(entity.getRole())
                .workPeriod(entity.getWorkPeriod() != null ? entity.getWorkPeriod() : WorkPeriod.DIURNO)
                .pontuacao(entity.getPontuacao());

        if (entity instanceof MedicoEntity medico) {
            builder.medicoRole(medico.getMedicoRole())
                    .shiftHours(medico.getShiftHours());
        }

        return builder.build();
    }

    public CollaboratorEntity createByRole(CollaboratorRequest request) {
        String role = request.role().toUpperCase();

        CollaboratorEntity collaborator;
        if (role.startsWith("MEDICO")) {
            collaborator = new MedicoEntity(
                    request.nome().toUpperCase(),
                    request.cpf(),
                    request.idCallRote(),
                    request.pontuacao(),
                    request.role(),
                    request.medicoRole(),
                    request.shiftHours(),
                    0L
            );
        } else {
            collaborator = switch (request.role().toUpperCase()) {
                case "TARM" -> new TarmEntity(
                        request.nome().toUpperCase(),
                        request.cpf(),
                        request.idCallRote(),
                        request.pontuacao(),
                        request.role(),
                        null
                );
                case "FROTA" -> new FrotaEntity(
                        request.nome().toUpperCase(),
                        request.cpf(),
                        request.idCallRote(),
                        request.pontuacao(),
                        request.role(),
                        null
                );
                default ->  new GenericEntity(
                        request.nome().toUpperCase(),
                        request.cpf(),
                        request.idCallRote(),
                        request.pontuacao(),
                        request.role(),
                        null
                );
            };
        }
        collaborator.setWorkPeriod(WorkPeriod.resolve(request.role(), request.medicoRole(), request.shiftHours(), request.workPeriod()));
        return collaborator;
    }

}
