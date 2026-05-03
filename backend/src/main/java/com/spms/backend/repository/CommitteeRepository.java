package com.spms.backend.repository;

import com.spms.backend.model.Committee;
import com.spms.backend.model.enums.CommitteeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
 import org.springframework.data.repository.query.Param;

public interface CommitteeRepository extends JpaRepository<Committee, Long> {
    Optional<Committee> findByCommitteeId(Long committeeId);

    Page<Committee> findByStatus(CommitteeStatus status, Pageable pageable);

    List<Committee> findByStatus(CommitteeStatus status); // For testing

    Page<Committee> findByCreatedBy(Long coordinatorId, Pageable pageable);

    List<Committee> findByCreatedBy(Long coordinatorId); // For testing

    @Query("SELECT c FROM Committee c WHERE c.committeeId = :committeeId")
    Optional<Committee> findWithFullDetails(@Param("committeeId") Long committeeId);

    @Query("SELECT c FROM Committee c")
    Page<Committee> findAllWithDetails(Pageable pageable);

    @Query("SELECT c FROM Committee c WHERE c.status = :status")
    Page<Committee> findByStatusWithDetails(@Param("status") CommitteeStatus status, Pageable pageable);
    @Query("SELECT c FROM Committee c WHERE " +
       "(:status IS NULL OR c.status = :status) AND " +
       "(:search IS NULL OR LOWER(c.committeeName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
       "ORDER BY " +
       "CASE WHEN :sort = 'name_asc' THEN c.committeeName END ASC, " +
       "CASE WHEN :sort = 'name_desc' THEN c.committeeName END DESC, " +
       "CASE WHEN :sort = 'created_asc' THEN c.createdAt END ASC, " +
       "CASE WHEN :sort = 'created_desc' THEN c.createdAt END DESC")
Page<Committee> findByFiltersManual(
    @Param("status") CommitteeStatus status, 
    @Param("search") String search, 
    @Param("sort") String sort, 
    Pageable pageable
);
    @Query("SELECT c FROM Committee c WHERE c.createdBy = :coordinatorId")
    Page<Committee> findByCreatedByWithDetails(@Param("coordinatorId") Long coordinatorId, Pageable pageable);
}
