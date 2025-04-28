package com.avaliadados.repository;

import com.avaliadados.model.TarmEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TarmRepository extends JpaRepository<TarmEntity, Long> {
    Optional<TarmEntity> findByNome(String nome);
}
