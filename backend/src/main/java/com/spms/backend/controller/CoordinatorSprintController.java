package com.spms.backend.controller;

import com.spms.backend.dto.request.SprintCreateRequestDto;
import com.spms.backend.dto.request.SprintUpdateRequestDto;
import com.spms.backend.dto.response.SprintDto;
import com.spms.backend.service.CoordinatorSprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Coordinator")
@RestController
@RequestMapping("/api/v1/coordinator/sprints")
public class CoordinatorSprintController {

    private final CoordinatorSprintService sprintService;

    public CoordinatorSprintController(CoordinatorSprintService sprintService) {
        this.sprintService = sprintService;
    }

    @Operation(summary = "List all sprints")
    @GetMapping
    public ResponseEntity<List<SprintDto>> getAllSprints(HttpServletRequest request) {
        if (!isCoordinator(request) && !isProfessor(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(sprintService.getAllSprints());
    }

    @Operation(summary = "Create a sprint")
    @PostMapping
    public ResponseEntity<SprintDto> createSprint(
            @Valid @RequestBody SprintCreateRequestDto body,
            HttpServletRequest request) {
        if (!isCoordinator(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.status(HttpStatus.CREATED).body(sprintService.createSprint(body));
    }

    @Operation(summary = "Update a sprint")
    @PutMapping("/{sprintId}")
    public ResponseEntity<SprintDto> updateSprint(
            @PathVariable Long sprintId,
            @Valid @RequestBody SprintUpdateRequestDto body,
            HttpServletRequest request) {
        if (!isCoordinator(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(sprintService.updateSprint(sprintId, body));
    }

    @Operation(summary = "Delete a sprint")
    @DeleteMapping("/{sprintId}")
    public ResponseEntity<Void> deleteSprint(
            @PathVariable Long sprintId,
            HttpServletRequest request) {
        if (!isCoordinator(request)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        sprintService.deleteSprint(sprintId);
        return ResponseEntity.noContent().build();
    }

    private boolean isCoordinator(HttpServletRequest request) {
        return "coordinator".equals(request.getAttribute("jwt_role"));
    }

    private boolean isProfessor(HttpServletRequest request) {
        return "professor".equals(request.getAttribute("jwt_role"));
    }
}
