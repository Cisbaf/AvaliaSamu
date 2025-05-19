package com.avaliadados.repository;

import com.avaliadados.model.SheetRow;
import com.avaliadados.model.enums.TypeAv;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SheetRowRepository extends MongoRepository<SheetRow, String> {
    List<SheetRow> findByProjectId(String projectId);

    void deleteByProjectIdAndType(String projectId, TypeAv type);

    List<SheetRow> findByProjectIdAndType(String projectId, TypeAv typeAv);
}