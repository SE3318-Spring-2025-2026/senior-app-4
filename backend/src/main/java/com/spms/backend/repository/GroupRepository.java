package com.spms.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.spms.backend.model.Group;
@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
}