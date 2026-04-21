package com.spms.backend.service;

import com.spms.backend.dto.request.GroupCreateRequestDto;
import com.spms.backend.dto.request.GroupUpdateRequestDto;
import com.spms.backend.dto.response.GroupDetailDto;
import com.spms.backend.dto.response.GroupResponseDto;
import com.spms.backend.dto.request.JiraBindingRequest;
import com.spms.backend.dto.response.JiraIntegrationResponse;
import com.spms.backend.dto.response.GithubIntegrationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;




import com.spms.backend.dto.response.MemberResponseDto;
import java.util.List;

public interface GroupService {
    GroupResponseDto createGroup(GroupCreateRequestDto request, Long creatorId);
    
    void updateGroupName(Long groupId, GroupUpdateRequestDto request, Long requesterId);
    
    //Rol bazlı sorgulama yapabilmek için requesterId ve requesterRole ekledim
    Page<GroupResponseDto> getGroups(Pageable pageable, Long requesterId, String requesterRole);
    
    void disbandGroup(Long groupId, Long requesterId, String requesterRole);

    void bindJiraIntegration(Long groupId, Long requesterId, JiraBindingRequest request);
    JiraIntegrationResponse getJiraIntegration(Long groupId);
    GithubIntegrationResponse getGithubIntegration(Long groupId);
    void unbindJiraIntegration(Long groupId, Long requesterId);

    //rol bazlı detaylıs sorgu
    GroupDetailDto getGroupDetails(Long groupId, Long requesterId, String requesterRole);

    //üye yöneyimi
    List<MemberResponseDto> getGroupMembers(Long groupId);
    void addMember(Long groupId, String studentId);
    void removeMember(Long groupId, String studentId);
    void leaveGroup(Long groupId, Long studentUserId);
}
