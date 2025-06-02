package com.avaliadados.repository;


import com.avaliadados.model.MedicoEntity;
import com.avaliadados.model.enums.MedicoRole;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicoRepository extends JpaRepository<MedicoEntity, String> {
    boolean existsByNomeAndMedicoRole(@NotBlank(message = "Campo nome é obrigatorio") String nome, MedicoRole medicoRole);

}
