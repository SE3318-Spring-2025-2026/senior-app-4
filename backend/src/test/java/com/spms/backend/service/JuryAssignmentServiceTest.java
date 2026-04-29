package com.spms.backend.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JuryAssignmentServiceTest {

    /* TDD SPECIFICATION:
       Uncomment and implement when JuryAssignmentService is created.
       
    @Mock
    private CommitteeRepository committeeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JuryAssignmentServiceImpl juryAssignmentService;

    @Test
    @DisplayName("JuryAssignmentService: Should assign jury successfully")
    void assignJury_Success() {
        Committee c = new Committee();
        c.setCommitteeId(2L);
        when(committeeRepository.findById(2L)).thenReturn(Optional.of(c));

        User jury = new User();
        jury.setUserId(200L);
        when(userRepository.findById(200L)).thenReturn(Optional.of(jury));

        juryAssignmentService.assignJury(2L, 200L, "INTERNAL");
        
        verify(committeeRepository, times(1)).save(c);
    }

    @Test
    @DisplayName("JuryAssignmentService: Type validation for Jury")
    void assignJury_InvalidType_ThrowsException() {
        Committee c = new Committee();
        c.setCommitteeId(2L);
        when(committeeRepository.findById(2L)).thenReturn(Optional.of(c));

        User jury = new User();
        jury.setUserId(200L);
        when(userRepository.findById(200L)).thenReturn(Optional.of(jury));

        assertThrows(BadRequestException.class, () -> juryAssignmentService.assignJury(2L, 200L, "INVALID_TYPE"));
    }
    */
}
