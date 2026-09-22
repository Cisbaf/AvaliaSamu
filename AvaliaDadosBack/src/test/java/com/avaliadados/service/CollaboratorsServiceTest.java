//package com.avaliadados.service;
//
//import com.avaliadados.model.CollaboratorEntity;
//import com.avaliadados.model.ProjectCollaborator;
//import com.avaliadados.model.ProjetoEntity;
//import com.avaliadados.model.dto.CollaboratorRequest;
//import com.avaliadados.model.dto.CollaboratorsResponse;
//import com.avaliadados.repository.CollaboratorRepository;
//import com.avaliadados.repository.MedicoRepository;
//import com.avaliadados.repository.ProjetoRepository;
//import com.avaliadados.service.utils.CollaboratorsMapper;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class CollaboratorsServiceTest {
//
//    @Mock
//    private CollaboratorRepository collaboratorRepo;
//
//    @Mock
//    private MedicoRepository medicoRepo;
//
//    @Mock
//    private CollaboratorsMapper mapper;
//
//    @Mock
//    private ProjetoRepository projetoRepository;
//
//    @InjectMocks
//    private CollaboratorsService service;
//
//    @Test
//    void deleteByIdShouldRemoveCollaboratorReferencesFromProjects() {
//        CollaboratorEntity collaborator = new CollaboratorEntity();
//        collaborator.setId("collab-1");
//        collaborator.setNome("Ana");
//
//        ProjetoEntity projeto = new ProjetoEntity();
//        projeto.setId("project-1");
//        projeto.setCollaborators(List.of(ProjectCollaborator.builder().collaboratorId("collab-1").nome("Ana").build()));
//
//        when(collaboratorRepo.findById("collab-1")).thenReturn(Optional.of(collaborator));
//        when(projetoRepository.findByCollaboratorsCollaboratorId("collab-1")).thenReturn(List.of(projeto));
//
//        service.deleteById("collab-1");
//
//        assertEquals(0, projeto.getCollaborators().size());
//        verify(projetoRepository).save(projeto);
//    }
//
//    @Test
//    void updateCollaboratorShouldSyncProjectCollaboratorData() {
//        CollaboratorEntity existing = new CollaboratorEntity();
//        existing.setId("old-id");
//        existing.setNome("Antigo");
//        existing.setCpf("11111111111");
//        existing.setIdCallRote("12345678901");
//        existing.setPontuacao(10);
//        existing.setRole("TARM");
//
//        CollaboratorEntity updated = new CollaboratorEntity();
//        updated.setId("new-id");
//        updated.setNome("Novo");
//        updated.setCpf("22222222222");
//        updated.setIdCallRote("10987654321");
//        updated.setPontuacao(20);
//        updated.setRole("FROTA");
//
//        ProjetoEntity projeto = new ProjetoEntity();
//        projeto.setId("project-1");
//        ProjectCollaborator projectCollaborator = ProjectCollaborator.builder()
//                .collaboratorId("old-id")
//                .nome("Antigo")
//                .role("TARM")
//                .idCallRote("12345678901")
//                .build();
//        projeto.setCollaborators(List.of(projectCollaborator));
//
//        CollaboratorRequest request = new CollaboratorRequest(
//                null,
//                "Novo",
//                "22222222222",
//                "10987654321",
//                20,
//                "FROTA",
//                null,
//                null
//        );
//
//        when(collaboratorRepo.findById("old-id")).thenReturn(Optional.of(existing));
//        when(mapper.createByRole(request)).thenReturn(updated);
//        when(collaboratorRepo.save(any(CollaboratorEntity.class))).thenReturn(updated);
//        when(projetoRepository.findByCollaboratorsCollaboratorId("old-id")).thenReturn(List.of(projeto));
//        when(projetoRepository.save(any(ProjetoEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
//        when(mapper.toCollaboratorsResponse(updated)).thenReturn(CollaboratorsResponse.builder().id("new-id").nome("Novo").build());
//
//        service.updateCollaborator(request, "old-id");
//
//        assertEquals("new-id", projectCollaborator.getCollaboratorId());
//        assertEquals("Novo", projectCollaborator.getNome());
//        assertEquals("FROTA", projectCollaborator.getRole());
//        assertEquals("10987654321", projectCollaborator.getIdCallRote());
//        assertEquals("FROTA", existing.getRole());
//    }
//}
