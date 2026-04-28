package com.spms.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AdvisorAssignmentServiceProcess5Test {

    /* TDD SPECIFICATION:
       Uncomment and implement when AdvisorAssignmentService methods are created.
       
    @Mock
    private CommitteeRepository committeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdvisorAssignmentServiceImpl advisorAssignmentService;

    @Test
    @DisplayName("AdvisorAssignmentService: Should assign advisor to committee successfully")
    void assignAdvisor_Success() {
        Committee c = new Committee();
        c.setId(1L);
        when(committeeRepository.findById(1L)).thenReturn(Optional.of(c));
        
        User advisor = new User();
        advisor.setUserId(100L);
        advisor.setRole("PROFESSOR");
        when(userRepository.findById(100L)).thenReturn(Optional.of(advisor));

        advisorAssignmentService.assignAdvisor(1L, 100L);
        
        verify(committeeRepository, times(1)).save(c);
        // Assuming committee.getAdvisors().contains(advisor)
    }

    @Test
    @DisplayName("AdvisorAssignmentService: Should prevent duplicate advisor assignment")
    void assignAdvisor_Duplicate_ThrowsException() {
        Committee c = new Committee();
        c.setId(1L);
        User advisor = new User();
        advisor.setUserId(100L);
        // Mock c.getAdvisors() containing advisor
        
        when(committeeRepository.findById(1L)).thenReturn(Optional.of(c));
        when(userRepository.findById(100L)).thenReturn(Optional.of(advisor));

        assertThrows(BadRequestException.class, () -> advisorAssignmentService.assignAdvisor(1L, 100L));
    }
    
    @Test
    @DisplayName("AdvisorAssignmentService: Should remove advisor from committee")
    void removeAdvisor_Success() {
        Committee c = new Committee();
        c.setId(1L);
        
        User advisor = new User();
        advisor.setUserId(100L);
        
        // Mocking that the advisor is currently assigned
        when(committeeRepository.findById(1L)).thenReturn(Optional.of(c));

        advisorAssignmentService.removeAdvisor(1L, 100L);
        
        // Verifying that the entity is saved after removal
        verify(committeeRepository, times(1)).save(c);
    }
    */
}
