package com.spms.backend.repository;

import com.spms.backend.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findByStatus(String status);

    List<Group> findByAdvisorIdIsNullAndStatusNot(String status);
}
