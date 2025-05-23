package com.avaliadados.repository;


import com.avaliadados.model.CollaboratorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollaboratorRepository extends JpaRepository<CollaboratorEntity, String> {

    @Query("SELECT c FROM CollaboratorEntity c WHERE UPPER(c.nome) LIKE UPPER(CONCAT('%', :nome, '%'))")
    List<CollaboratorEntity> findByNomeApproximate(@Param("nome") String nome);

    Optional<CollaboratorEntity> findByNome(String name);
}
