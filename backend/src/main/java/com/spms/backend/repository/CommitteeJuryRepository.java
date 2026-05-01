package com.spms.backend.repository;

import com.spms.backend.model.CommitteeJury;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommitteeJuryRepository extends JpaRepository<CommitteeJury, Long> {

    @Query("SELECT cj FROM CommitteeJury cj WHERE cj.committee.committeeId = :committeeId")
    List<CommitteeJury> findByCommitteeId(@Param("committeeId") Long committeeId);

    @Query("SELECT cj FROM CommitteeJury cj WHERE cj.juryMember.userId = :juryMemberId")
    List<CommitteeJury> findByJuryMemberId(@Param("juryMemberId") Long juryMemberId);

    @Query("SELECT cj FROM CommitteeJury cj WHERE cj.committee.committeeId = :committeeId AND cj.juryMember.userId = :juryMemberId")
    Optional<CommitteeJury> findByCommitteeIdAndJuryMemberId(@Param("committeeId") Long committeeId, @Param("juryMemberId") Long juryMemberId);

    @Query("SELECT cj.committee.committeeId FROM CommitteeJury cj WHERE cj.juryMember.userId = :juryMemberId")
    List<Long> findCommitteeIdsByJuryMemberId(@Param("juryMemberId") Long juryMemberId);
}
