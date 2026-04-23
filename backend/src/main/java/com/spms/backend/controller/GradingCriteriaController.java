package com.spms.backend.controller;

import com.spms.backend.dto.request.GradingCriteriaCreateRequestDto;
import com.spms.backend.dto.response.GradingCriteriaDto;
import com.spms.backend.exception.ForbiddenException;
import com.spms.backend.service.GradingCriteriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/grading-criteria")
public class GradingCriteriaController {

    private final GradingCriteriaService gradingCriteriaService;

    public GradingCriteriaController(GradingCriteriaService gradingCriteriaService) {
        this.gradingCriteriaService = gradingCriteriaService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createCriteria(
            @Valid @RequestBody GradingCriteriaCreateRequestDto request,
            @RequestAttribute("jwt_userId") Object userId,
            @RequestAttribute("jwt_role") Object role) {

        if (!"coordinator".equals(role)) {
            throw new ForbiddenException("Only coordinators can create grading criteria.");
        }

        Long creatorId = Long.valueOf(userId.toString());
        GradingCriteriaDto created = gradingCriteriaService.create(request, creatorId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "success",
                "message", "Grading criteria created successfully.",
                "data", created
        ));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listCriteria(
            @RequestParam(required = false) String deliverableType) {

        List<GradingCriteriaDto> criteria = gradingCriteriaService.list(deliverableType);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", criteria
        ));
    }
}
