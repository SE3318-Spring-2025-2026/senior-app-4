package com.spms.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitteeDto {
    private Long id;
    private String name;
    private String status;
    private Long coordinatorId;
}