package com.avaliadados.repository;

import com.avaliadados.model.FrotaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FrotaRepository extends JpaRepository<FrotaEntity, Long> {
    Optional<FrotaEntity> findByNome(String nome);
}
