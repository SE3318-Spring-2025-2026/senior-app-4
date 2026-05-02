package com.spms.backend.controller;

import com.spms.backend.dto.request.BulkUpdateRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/records")
public class RecordsController {

    @PostMapping("/prepare-sync")
    public ResponseEntity<BulkUpdateRequestDto> prepareSync(@Valid @RequestBody BulkUpdateRequestDto requestDto) {
        // Automatically validated by @Valid
        return ResponseEntity.ok(requestDto);
    }
}
