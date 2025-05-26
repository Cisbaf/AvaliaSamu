package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.dto.CollaboratorRequest;
import com.avaliadados.model.dto.CollaboratorsResponse;
import com.avaliadados.model.MedicoEntity;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
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
    private final ProjetoRepository projetoRepository;


    @Transactional
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
        return collaboratorRepository.findByNomeApproximate(nome);
    }

    public void deleteById(String id) {
        collaboratorRepository.findById(id)
                .ifPresentOrElse(
                        entity -> {
                            collaboratorRepository.delete(entity);
                            collaboratorRepository.flush();
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

        if (existing instanceof MedicoEntity medicoEntity) {
            updateMedicoFields(medicoEntity, request);
        }

        updateCommonFields(existing, request);
        var updated = collaboratorRepository.save(existing);
        syncIds(existing.getId(), updated.getId());
        return mapper.toCollaboratorsResponse(updated);
    }

    private void updateMedicoFields(MedicoEntity entity, CollaboratorRequest request) {
        entity.setMedicoRole(request.medicoRole());
        entity.setShiftHours(request.shiftHours());
    }

    private void updateCommonFields(CollaboratorEntity entity, CollaboratorRequest request) {
        entity.setNome(request.nome());
        entity.setCpf(request.cpf());
        entity.setIdCallRote(request.idCallRote());
        entity.setPontuacao(request.pontuacao());
    }

    private CollaboratorsResponse handleRoleChange(CollaboratorEntity oldEntity, CollaboratorRequest request) {
        CollaboratorEntity newEntity = mapper.createByRole(request);
        copyCommonFields(oldEntity, newEntity);

        collaboratorRepository.delete(oldEntity);
        CollaboratorEntity saved = collaboratorRepository.save(newEntity);

        var updated = mapper.toCollaboratorsResponse(saved);

        syncIds(oldEntity.getId(), updated.getId());
        return updated;
    }

    private void copyCommonFields(CollaboratorEntity source, CollaboratorEntity target) {
        target.setNome(source.getNome());
        target.setCpf(source.getCpf());
        target.setIdCallRote(source.getIdCallRote());
        target.setPontuacao(source.getPontuacao());
    }

    public void syncIds(String oldId, String newId) {
        log.info("Atualizando ID do colaborador de [{}] para [{}]", oldId, newId);

        List<ProjetoEntity> projetos = projetoRepository.findByCollaboratorsCollaboratorId(oldId);
        if (projetos.isEmpty()) {
            log.warn("Nenhum projeto encontrado para o colaborador com ID [{}]", oldId);
            return;
        }
        projetos.forEach(projeto -> {
            projeto.getCollaborators().forEach(pc -> {
                if (pc.getCollaboratorId().equals(oldId)) {
                    pc.setCollaboratorId(newId);
                }
            });
            projetoRepository.save(projeto);
        });

        log.info("ID do colaborador atualizado com sucesso em todos os projetos.");
    }

}