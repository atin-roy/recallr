package com.atinroy.recallr.domain.subject;

import com.atinroy.recallr.domain.subject.dto.SubjectRequest;
import com.atinroy.recallr.domain.subject.dto.SubjectResponse;
import com.atinroy.recallr.domain.user.User;
import org.springframework.stereotype.Component;

@Component
public class SubjectMapper {

    public Subject toEntity(SubjectRequest request, User user) {
        Subject subject = new Subject();
        subject.setUser(user);
        subject.setName(request.name());
        subject.setDescription(request.description());
        return subject;
    }

    public SubjectResponse toResponse(Subject subject) {
        return new SubjectResponse(
                subject.getId().toString(),
                subject.getName(),
                subject.getDescription()
        );
    }
}
