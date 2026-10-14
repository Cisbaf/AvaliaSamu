package com.avaliadados.repository;


import com.avaliadados.model.roles.MedicoEntity;
import com.avaliadados.model.enums.MedicoRole;
import com.avaliadados.model.enums.WorkPeriod;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicoRepository extends JpaRepository<MedicoEntity, String> {
    boolean existsByNomeAndMedicoRole(@NotBlank(message = "Campo nome é obrigatorio") String nome, MedicoRole medicoRole);

    boolean existsByNomeAndMedicoRoleAndWorkPeriod(String nome, MedicoRole medicoRole, WorkPeriod workPeriod);

}
