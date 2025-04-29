package com.avaliadados.repository;

import com.avaliadados.model.ProjetoEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProjetoRepository extends MongoRepository<ProjetoEntity, String> {
}
