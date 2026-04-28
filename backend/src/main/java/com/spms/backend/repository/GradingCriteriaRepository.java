package com.spms.backend.repository;

import com.spms.backend.model.enums.DeliverableType;
import com.spms.backend.model.GradingCriteria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradingCriteriaRepository extends JpaRepository<GradingCriteria, Long> {

    List<GradingCriteria> findByDeliverableType(DeliverableType deliverableType);
}
