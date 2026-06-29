package com.atinroy.recallr.topic;

import com.atinroy.recallr.topic.dto.TopicRequest;
import com.atinroy.recallr.topic.dto.TopicResponse;
import com.atinroy.recallr.topic.dto.TopicUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subjects/{subjectId}/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public TopicResponse createTopic(@PathVariable UUID subjectId,
                                     @RequestBody @Valid TopicRequest request) {
        return topicService.createTopic(subjectId, request);
    }

    @GetMapping("/{topicId}")
    public TopicResponse getTopicById(@PathVariable UUID subjectId,
                                      @PathVariable UUID topicId) {
        return topicService.getTopicById(subjectId, topicId);
    }

    @PutMapping("/{topicId}")
    public TopicResponse updateTopic(@PathVariable UUID subjectId,
                                     @PathVariable UUID topicId,
                                     @RequestBody @Valid TopicUpdateRequest request) {
        return topicService.updateTopic(subjectId, topicId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{topicId}")
    public void deleteTopic(@PathVariable UUID subjectId,
                            @PathVariable UUID topicId) {
        topicService.deleteTopic(subjectId, topicId);
    }
}
