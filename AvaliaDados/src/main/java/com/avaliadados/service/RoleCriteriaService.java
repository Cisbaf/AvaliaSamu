package com.avaliadados.service;

import com.avaliadados.model.CriterionValue;
import com.avaliadados.model.RoleCriteriaEntity;
import com.avaliadados.repository.RoleCriteriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleCriteriaService {
    private final RoleCriteriaRepository repo;

    public RoleCriteriaService(RoleCriteriaRepository repo) {
        this.repo = repo;
    }

    /**
     * Busca os critérios configurados para esse role e tipo
     */
    public RoleCriteriaEntity getCriteria(String role, String type) {
        return repo.findByRoleAndCriterionType(role, type);
    }

    /**
     * Cria ou atualiza os critérios para esse role+tipo
     */
    @Transactional
    public RoleCriteriaEntity updateCriteria(String role, String type, List<CriterionValue> values) {
        RoleCriteriaEntity entity = repo.findByRoleAndCriterionType(role, type);
        if (entity == null) {
            entity = RoleCriteriaEntity.builder()
                    .role(role).criterionType(type)
                    .values(values)
                    .build();
        } else {
            entity.getValues().clear();
            entity.getValues().addAll(values);
        }
        return repo.save(entity);
    }
}