package com.avaliadados.repository;

import com.avaliadados.model.RoleCriteriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleCriteriaRepository extends JpaRepository<RoleCriteriaEntity, String> {
    RoleCriteriaEntity findByRoleAndCriterionType(String role, String criterionType);
}