package com.avaliadados.repository;


import com.avaliadados.model.MedicoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicoRepository extends JpaRepository<MedicoEntity, String> {

}
