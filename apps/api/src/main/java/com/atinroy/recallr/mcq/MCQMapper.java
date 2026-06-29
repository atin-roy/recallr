package com.atinroy.recallr.mcq;

import com.atinroy.recallr.mcq.dto.MCQResponse;
import com.atinroy.recallr.user.User;
import org.springframework.stereotype.Component;

@Component
public class MCQMapper {

    public MCQ toEntity(MCQCreateRequest request, User user) {
        MCQ mcq = new MCQ();
        mcq.setUser(user);
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
                mcq.getExplanation()
        );
    }
}
