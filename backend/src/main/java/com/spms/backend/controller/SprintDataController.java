package com.spms.backend.controller;

import com.spms.backend.dto.request.BulkUpdateRequestDto;
import com.spms.backend.model.ValidStudentId;
import com.spms.backend.service.SprintDataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sprint-data")
public class SprintDataController {

    private final SprintDataService sprintDataService;

    public SprintDataController(SprintDataService sprintDataService) {
        this.sprintDataService = sprintDataService;
    }

    @PutMapping("/bulk-update")
    public ResponseEntity<Map<String, String>> bulkUpdate(@RequestBody BulkUpdateRequestDto requestDto) {
        sprintDataService.bulkUpdateRecords(requestDto);
        return ResponseEntity.ok(Map.of("message", "Bulk update successful"));
    }

    @GetMapping("/records")
    public ResponseEntity<Map<String, List<ValidStudentId>>> getRecords() {
        List<ValidStudentId> records = sprintDataService.getAllRecords();
        return ResponseEntity.ok(Map.of("content", records));
    }
}
