package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.DTO.CollaboratorRequest;
import com.avaliadados.model.DTO.CollaboratorsResponse;
import com.avaliadados.model.MedicoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.ShiftHours;
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

        // Atualiza campos específicos de médico
        if (existing instanceof MedicoEntity medicoEntity) {
            updateMedicoFields(medicoEntity, request);
        }

        updateCommonFields(existing, request);
        return mapper.toCollaboratorsResponse(collaboratorRepository.save(existing));
    }

    private void updateMedicoFields(MedicoEntity entity, CollaboratorRequest request) {
        String[] parts = request.role().split("_");
        entity.setMedicoRole(MedicoRole.valueOf(parts[1]));
        entity.setShiftHours(ShiftHours.valueOf(parts[2]));
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

        return mapper.toCollaboratorsResponse(saved);
    }

    private void copyCommonFields(CollaboratorEntity source, CollaboratorEntity target) {
        target.setNome(source.getNome());
        target.setCpf(source.getCpf());
        target.setIdCallRote(source.getIdCallRote());
        target.setPontuacao(source.getPontuacao());
    }

}