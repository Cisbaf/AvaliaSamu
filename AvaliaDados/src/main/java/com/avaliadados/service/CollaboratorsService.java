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

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CollaboratorsService {
    private final TarmRepository tarmRepository;
    private final FrotaRepository frotaRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorsMapper mapper;


    public TarmEntity createTarm(CollaboratorRequest tarm) {

        return tarmRepository.save(mapper.toTarmEntity(tarm));
    }

    public FrotaEntity createFrota(CollaboratorRequest frota) {
        return frotaRepository.save(mapper.toFrotaEntity(frota));
    }

    public CollaboratorsResponse findByid(Long id) {
        var collaborator = mapper.buscarPorId(id);
        return mapper.toCollaboratorsResponse(collaborator);
    }

    public List<String> findAll() {
        List<String> allList = new ArrayList<>();

        List<TarmEntity> tarmList = tarmRepository.findAll();
        for (TarmEntity tarm : tarmList) {
            allList.add("Tarm nome: " + tarm.getNome() + ", tempo: " + tarm.getTempoRegulaco());
        }
        List<FrotaEntity> frotaList = frotaRepository.findAll();
        for (FrotaEntity fnota : frotaList) {
            allList.add("Frota nome: " + fnota.getNome() + ", tempo: " + fnota.getRegulacaoMedica());
        }

        return allList;
    }

    public List<CollaboratorEntity> findByName(String nome) {
        return collaboratorRepository.findByNomeApproximate(nome);
    }

}
