package com.avaliadados.repository;

import com.avaliadados.model.roles.MedicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicoEntityRepository extends JpaRepository<MedicoEntity, String> {
}
