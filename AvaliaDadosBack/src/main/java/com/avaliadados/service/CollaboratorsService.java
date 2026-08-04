package com.avaliadados.service;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.dto.CollaboratorRequest;
import com.avaliadados.model.dto.CollaboratorsResponse;
import com.avaliadados.model.roles.MedicoEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.MedicoRepository;
import com.avaliadados.service.utils.CollaboratorsMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollaboratorsService {

    private final CollaboratorRepository collaboratorRepo;
    private final MedicoRepository medicoRepo;
    private final CollaboratorsMapper mapper;


    @Transactional
    public CollaboratorsResponse createCollaborator(CollaboratorRequest request) {

        if (request.medicoRole() == null) {
            if (collaboratorRepo.existsByNome(request.nome().trim())) {
                throw new IllegalArgumentException("Colaborador com nome já existente: " + request.nome());
            }
            if (collaboratorRepo.existsByCpf((request.cpf()))) {
                throw new IllegalArgumentException("Colaborador com CPF: " + request.cpf());
            }
            if (collaboratorRepo.existsByIdCallRote(request.idCallRote())) {
                throw new IllegalArgumentException("Colaborador com ID de Call Rote já existente: " + request.idCallRote());
            }
        } else if (medicoRepo.existsByNomeAndMedicoRole(request.nome(), request.medicoRole())) {
            throw new IllegalArgumentException("Colaborador médico com nome e função já existente: " + request.nome() + " - " + request.medicoRole());
        }


        CollaboratorEntity newCollaborator = mapper.createByRole(request);
        CollaboratorEntity saved = collaboratorRepo.save(newCollaborator);
        return mapper.toCollaboratorsResponse(saved);
    }


    public CollaboratorsResponse findById(String id) {
        var collaborator = collaboratorRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Colaborador não encontrado com ID: " + id));
        return mapper.toCollaboratorsResponse(collaborator);
    }


    public List<CollaboratorEntity> findAll() {
        return collaboratorRepo.findAll();
    }

    public List<CollaboratorEntity> findByName(String nome) {
        return collaboratorRepo.findByNomeApproximate(nome);
    }

    @Transactional
    public void deleteById(String id) {
        collaboratorRepo.findById(id)
                .ifPresentOrElse(
                        collaboratorRepo::delete,
                        () -> {
                            throw new EntityNotFoundException("Colaborador não encontrado para deleção com ID: " + id);
                        }
                );
    }

    @Transactional
    public CollaboratorsResponse updateCollaborator(CollaboratorRequest request, String id) {
        CollaboratorEntity existing = collaboratorRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Colaborador não encontrado"));
        if (!existing.getRole().equalsIgnoreCase(request.role())) {
            return handleRoleChange(existing, request);
        }

        if (existing instanceof MedicoEntity medicoEntity) {
            updateMedicoFields(medicoEntity, request);
        }

        updateCommonFields(existing, request);
        var updated = collaboratorRepo.save(existing);
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
        entity.setRole(request.role());
    }

    private CollaboratorsResponse handleRoleChange(CollaboratorEntity oldEntity, CollaboratorRequest request) {
        CollaboratorEntity newEntity = mapper.createByRole(request);
        copyCommonFields(oldEntity, newEntity);

        collaboratorRepo.delete(oldEntity);
        CollaboratorEntity saved = collaboratorRepo.save(newEntity);

        return mapper.toCollaboratorsResponse(saved);
    }

    private void copyCommonFields(CollaboratorEntity source, CollaboratorEntity target) {
        target.setNome(source.getNome());
        target.setCpf(source.getCpf());
        target.setIdCallRote(source.getIdCallRote());
        target.setPontuacao(source.getPontuacao());
    }


}