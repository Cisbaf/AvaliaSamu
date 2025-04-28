package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.FrotaEntityDto;
import com.avaliadados.model.DTO.TarmEntityDto;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.TarmEntity;
import com.avaliadados.repository.FrotaRepository;
import com.avaliadados.repository.TarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollaboratorsMapper {
    private final FrotaRepository frotaRepository;
    private final TarmRepository tarmRepository;

    protected FrotaEntity toFrotaEntity(FrotaEntityDto dto) {
        return new FrotaEntity(
                dto.getNome(),
                dto.getCpf(),
                dto.getIdCallRote(),
                dto.getPontuacao(),
                dto.getRole(),
                dto.getRegulacaoMedica()
        );
    }

    protected TarmEntity toTarmEntity(TarmEntityDto dto) {
        return new TarmEntity(
                dto.getNome(),
                dto.getCpf(),
                dto.getIdCallRote(),
                dto.getPontuacao(),
                dto.getRole(),
                dto.getTempoRegulaco()
        );
    }

    protected CollaboratorsResponse toCollaboratorsResponse(CollaboratorEntity entity) {
        return new CollaboratorsResponse(
                entity.getNome(),
                entity.getCpf(),
                entity.getIdCallRote(),
                entity.getPontuacao(),
                entity.getRole()
        );
    }

    public CollaboratorEntity buscarPorId(Long id, String tipo) {
        return switch (tipo.toUpperCase()) {
            case "FROTA" -> frotaRepository.findById(id).orElseThrow();
            case "TARM" -> tarmRepository.findById(id).orElseThrow();
            default -> throw new IllegalArgumentException("Tipo inválido");
        };
    }


}
