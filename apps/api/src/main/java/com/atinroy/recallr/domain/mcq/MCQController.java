package com.atinroy.recallr.domain.mcq;

import com.atinroy.recallr.domain.mcq.dto.MCQResponse;
import com.atinroy.recallr.domain.mcq.dto.MCQUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subjects/{subjectId}/mcqs")
@RequiredArgsConstructor
public class MCQController {

    private final MCQService mcqService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public MCQResponse createMCQ(@PathVariable UUID subjectId,
                                  @RequestBody @Valid MCQCreateRequest request) {
        return mcqService.createMCQ(subjectId, request);
    }

    @GetMapping("/{mcqId}")
    public MCQResponse getMCQById(@PathVariable UUID subjectId,
                                   @PathVariable UUID mcqId) {
        return mcqService.getMCQById(subjectId, mcqId);
    }

    @PutMapping("/{mcqId}")
    public MCQResponse updateMCQ(@PathVariable UUID subjectId,
                                  @PathVariable UUID mcqId,
                                  @RequestBody @Valid MCQUpdateRequest request) {
        return mcqService.updateMCQ(subjectId, mcqId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{mcqId}")
    public void deleteMCQ(@PathVariable UUID subjectId,
                          @PathVariable UUID mcqId) {
        mcqService.deleteMCQ(subjectId, mcqId);
    }
}
