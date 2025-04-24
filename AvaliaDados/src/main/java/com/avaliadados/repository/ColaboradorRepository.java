package com.avaliadados.repository;


import com.avaliadados.model.ColaboradorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ColaboradorRepository  extends JpaRepository<ColaboradorEntity, Long> {
    boolean existsByNome(String nome);

    @Query("SELECT c FROM ColaboradorEntity c WHERE UPPER(c.nome) LIKE UPPER(CONCAT('%', :nome, '%'))")
    List<ColaboradorEntity> findByNomeApproximate(@Param("nome") String nome);
}
