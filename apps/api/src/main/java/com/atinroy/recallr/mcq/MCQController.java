package com.atinroy.recallr.mcq;

import com.atinroy.recallr.mcq.dto.MCQResponse;
import com.atinroy.recallr.mcq.dto.MCQUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/mcqs")
@RequiredArgsConstructor
public class MCQController {

    private final MCQService mcqService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public MCQResponse createMCQ(@RequestBody @Valid MCQCreateRequest request) {
        return mcqService.createMCQ(request);
    }

    @GetMapping("/{mcqId}")
    public MCQResponse getMCQById(@PathVariable UUID mcqId) {
        return mcqService.getMCQById(mcqId);
    }

    @PutMapping("/{mcqId}")
    public MCQResponse updateMCQ(@PathVariable UUID mcqId, @RequestBody @Valid MCQUpdateRequest request) {
        return mcqService.updateMCQ(mcqId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{mcqId}")
    public void deleteMCQ(@PathVariable UUID mcqId) {
        mcqService.deleteMCQ(mcqId);
    }
}
