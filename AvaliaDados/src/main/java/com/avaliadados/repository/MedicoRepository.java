package com.avaliadados.repository;


import com.avaliadados.model.MedicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicoRepository extends JpaRepository<MedicoEntity, String> {

    Optional<MedicoEntity> findByNome(String name);
}
