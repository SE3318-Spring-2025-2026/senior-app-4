package com.spms.backend.service;

import com.spms.backend.dto.response.GradeItemDTO;
import com.spms.backend.dto.response.GradeListResponse;
import com.spms.backend.model.Submission;
import com.spms.backend.model.SubmissionGrade;
import com.spms.backend.repository.SubmissionGradeRepository;
import com.spms.backend.repository.SubmissionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubmissionGradeService {

    private final SubmissionGradeRepository gradeRepository;
    private final SubmissionRepository submissionRepository;

    public SubmissionGradeService(SubmissionGradeRepository gradeRepository, SubmissionRepository submissionRepository) {
        this.gradeRepository = gradeRepository;
        this.submissionRepository = submissionRepository;
    }

    public GradeListResponse getSubmissionGrades(Long submissionId, String userRole) {
        // Enforce 404 if submission doesn't exist
        submissionRepository.findById(submissionId)
            .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));
            
        List<SubmissionGrade> grades = gradeRepository.findBySubmissionId(submissionId);
        
        int gradeCount = grades.size();
        // Since we are mocking the committee, let's assume total members is 3 for demonstration.
        // In real process, this comes from CommitteeService or D5 store.
        int totalCommitteeMembers = 3; 
        
        boolean isGradingComplete = (gradeCount >= totalCommitteeMembers);
        
        Double averageGrade = null;
        if (!grades.isEmpty() && isGradingComplete) {
            averageGrade = grades.stream().mapToDouble(SubmissionGrade::getScore).average().orElse(0.0);
        }

        List<GradeItemDTO> gradeItems = new ArrayList<>();
        
        // Spec: Students receive only averageGrade and isGradingComplete status.
        // If not a student, send full details.
        if (!"STUDENT".equalsIgnoreCase(userRole)) {
            gradeItems = grades.stream().map(g -> new GradeItemDTO(
                    g.getId(),
                    g.getReviewerId(),
                    g.getProfessorName(),
                    g.getScore(),
                    g.getComments(),
                    new ArrayList<>(), // Mocking empty criteria scores for now
                    g.getGradedAt()
            )).collect(Collectors.toList());
            
            // If grading is not complete, we still calculate an average for Professors/Coordinators to see current progress.
            if (!isGradingComplete && !grades.isEmpty()) {
                averageGrade = grades.stream().mapToDouble(SubmissionGrade::getScore).average().orElse(0.0);
            }
        }

        GradeListResponse.GradeListData data = new GradeListResponse.GradeListData(
                averageGrade,
                gradeCount,
                totalCommitteeMembers,
                isGradingComplete,
                gradeItems
        );

        return new GradeListResponse("success", data);
    }
}
