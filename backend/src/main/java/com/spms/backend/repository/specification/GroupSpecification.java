package com.spms.backend.repository.specification;

import com.spms.backend.model.Group;
import com.spms.backend.model.GroupStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class GroupSpecification {

    public static Specification<Group> filterGroups(String status, String advisorAssigned, String groupName, String role, Long requesterId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(groupName)) {
                predicates.add(cb.like(cb.lower(root.get("groupName")), "%" + groupName.toLowerCase() + "%"));
            }

            if (StringUtils.hasText(status) && !"all".equalsIgnoreCase(status)) {
                try {
                    predicates.add(cb.equal(root.get("status"), GroupStatus.valueOf(status.toUpperCase())));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid status
                }
            }

            if (StringUtils.hasText(advisorAssigned) && !"all".equalsIgnoreCase(advisorAssigned)) {
                if ("has_advisor".equalsIgnoreCase(advisorAssigned)) {
                    predicates.add(cb.isNotNull(root.get("advisor")));
                } else if ("no_advisor".equalsIgnoreCase(advisorAssigned)) {
                    predicates.add(cb.isNull(root.get("advisor")));
                }
            }

            if ("professor".equalsIgnoreCase(role)) {
                predicates.add(cb.equal(root.get("advisor").get("userId"), requesterId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
