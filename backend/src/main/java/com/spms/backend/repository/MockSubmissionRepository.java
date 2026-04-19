package com.spms.backend.repository;

import com.spms.backend.model.Submission;
import com.spms.backend.model.enums.DeliverableType;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class MockSubmissionRepository implements SubmissionRepository {

    private final List<Submission> submissions = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<Submission> findTopByGroupIdAndDeliverableTypeOrderByCreatedAtDesc(Long groupId, DeliverableType type) {
        return submissions.stream()
                .filter(s -> s.getGroupId().equals(groupId) && s.getDeliverableType() == type)
                .max((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()));
    }

    @Override
    public Optional<Submission> findById(Long id) {
        return submissions.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    @Override
    public Submission save(Submission submission) {
        if (submission.getId() == null) {
            submission.setId(idGenerator.getAndIncrement());
            submissions.add(submission);
        } else {
            submissions.removeIf(s -> s.getId().equals(submission.getId()));
            submissions.add(submission);
        }
        return submission;
    }
}
