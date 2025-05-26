package com.avaliadados;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.dto.CollaboratorRequest;
import com.avaliadados.model.dto.CollaboratorsResponse;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.service.CollaboratorsMapper;
import com.avaliadados.service.CollaboratorsService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaboratorsServiceTest {

    @Mock
    private CollaboratorRepository collaboratorRepository;
    @Mock
    private CollaboratorsMapper mapper;
    @Mock
    private ProjetoRepository projetoRepository;
    @InjectMocks
    private CollaboratorsService service;

    private CollaboratorRequest request;
    private CollaboratorEntity entity;
    private CollaboratorsResponse response;

    @BeforeEach
    void setUp() {
        request = new CollaboratorRequest("id", "name", "role", "cpf", 0, "call", null, null);
        entity = new CollaboratorEntity();
        entity.setId("id");
        entity.setNome("name");
        response = new CollaboratorsResponse("id", "name", "cpf", "call", 0, "role", null, null, null, 0L, 0L, 0);
    }

    @Test
    void createCollaborator_success() {
        when(mapper.createByRole(request)).thenReturn(entity);
        when(collaboratorRepository.save(entity)).thenReturn(entity);
        when(mapper.toCollaboratorsResponse(entity)).thenReturn(response);

        CollaboratorsResponse result = service.createCollaborator(request);

        assertThat(result).isEqualTo(response);
        verify(collaboratorRepository).save(entity);
    }

    @Test
    void findById_notFound_throws() {
        when(collaboratorRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById("missing"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Colaborador não encontrado");
    }

    @Test
    void findById_success() {
        when(collaboratorRepository.findById("id")).thenReturn(Optional.of(entity));
        when(mapper.toCollaboratorsResponse(entity)).thenReturn(response);

        CollaboratorsResponse result = service.findById("id");
        assertThat(result).isEqualTo(response);
    }

    @Test
    void findAll_returnsList() {
        List<CollaboratorEntity> list = Collections.singletonList(entity);
        when(collaboratorRepository.findAll()).thenReturn(list);

        List<CollaboratorEntity> result = service.findAll();
        assertThat(result).isEqualTo(list);
    }

    @Test
    void findByName_callsRepository() {
        String nome = "test";
        List<CollaboratorEntity> list = Collections.singletonList(entity);
        when(collaboratorRepository.findByNomeApproximate(nome)).thenReturn(list);

        List<CollaboratorEntity> result = service.findByName(nome);
        assertThat(result).isEqualTo(list);
    }

    @Test
    void deleteById_existing_deletes() {
        when(collaboratorRepository.findById("id")).thenReturn(Optional.of(entity));
        service.deleteById("id");
        verify(collaboratorRepository).deleteById("id");
    }

    @Test
    void syncIds_updatesAcrossProjects() {
        String oldId = "old";
        String newId = "new";
        ProjetoEntity projeto = new ProjetoEntity();
        ProjectCollaborator pc = new ProjectCollaborator();
        pc.setCollaboratorId(oldId);
        projeto.setCollaborators(Collections.singletonList(pc));
        when(projetoRepository.findAll()).thenReturn(Collections.singletonList(projeto));

        service.syncIds(oldId, newId);
        assertThat(projeto.getCollaborators().getFirst().getCollaboratorId()).isEqualTo(newId);
        verify(projetoRepository).save(projeto);
    }
}