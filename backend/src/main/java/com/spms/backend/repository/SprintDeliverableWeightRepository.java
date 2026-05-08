package com.spms.backend.repository;

import com.spms.backend.model.SprintDeliverableWeight;
import com.spms.backend.model.enums.DeliverableType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SprintDeliverableWeightRepository extends JpaRepository<SprintDeliverableWeight, Long> {
    List<SprintDeliverableWeight> findAllByOrderByDeliverableTypeAscSprint_IdAsc();
    List<SprintDeliverableWeight> findByDeliverableType(DeliverableType deliverableType);
}
