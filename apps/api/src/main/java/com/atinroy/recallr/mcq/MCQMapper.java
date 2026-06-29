package com.atinroy.recallr.mcq;

import com.atinroy.recallr.mcq.dto.MCQResponse;
import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.topic.Topic;
import org.springframework.stereotype.Component;

@Component
public class MCQMapper {

    public MCQ toEntity(MCQCreateRequest request, Subject subject, Topic topic) {
        MCQ mcq = new MCQ();
        mcq.setUser(subject.getUser());
        mcq.setSubject(subject);
        mcq.setTopic(topic);
        mcq.setQuestion(request.question());
        mcq.setOptions(request.options());
        mcq.setCorrectOptionIndex(request.correctOptionIndex());
        mcq.setExplanation(request.explanation());
        return mcq;
    }

    public MCQResponse toResponse(MCQ mcq) {
        return new MCQResponse(
                mcq.getId().toString(),
                mcq.getQuestion(),
                mcq.getOptions(),
                mcq.getCorrectOptionIndex(),
                mcq.getExplanation(),
                mcq.getSubject().getId().toString(),
                mcq.getTopic() != null ? mcq.getTopic().getId().toString() : null
        );
    }
}
