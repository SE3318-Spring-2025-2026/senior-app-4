package com.spms.backend.repository;

import com.spms.backend.model.Sprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * ISSUE #396: SprintRepository for querying sprint data
 */
@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    /**
     * Find active sprint by current date.
     * Returns the sprint where the given date falls within start_date and end_date,
     * and the status is 'Active'.
     *
     * @param currentDate the date to check (usually today)
     * @return Optional containing the active sprint if found
     */
    @Query("SELECT s FROM Sprint s WHERE s.status = 'Active' " +
           "AND :currentDate >= s.startDate AND :currentDate <= s.endDate")
    Optional<Sprint> findActiveSprintByDate(@Param("currentDate") LocalDate currentDate);
}
