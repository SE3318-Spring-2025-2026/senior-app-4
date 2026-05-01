package com.spms.backend.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
public class CommitteeRepositoryTest {

    /* TDD SPECIFICATION:
       Uncomment and implement when Committee and CommitteeRepository are created.
       
    @Autowired
    private CommitteeRepository committeeRepository;

    @Test
    @DisplayName("CommitteeRepository: Should save and find committee by ID")
    void saveAndFindById_Success() {
        Committee committee = new Committee();
        committee.setCommitteeName("Project Defense Committee");
        committee.setStatus("ACTIVE");
        
        Committee saved = committeeRepository.save(committee);
        Optional<Committee> found = committeeRepository.findById(saved.getCommitteeId());

        assertTrue(found.isPresent());
        assertEquals("Project Defense Committee", found.get().getCommitteeName());
    }

    @Test
    @DisplayName("CommitteeRepository: Should find committees by status")
    void findByStatus_ReturnsMatchingCommittees() {
        Committee c1 = new Committee();
        c1.setStatus("ACTIVE");
        committeeRepository.save(c1);

        List<Committee> activeCommittees = committeeRepository.findByStatus("ACTIVE");

        assertEquals(1, activeCommittees.size());
        assertEquals("ACTIVE", activeCommittees.get(0).getStatus());
    }

    @Test
    @DisplayName("CommitteeRepository: Should find committees by createdBy")
    void findByCreatedBy_ReturnsMatchingCommittees() {
        Committee c1 = new Committee();
        c1.setCreatedBy(100L);
        committeeRepository.save(c1);

        List<Committee> myCommittees = committeeRepository.findByCreatedBy(100L);

        assertEquals(1, myCommittees.size());
        assertEquals(100L, myCommittees.get(0).getCreatedBy());
    }
    
    @Test
    @DisplayName("CommitteeRepository: Should cascade delete properly")
    void delete_Cascades() {
        Committee c1 = new Committee();
        Committee saved = committeeRepository.save(c1);
        
        committeeRepository.delete(saved);
        Optional<Committee> found = committeeRepository.findById(saved.getCommitteeId());
        
        assertFalse(found.isPresent());
    }
    */
}
