package com.avaliadados.service;

import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.DTO.FrotaEntityDto;
import com.avaliadados.model.DTO.TarmEntityDto;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.TarmEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.FrotaRepository;
import com.avaliadados.repository.TarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollaboratorsService {
    private final TarmRepository tarmRepository;
    private final FrotaRepository frotaRepository;
    private final CollaboratorsMapper mapper;


    public TarmEntity createTarm(TarmEntityDto tarm) {
        return tarmRepository.save(mapper.toTarmEntity(tarm));
    }

    public FrotaEntity createFrota(FrotaEntityDto frota) {
        return frotaRepository.save(mapper.toFrotaEntity(frota));
    }

    public CollaboratorsResponse findByid(Long id, String role) {
        var collaborator = mapper.buscarPorId(id, role);
        return mapper.toCollaboratorsResponse(collaborator);
    }

}
