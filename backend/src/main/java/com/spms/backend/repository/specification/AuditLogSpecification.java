package com.spms.backend.repository.specification;

import com.spms.backend.model.ActionType;
import com.spms.backend.model.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    public static Specification<AuditLog> filterBy(ActionType action, String entityType, Instant startDate, Instant endDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null) {
                predicates.add(criteriaBuilder.equal(root.get("actionType"), action));
            }

            if (entityType != null && !entityType.trim().isEmpty()) {
                if ("GROUP".equalsIgnoreCase(entityType)) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("groupId")));
                } else if ("COMMITTEE".equalsIgnoreCase(entityType)) {
                    predicates.add(criteriaBuilder.isNotNull(root.get("committeeId")));
                }
            }

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }

            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
