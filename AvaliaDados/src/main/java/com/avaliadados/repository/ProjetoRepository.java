package com.avaliadados.repository;

import com.avaliadados.model.ProjetoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProjetoRepository extends MongoRepository<ProjetoEntity, String> {
    List<ProjetoEntity> findByCollaboratorsCollaboratorId(String collaboratorId);
}
