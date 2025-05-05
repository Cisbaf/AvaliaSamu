package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorRequest;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.TarmEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.FrotaRepository;
import com.avaliadados.repository.TarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollaboratorsMapper {
    private final FrotaRepository frotaRepository;
    private final TarmRepository tarmRepository;
    private final CollaboratorRepository collaboratorRepository;

    protected FrotaEntity toFrotaEntity(CollaboratorRequest dto) {
        return new FrotaEntity(
                dto.nome(),
                dto.cpf(),
                dto.idCallRote(),
                dto.pontuacao(),
                dto.role(),
                dto.regulacaoMedica()
        );
    }

    protected TarmEntity toTarmEntity(CollaboratorRequest dto) {
        return new TarmEntity(
                dto.nome(),
                dto.cpf(),
                dto.idCallRote(),
                dto.pontuacao(),
                dto.role(),
                dto.tempoRegulaco()

        );
    }

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

    public CollaboratorEntity buscarPorId(String id) {
        var collaborator = collaboratorRepository.findById(id);
        return switch (collaborator.get().getRole().toUpperCase()) {
            case "FROTA" -> frotaRepository.findById(id).orElseThrow();
            case "TARM" -> tarmRepository.findById(id).orElseThrow();
            default -> throw new IllegalArgumentException("Tipo inválido");
        };
    }

    protected CollaboratorEntity createNewEntityByRole(CollaboratorRequest request) {
        return switch (request.role().toUpperCase()) {
            case "TARM" -> new TarmEntity(
                    request.nome(),
                    request.cpf(),
                    request.idCallRote(),
                    request.pontuacao(),
                    request.role(),
                    request.tempoRegulaco()
            );
            case "FROTA" -> new FrotaEntity(
                    request.nome(),
                    request.cpf(),
                    request.idCallRote(),
                    request.pontuacao(),
                    request.role(),
                    request.regulacaoMedica()
            );
            default -> throw new IllegalArgumentException("Role inválido: " + request.role());
        };
    }

}
