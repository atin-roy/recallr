package com.atinroy.recallr.topic;

import com.atinroy.recallr.subject.Subject;
import com.atinroy.recallr.topic.dto.TopicRequest;
import com.atinroy.recallr.topic.dto.TopicResponse;
import org.springframework.stereotype.Component;

@Component
public class TopicMapper {

    public Topic toEntity(TopicRequest request, Subject subject) {
        Topic topic = new Topic();
        topic.setSubject(subject);
        topic.setName(request.name());
        topic.setDescription(request.description());
        return topic;
    }

    public TopicResponse toResponse(Topic topic) {
        return new TopicResponse(
                topic.getId().toString(),
                topic.getName(),
                topic.getDescription()
        );
    }
}
