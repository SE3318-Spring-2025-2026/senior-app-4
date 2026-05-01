package com.spms.backend.service;

import com.spms.backend.model.User;
import com.spms.backend.repository.CommitteeJuryRepository;
import com.spms.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class JuryValidator {

    private final UserRepository userRepository;
    private final CommitteeJuryRepository committeeJuryRepository;

    public JuryValidator(UserRepository userRepository, CommitteeJuryRepository committeeJuryRepository) {
        this.userRepository = userRepository;
        this.committeeJuryRepository = committeeJuryRepository;
    }

    public ValidationResult isJuryQualified(Long juryMemberId) {
        Optional<User> userOpt = userRepository.findById(juryMemberId);
        if (userOpt.isEmpty()) {
            return ValidationResult.failure("Jury member does not exist.");
        }
        User user = userOpt.get();
        if ("student".equalsIgnoreCase(user.getRole())) {
            return ValidationResult.failure("Jury member must have faculty status.");
        }
        return ValidationResult.success("Jury member is qualified.");
    }

    public int getJuryCommitteeCount(Long juryMemberId) {
        return committeeJuryRepository.findCommitteeIdsByJuryMemberId(juryMemberId).size();
    }

    public ValidationResult validateJuryType(String juryType) {
        if (juryType == null || juryType.isBlank()) {
            return ValidationResult.failure("Jury type is required.");
        }
        String type = juryType.toUpperCase();
        if (!("INTERNAL".equals(type) || "EXTERNAL".equals(type) || "ADDITIONAL".equals(type))) {
            return ValidationResult.failure("Invalid jury type. Must be INTERNAL, EXTERNAL, or ADDITIONAL.");
        }
        return ValidationResult.success("Jury type is valid.");
    }

    public ValidationResult isDuplicateAssignment(Long committeeId, Long juryMemberId) {
        boolean isDuplicate = committeeJuryRepository.findByCommitteeIdAndJuryMemberId(committeeId, juryMemberId).isPresent();

        if (isDuplicate) {
            return ValidationResult.failure("Duplicate assignment detected.");
        }
        return ValidationResult.success("Assignment is unique.");
    }
}
