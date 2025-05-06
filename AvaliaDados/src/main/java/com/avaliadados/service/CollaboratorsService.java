package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorRequest;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.FrotaEntity;
import com.avaliadados.model.TarmEntity;
import com.avaliadados.repository.CollaboratorRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollaboratorsService {

    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorsMapper mapper;
    private final ProjectCollabService projetosService;


    public CollaboratorsResponse createCollaborator(CollaboratorRequest request) {
        CollaboratorEntity newCollaborator = mapper.createByRole(request);
        CollaboratorEntity saved = collaboratorRepository.save(newCollaborator);
        return mapper.toCollaboratorsResponse(saved);
    }


    public CollaboratorsResponse findById(String id) {
        var collaborator = collaboratorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Colaborador não encontrado com ID: " + id));
        return mapper.toCollaboratorsResponse(collaborator);
    }


    public List<CollaboratorEntity> findAll() {
        return collaboratorRepository.findAll();
    }

    public List<CollaboratorEntity> findByName(String nome) {
        return collaboratorRepository.findByNomeApproximate(nome); // Supondo que este método exista
    }

    public void deleteById(String id) {
        collaboratorRepository.findById(id)
                .ifPresentOrElse(
                        entity -> {
                            collaboratorRepository.delete(entity);
                            collaboratorRepository.flush(); // Para garantir a deleção imediata se necessário
                            log.info("Colaborador com ID {} deletado.", id);
                        },
                        () -> {
                            log.warn("Tentativa de deletar colaborador com ID {}, mas não foi encontrado.", id);
                            throw new EntityNotFoundException("Colaborador não encontrado para deleção com ID: " + id);
                        }
                );
    }

    @Transactional
    public CollaboratorsResponse updateCollaborator(CollaboratorRequest request, String id) {
        CollaboratorEntity existing = collaboratorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Colaborador não encontrado"));

        if (!existing.getRole().equalsIgnoreCase(request.role())) {
            return handleRoleChange(existing, request);
        }

        updateExistingFields(existing, request);
        var newCollab = collaboratorRepository.save(existing);
        projetosService.syncCollaboratorData(newCollab.getId());

        return mapper.toCollaboratorsResponse(newCollab);
    }

    private CollaboratorsResponse handleRoleChange(CollaboratorEntity oldEntity, CollaboratorRequest request) {
        CollaboratorEntity newEntity = mapper.createByRole(request);
        copyCommonFields(oldEntity, newEntity);
        copySpecificFields(oldEntity, newEntity);

        collaboratorRepository.delete(oldEntity);
        CollaboratorEntity saved = collaboratorRepository.save(newEntity);

        return mapper.toCollaboratorsResponse(saved);
    }

    private void copyCommonFields(CollaboratorEntity source, CollaboratorEntity target) {
        target.setNome(source.getNome());
        target.setCpf(source.getCpf());
        target.setIdCallRote(source.getIdCallRote());
        target.setPontuacao(source.getPontuacao());
    }

    private void copySpecificFields(CollaboratorEntity oldEntity, CollaboratorEntity newEntity) {
        if (oldEntity instanceof TarmEntity oldTarm && newEntity instanceof TarmEntity newTarm) {
            newTarm.setTempoRegulaco(oldTarm.getTempoRegulaco());
        }
        else if (oldEntity instanceof FrotaEntity oldFrota && newEntity instanceof FrotaEntity newFrota) {
            newFrota.setRegulacaoMedica(oldFrota.getRegulacaoMedica());
        }
    }

    private void updateExistingFields(CollaboratorEntity entity, CollaboratorRequest request) {
        entity.setNome(request.nome());
        entity.setCpf(request.cpf());
        entity.setIdCallRote(request.idCallRote());
        entity.setPontuacao(request.pontuacao());

        if (entity instanceof TarmEntity tarm) {
            tarm.setTempoRegulaco(request.tempoRegulaco());
        }
        else if (entity instanceof FrotaEntity frota) {
            frota.setRegulacaoMedica(request.regulacaoMedica());
        }
    }

}