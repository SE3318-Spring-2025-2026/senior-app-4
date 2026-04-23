package com.spms.backend.service;

import com.spms.backend.exception.BadRequestException;
import com.spms.backend.exception.NotFoundException;
import com.spms.backend.model.Submission;
import com.spms.backend.model.SubmissionStatus;
import com.spms.backend.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;

    public SubmissionService(SubmissionRepository submissionRepository) {
        this.submissionRepository = submissionRepository;
    }

    @Transactional
    public Submission createRevision(Long submissionId) {
        Submission parent = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NotFoundException("Submission not found."));

        if (parent.getStatus() != SubmissionStatus.REVISION_REQUESTED) {
            throw new BadRequestException("Parent submission must be in REVISION_REQUESTED status.");
        }

        // update parent status
        parent.setStatus(SubmissionStatus.SUPERSEDED);
        submissionRepository.save(parent);

        // create new revision
        Submission revision = new Submission();
        revision.setParentSubmissionId(parent.getId());
        revision.setVersion(parent.getVersion() + 1);
        revision.setStatus(SubmissionStatus.PENDING); // newly created revision is PENDING by default
        
        // Copy fields from parent
        revision.setGroupId(parent.getGroupId());
        revision.setDeliverableType(parent.getDeliverableType());
        revision.setContent(parent.getContent());
        revision.setCommitteeId(parent.getCommitteeId());
        
        return submissionRepository.save(revision);
    }

    public List<Submission> getRevisions(Long submissionId) {
        return submissionRepository.findByParentSubmissionIdOrderByIdAsc(submissionId);
    }
}
