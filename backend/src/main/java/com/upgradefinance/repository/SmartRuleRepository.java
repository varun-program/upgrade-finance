package com.upgradefinance.repository;

import com.upgradefinance.model.SmartRule;
import com.upgradefinance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SmartRuleRepository extends JpaRepository<SmartRule, String> {
    List<SmartRule> findByUserAndIsDeletedFalse(User user);
    List<SmartRule> findByUserAndUpdatedAtGreaterThan(User user, long timestamp);
}
