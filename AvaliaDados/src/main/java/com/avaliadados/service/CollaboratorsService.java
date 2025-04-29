package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorRequest;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.TarmEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.FrotaRepository;
import com.avaliadados.repository.TarmRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
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

    @Transactional
    public CollaboratorsResponse updateCollaborator(CollaboratorRequest request, Long id) {
        // Busca a entidade existente
        CollaboratorEntity existing = collaboratorRepository.findById(id)
                .orElseThrow();

        // Se o role não mudou
        if (existing.getRole().equals(request.role())) {
            updateExistingEntity(existing, request);
            return mapper.toCollaboratorsResponse(collaboratorRepository.save(existing));
        }

        // Cria nova entidade com novo ID
        CollaboratorEntity newEntity = mapper.createNewEntityByRole(request);

        // Copia campos comuns (exceto ID e campos versionados)
        copyCommonFields(existing, newEntity);

        // Salva e remove em operações separadas
        collaboratorRepository.delete(existing);
        collaboratorRepository.flush(); // Força a sincronização com o banco

        CollaboratorEntity savedEntity = collaboratorRepository.save(newEntity);

        return mapper.toCollaboratorsResponse(savedEntity);
    }
    private void copyCommonFields(CollaboratorEntity source, CollaboratorEntity target) {
        target.setNome(source.getNome());
        target.setCpf(source.getCpf());
        target.setIdCallRote(source.getIdCallRote());
        target.setPontuacao(source.getPontuacao());
        // Não copiar ID ou campos de versionamento
    }




    public CollaboratorsResponse findByid(Long id) {
        var collaborator = mapper.buscarPorId(id);
        return mapper.toCollaboratorsResponse(collaborator);
    }

    public List<CollaboratorEntity> findAll() {
        return collaboratorRepository.findAll();
    }

    public List<CollaboratorEntity> findByName(String nome) {
        return collaboratorRepository.findByNomeApproximate(nome);
    }


    private void updateExistingEntity(CollaboratorEntity entity, CollaboratorRequest request) {
        entity.setNome(request.nome());
        entity.setCpf(request.cpf());
        entity.setIdCallRote(request.idCallRote());
        entity.setPontuacao(request.pontuacao());

        if (entity instanceof TarmEntity tarm) {
            tarm.setTempoRegulaco(request.tempoRegulaco());
        }
        if (entity instanceof FrotaEntity frota) {
            frota.setRegulacaoMedica(request.regulacaoMedica());
        }
    }

}
