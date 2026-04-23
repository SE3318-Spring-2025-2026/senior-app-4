package com.spms.backend.service.impl;

import com.spms.backend.dto.response.AdvisorRequestSummaryDto;
import com.spms.backend.model.Group;
import com.spms.backend.model.User;
import com.spms.backend.model.notification.Notification;
import com.spms.backend.model.notification.NotificationStatus;
import com.spms.backend.model.notification.NotificationType;
import com.spms.backend.repository.GroupMemberRepository;
import com.spms.backend.repository.GroupRepository;
import com.spms.backend.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AdvisorRequestServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private AdvisorRequestServiceImpl advisorRequestService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listAdvisorRequests_AsProfessor_MapsStatusesCorrectly() {
        // Arrange
        Long profId = 1L;
        
        User prof = new User();
        prof.setUserId(profId);
        prof.setFullName("Dr. Test");

        Notification n1 = new Notification();
        n1.setId(10L);
        n1.setStatus(NotificationStatus.PENDING);
        n1.setGroupId(100L);
        n1.setToUser(prof);
        n1.setCreatedAt(Instant.now());

        Notification n2 = new Notification();
        n2.setId(11L);
        n2.setStatus(NotificationStatus.ACCEPTED);
        n2.setGroupId(100L);
        n2.setToUser(prof);
        n2.setCreatedAt(Instant.now());

        when(notificationRepository.findByToUser_UserIdAndTypeOrderByCreatedAtDesc(profId, NotificationType.ADVISOR_REQUEST))
                .thenReturn(List.of(n1, n2));

        Group group = new Group();
        group.setId(100L);
        group.setGroupName("Test Group");
        when(groupRepository.findById(100L)).thenReturn(Optional.of(group));

        // Act
        List<AdvisorRequestSummaryDto> results = advisorRequestService.listAdvisorRequests(profId, "professor", null, null, null);

        // Assert
        assertEquals(2, results.size());
        
        // n1 (PENDING) -> PENDING, decidedAt null
        assertEquals("PENDING", results.get(0).status());
        assertNull(results.get(0).decidedAt());

        // n2 (ACCEPTED) -> APPROVED, decidedAt not null
        assertEquals("APPROVED", results.get(1).status());
        assertEquals(n2.getCreatedAt(), results.get(1).decidedAt());
    }
}
