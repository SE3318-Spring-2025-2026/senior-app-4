package com.spms.backend.controller;

import com.spms.backend.dto.request.AdvisorAssignmentRequest;
import com.spms.backend.dto.request.CommitteeCreateRequest;
import com.spms.backend.dto.request.JuryAssignmentRequest;
import com.spms.backend.dto.response.AdvisorListResponse;
import com.spms.backend.dto.response.AuditLogListResponse;
import com.spms.backend.dto.response.CommitteeDetailDto;
import com.spms.backend.dto.response.CommitteeListResponse;
import com.spms.backend.dto.response.CommitteePaginationInfo;
import com.spms.backend.dto.response.JuryListResponse;
import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Committee;
import com.spms.backend.model.enums.CommitteeStatus;
import com.spms.backend.service.AdvisorAssignmentService;
import com.spms.backend.service.AuditLogService;
import com.spms.backend.service.CommitteeService;
import com.spms.backend.service.JuryAssignmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/committees")
public class CommitteeController {

    private final CommitteeService committeeService;
    private final AdvisorAssignmentService advisorAssignmentService;
    private final JuryAssignmentService juryAssignmentService;
    private final AuditLogService auditLogService;

    public CommitteeController(CommitteeService committeeService,
            AdvisorAssignmentService advisorAssignmentService,
            JuryAssignmentService juryAssignmentService,
            AuditLogService auditLogService) {
        this.committeeService = committeeService;
        this.advisorAssignmentService = advisorAssignmentService;
        this.juryAssignmentService = juryAssignmentService;
        this.auditLogService = auditLogService;
    }

    @PostMapping
    public ResponseEntity<CommitteeDetailDto> createCommittee(
            @Valid @RequestBody CommitteeCreateRequest request,
            HttpServletRequest httpReq) {
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        Committee created = committeeService.createCommittee(request, userId);
        Committee fullDetails = committeeService.getCommitteeByIdWithFullDetails(created.getCommitteeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toCommitteeDetailDto(fullDetails));
    }
    @GetMapping
    public ResponseEntity<CommitteeListResponse> getAllCommittees(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "created_desc") String sort,// Varsayılanı 'created_desc' yaptık

            @RequestParam(defaultValue = "ASC") String sortDir){
        // Sayfalama parametrelerinin doğruluğunu kontrol ediyoruz
        if (page < 0 || size <= 0 || size > 100) {
            throw new com.spms.backend.exception.BadRequestException(
                    "Invalid pagination parameters. Page must be >= 0, size must be between 1 and 100.");
        }

        // Eğer status (durum) parametresi boş değilse onu Enum'a çeviriyoruz
        CommitteeStatus committeeStatus = null;
        if (!status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            try {
                committeeStatus = CommitteeStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new com.spms.backend.exception.BadRequestException(
                        "Invalid status. Allowed values: ACTIVE, INACTIVE, COMPLETED");
            }
        }

        // YENİ: Frontend'den gelen sıralama kelimelerini veritabanının anlayacağı dile çeviriyoruz
        
        // ... (Yukarıdaki sayfalama ve status kontrol kodları aynı kalacak) ...

        // YENİ: Sıralama kurallarını tutacağımız bir liste oluşturuyoruz
        List<Sort.Order> orders = new ArrayList<>();

        // Frontend'den gelen isteğe göre doğru kuralı (Büyük/Küçük harf duyarsız olarak) ekliyoruz
        if (sort != null) {
            if (sort.equals("name_asc")) {
                // A'dan Z'ye sırala ve büyük/küçük harf ayrımını YOK SAY
                orders.add(Sort.Order.asc("committeeName").ignoreCase());
            } else if (sort.equals("name_desc")) {
                // Z'den A'ya sırala ve büyük/küçük harf ayrımını YOK SAY
                orders.add(Sort.Order.desc("committeeName").ignoreCase());
            } else if (sort.equals("created_asc")) {
                // Tarihe göre eskiden yeniye (Tarihlerde harf duyarlılığına gerek yoktur)
                orders.add(Sort.Order.asc("createdAt"));
            } else if (sort.equals("created_desc")) {
                // Tarihe göre yeniden eskiye
                orders.add(Sort.Order.desc("createdAt"));
            } else {
                // Beklenmedik bir kelime gelirse varsayılan olarak en yenileri getir
                orders.add(Sort.Order.desc("createdAt")); 
            }
        } else {
            orders.add(Sort.Order.desc("createdAt"));
        }

        // Unsorted PageRequest because the @Query explicitly handles sorting using the :sort parameter
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Committee> committeePages = committeeService.getCommitteesByFilters(committeeStatus, search,sort, pageable);

        List<CommitteeDetailDto> committees = committeePages.getContent().stream()
                .map(this::toCommitteeDetailDto)
                .collect(Collectors.toList());

        CommitteePaginationInfo pagination = new CommitteePaginationInfo(
                committeePages.getNumber(),
                committeePages.getTotalPages(),
                committeePages.getTotalElements(),
                committeePages.getSize());

        return ResponseEntity.ok(new CommitteeListResponse("success", committees, pagination));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommitteeDetailDto> getCommitteeById(@PathVariable Long id) {
        Committee committee = committeeService.getCommitteeByIdWithFullDetails(id);
        return ResponseEntity.ok(toCommitteeDetailDto(committee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommitteeDetailDto> updateCommittee(
            @PathVariable Long id,
            @Valid @RequestBody CommitteeCreateRequest request,
            HttpServletRequest httpReq) {
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        Committee updated = committeeService.updateCommittee(id, request, userId);
        Committee fullDetails = committeeService.getCommitteeByIdWithFullDetails(updated.getCommitteeId());
        return ResponseEntity.ok(toCommitteeDetailDto(fullDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommittee(@PathVariable Long id, HttpServletRequest httpReq) {
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        committeeService.deleteCommittee(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/advisors")
    public ResponseEntity<Map<String, String>> assignAdvisor(
            @PathVariable Long id,
            @Valid @RequestBody AdvisorAssignmentRequest request,
            HttpServletRequest httpReq) {
        Object role = httpReq.getAttribute("jwt_role");
        if (role == null || !"coordinator".equalsIgnoreCase(role.toString())) {
            throw new ForbiddenException("Only coordinators can assign advisors.");
        }
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        advisorAssignmentService.assignAdvisor(id, request.getAdvisorId(), request.getRole(), userId);
        return ResponseEntity.ok(Map.of("message", "Advisor assigned successfully"));
    }

    @GetMapping("/{id}/advisors")
    public ResponseEntity<AdvisorListResponse> getAdvisorsByCommittee(@PathVariable Long id) {
        Committee committee = committeeService.getCommitteeById(id);
        List<com.spms.backend.dto.response.AdvisorDto> advisors = committee.getAdvisors().stream()
                .map(ca -> new com.spms.backend.dto.response.AdvisorDto(
                        ca.getCommitteeAdvisorId(),
                        ca.getAdvisor().getUserId(),
                        ca.getAdvisor().getFullName(),
                        ca.getRole(),
                        ca.getAssignedAt()))
                .toList();
        return ResponseEntity.ok(new AdvisorListResponse("success", advisors));
    }

    @DeleteMapping("/{id}/advisors/{committeeAdvisorId}")
    public ResponseEntity<Void> removeAdvisor(@PathVariable Long id, @PathVariable Long committeeAdvisorId,
            HttpServletRequest httpReq) {
        Object role = httpReq.getAttribute("jwt_role");
        if (role == null || !"coordinator".equalsIgnoreCase(role.toString())) {
            throw new ForbiddenException("Only coordinators can remove advisors.");
        }
        advisorAssignmentService.removeAdvisor(id, committeeAdvisorId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/jury")
    public ResponseEntity<Map<String, String>> assignJury(
            @PathVariable Long id,
            @Valid @RequestBody JuryAssignmentRequest request,
            HttpServletRequest httpReq) {
        Object role = httpReq.getAttribute("jwt_role");
        if (role == null || !"coordinator".equalsIgnoreCase(role.toString())) {
            throw new ForbiddenException("Only coordinators can assign jury members.");
        }
        Long userId = ((Number) httpReq.getAttribute("jwt_userId")).longValue();
        juryAssignmentService.assignJury(id, request.getJuryMemberId(), request.getJuryType(), userId);
        return ResponseEntity.ok(Map.of("message", "Jury member assigned successfully"));
    }

    @GetMapping("/{id}/jury")
    public ResponseEntity<JuryListResponse> getJuryByCommittee(@PathVariable Long id) {
        return ResponseEntity.ok(juryAssignmentService.getJuryByCommittee(id));
    }

    @DeleteMapping("/{id}/jury/{juryMemberId}")
    public ResponseEntity<Void> removeJury(@PathVariable Long id, @PathVariable Long juryMemberId,
            HttpServletRequest httpReq) {
        Object role = httpReq.getAttribute("jwt_role");
        if (role == null || !"coordinator".equalsIgnoreCase(role.toString())) {
            throw new ForbiddenException("Only coordinators can remove jury members.");
        }
        juryAssignmentService.removeJury(id, juryMemberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<CommitteeListResponse> getCommitteesByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new com.spms.backend.exception.BadRequestException(
                    "Invalid pagination parameters. Page must be >= 0, size must be between 1 and 100.");
        }

        CommitteeStatus committeeStatus;
        try {
            committeeStatus = CommitteeStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.spms.backend.exception.BadRequestException(
                    "Invalid status. Allowed values: ACTIVE, INACTIVE, COMPLETED");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Committee> committeePages = committeeService.getCommitteesByStatus(committeeStatus, pageable);

        List<CommitteeDetailDto> committees = committeePages.getContent().stream()
                .map(this::toCommitteeDetailDto)
                .collect(Collectors.toList());

        CommitteePaginationInfo pagination = new CommitteePaginationInfo(
                committeePages.getNumber(),
                committeePages.getTotalPages(),
                committeePages.getTotalElements(),
                committeePages.getSize());

        return ResponseEntity.ok(new CommitteeListResponse("success", committees, pagination));
    }

    @GetMapping("/coordinator/{coordinatorId}")
    public ResponseEntity<CommitteeListResponse> getCommitteesByCoordinator(
            @PathVariable Long coordinatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException(
                    "Invalid pagination parameters. Page must be >= 0, size must be between 1 and 100.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Committee> committeePages = committeeService.getCommitteesByCoordinator(coordinatorId, pageable);

        List<CommitteeDetailDto> committees = committeePages.getContent().stream()
                .map(this::toCommitteeDetailDto)
                .collect(Collectors.toList());

        CommitteePaginationInfo pagination = new CommitteePaginationInfo(
                committeePages.getNumber(),
                committeePages.getTotalPages(),
                committeePages.getTotalElements(),
                committeePages.getSize());

        return ResponseEntity.ok(new CommitteeListResponse("success", committees, pagination));
    }

    @GetMapping("/{committeeId}/audit-logs")
    public ResponseEntity<AuditLogListResponse> getCommitteeAuditLogs(
            @PathVariable Long committeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpReq) {
        Object role = httpReq.getAttribute("jwt_role");
        if (role == null || (!("coordinator".equalsIgnoreCase(role.toString()))
                && !("admin".equalsIgnoreCase(role.toString())))) {
            throw new ForbiddenException("Only coordinators and admins can view audit logs.");
        }

        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException(
                    "Invalid pagination parameters. Page must be >= 0, size must be between 1 and 100.");
        }

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(auditLogService.getAuditLogsByCommittee(committeeId, pageable));
    }

    @GetMapping("/jury/{juryMemberId}/committees")
    public ResponseEntity<JuryListResponse> getCommitteesByJury(@PathVariable Long juryMemberId) {
        return ResponseEntity.ok(juryAssignmentService.getCommitteesByJury(juryMemberId));
    }

    private CommitteeDetailDto toCommitteeDetailDto(Committee c) {
        List<com.spms.backend.dto.response.AdvisorDto> advisors = c.getAdvisors() != null ? c.getAdvisors().stream()
                .map(ca -> new com.spms.backend.dto.response.AdvisorDto(
                        ca.getCommitteeAdvisorId(),
                        ca.getAdvisor().getUserId(),
                        ca.getAdvisor().getFullName(),
                        ca.getRole(),
                        ca.getAssignedAt()))
                .collect(Collectors.toList()) : List.of();

        List<com.spms.backend.dto.response.JuryMemberDto> juryMembers = c.getJuryMembers() != null
                ? c.getJuryMembers().stream()
                        .map(cj -> new com.spms.backend.dto.response.JuryMemberDto(
                                cj.getJuryMemberId(),
                                cj.getJuryMember().getUserId(),
                                cj.getJuryMember().getFullName(),
                                cj.getJuryType(),
                                cj.getAssignedAt()))
                        .collect(Collectors.toList())
                : List.of();

        List<com.spms.backend.dto.response.GroupAssignmentDto> groupAssignments = c.getGroupAssignments() != null
                ? c.getGroupAssignments().stream()
                        .map(ga -> new com.spms.backend.dto.response.GroupAssignmentDto(
                                ga.getAssignmentId(),
                                ga.getGroup().getId(),
                                ga.getGroup().getGroupName(),
                                ga.getStatus(),
                                ga.getExamDate(),
                                ga.getAssignedAt()))
                        .collect(Collectors.toList())
                : List.of();

        return new CommitteeDetailDto(
                c.getCommitteeId(),
                c.getCommitteeName(),
                c.getDescription(),
                c.getStatus() != null ? c.getStatus().name() : null,
                c.getCreatedBy(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                advisors,
                juryMembers,
                groupAssignments);
    }
}
