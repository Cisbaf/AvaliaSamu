package com.avaliadados;

import com.avaliadados.model.ProjetoEntity;
import com.avaliadados.repository.ProjetoRepository;
import com.avaliadados.service.ProjetosService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjetosServiceTest {

    @Mock
    private ProjetoRepository projetoRepo;
    @Mock

    @InjectMocks
    private ProjetosService service;

    @Test
    void createProjetoWithCollaborators_success() {
        ProjetoEntity projeto = new ProjetoEntity();
        projeto.setId("p1");
        when(projetoRepo.save(projeto)).thenReturn(projeto);

        ProjetoEntity result = service.createProjetoWithCollaborators(projeto);
        assertThat(result).isEqualTo(projeto);
    }

    @Test
    void updateProjeto_success() {
        ProjetoEntity projeto = new ProjetoEntity();
        projeto.setId("p1");
        Map<String, Object> updates = Map.of("name", "newName");
        when(projetoRepo.findById("p1")).thenReturn(Optional.of(projeto));
        when(projetoRepo.save(projeto)).thenReturn(projeto);

        ProjetoEntity result = service.updateProjeto("p1", updates);
        assertThat(result).isEqualTo(projeto);
    }

    @Test
    void getAllProjeto_returnsList() {
        List<ProjetoEntity> list = Collections.singletonList(new ProjetoEntity());
        when(projetoRepo.findAll()).thenReturn(list);

        List<ProjetoEntity> result = service.getAllProjeto();
        assertThat(result).isEqualTo(list);
    }

    @Test
    void deleteProject_invokesRepositoryDelete() {
        service.deleteProject("p123");
        verify(projetoRepo).deleteById("p123");
    }

    @Test
    void updateProjeto_notFound_throws() {
        when(projetoRepo.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateProjeto("missing", Map.of()))
                .isInstanceOf(EntityNotFoundException.class);
    }


}
