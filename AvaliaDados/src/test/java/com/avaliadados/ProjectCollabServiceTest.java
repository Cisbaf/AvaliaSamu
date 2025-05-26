package com.avaliadados;

import com.avaliadados.model.CollaboratorEntity;
import com.avaliadados.model.dto.CollaboratorsResponse;
import com.avaliadados.model.dto.ProjectCollabRequest;
import com.avaliadados.model.ProjectCollaborator;
import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.repository.CollaboratorRepository;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.repository.SheetRowRepository;
import com.avaliadados.service.ProjectCollabService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectCollabServiceTest {

    @Mock
    private ProjetoRepository projetoRepo;
    @Mock
    private CollaboratorRepository collaboratorRepo;
    @Mock
    private SheetRowRepository rowRepository;
    @InjectMocks
    private ProjectCollabService service;

    private ProjetoEntity projeto;
    private CollaboratorEntity collaborator;
    private ProjectCollabRequest dto;

    @BeforeEach
    void init() {
        projeto = new ProjetoEntity(); projeto.setId("p1"); projeto.setCollaborators(new ArrayList<>());
        collaborator = new CollaboratorEntity(); collaborator.setId("c1"); collaborator.setNome("Test");
        dto = new ProjectCollabRequest();
        dto.setCollaboratorId("c1"); dto.setRole("ROLE"); dto.setDurationSeconds(100L);
        dto.setQuantity(2); dto.setPausaMensalSeconds(5L);
    }

    @Test
    void addCollaborator_success() {
        when(projetoRepo.findById("p1")).thenReturn(Optional.of(projeto));
        when(collaboratorRepo.findById("c1")).thenReturn(Optional.of(collaborator));
        when(rowRepository.findByCollaboratorIdAndProjectId("c1", "p1")).thenReturn(null);
        when(rowRepository.findByProjectId("p1")).thenReturn(Collections.emptyList());
        when(projetoRepo.save(projeto)).thenReturn(projeto);

        ProjetoEntity result = service.addCollaborator("p1", dto);
        assertThat(result.getCollaborators()).hasSize(1);
    }

    @Test
    void addCollaborator_projectNotFound_exception() {
        when(projetoRepo.findById("pX")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addCollaborator("pX", dto))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getAllProjectCollaborators_returnsResponses() {
        ProjectCollaborator pc = new ProjectCollaborator();
        pc.setCollaboratorId("c1"); pc.setRole(MedicoRole.REGULADOR.name());
        projeto.setCollaborators(Collections.singletonList(pc));
        when(projetoRepo.findById("p1")).thenReturn(Optional.of(projeto));
        when(collaboratorRepo.findById("c1")).thenReturn(Optional.of(collaborator));

        List<CollaboratorsResponse> responses = service.getAllProjectCollaborators("p1");
        assertThat(responses).isNotNull();
    }

    @Test
    void removeCollaborator_success() {
        ProjectCollaborator pc = new ProjectCollaborator(); pc.setCollaboratorId("c1");
        projeto.setCollaborators(new ArrayList<>(Collections.singleton(pc)));
        when(projetoRepo.findById("p1")).thenReturn(Optional.of(projeto));

        service.removeCollaborator("p1", "c1");
        assertThat(projeto.getCollaborators()).isEmpty();
        verify(projetoRepo).save(projeto);
    }

    @Test
    void syncCollaboratorData_callsSaveAll() {
        when(projetoRepo.findByCollaboratorsCollaboratorId("c1")).thenReturn(Collections.singletonList(projeto));
        service.syncCollaboratorData("c1");
        verify(projetoRepo).saveAll(Collections.singletonList(projeto));
    }
}

