package com.spms.backend.service;

import com.spms.backend.model.User;
import com.spms.backend.repository.CommitteeAdvisorRepository;
import com.spms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AdvisorValidator {

    private final UserRepository userRepository;
    private final CommitteeAdvisorRepository committeeAdvisorRepository;

    public AdvisorValidator(UserRepository userRepository, CommitteeAdvisorRepository committeeAdvisorRepository) {
        this.userRepository = userRepository;
        this.committeeAdvisorRepository = committeeAdvisorRepository;
    }

    public ValidationResult isAdvisorQualified(Long advisorId) {
        Optional<User> userOpt = userRepository.findById(advisorId);
        if (userOpt.isEmpty()) {
            return ValidationResult.failure("Advisor does not exist.");
        }
        User user = userOpt.get();
        if ("student".equalsIgnoreCase(user.getRole())) {
            return ValidationResult.failure("Advisor must have faculty status.");
        }
        return ValidationResult.success("Advisor is qualified.");
    }

    public int getAdvisorCommitteeCount(Long advisorId) {
        return committeeAdvisorRepository.findCommitteeIdsByAdvisorUserId(advisorId).size();
    }

    public ValidationResult isDuplicateAssignment(Long committeeId, Long advisorId) {
        boolean isDuplicate = committeeAdvisorRepository.findAll().stream()
                .anyMatch(ca -> ca.getCommittee().getCommitteeId().equals(committeeId)
                        && ca.getAdvisor().getUserId().equals(advisorId));
        
        if (isDuplicate) {
            return ValidationResult.failure("Duplicate assignment detected.");
        }
        return ValidationResult.success("Assignment is unique.");
    }
}
