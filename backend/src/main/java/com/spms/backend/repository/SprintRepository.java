package com.spms.backend.repository;

import com.spms.backend.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ISSUE #396: SprintRepository for querying sprint data
 */
@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    List<Sprint> findAllByOrderByStartDateAscIdAsc();

    /**
     * Find the sprint whose date range contains the given date.
     * Status is intentionally excluded from the filter — it is computed
     * dynamically from dates and may lag in legacy rows.
     */
    @Query("SELECT s FROM Sprint s WHERE :currentDate >= s.startDate AND :currentDate <= s.endDate")
    Optional<Sprint> findActiveSprintByDate(@Param("currentDate") LocalDate currentDate);
}
