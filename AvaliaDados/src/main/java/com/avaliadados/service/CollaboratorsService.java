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
import lombok.extern.slf4j.Slf4j;
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

    public Object updateColaborador(CollaboratorRequest entity, long id) {
        var updated = collaboratorRepository.findById(id);
        if (updated.isPresent()) {
            updated.get().setNome(entity.nome());
            updated.get().setCpf(entity.cpf());
            updated.get().setRole(entity.role());
            updated.get().setIdCallRote(entity.idCallRote());
            updated.get().setPontuacao(entity.pontuacao());
            log.info(updated.toString());
            return collaboratorRepository.save(updated.get());
        }
        throw new NullPointerException("Colaborador não existe no banco || Favor entrar em contato com a equipe de TI");
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

}
