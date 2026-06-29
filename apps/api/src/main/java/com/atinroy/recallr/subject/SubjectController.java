package com.atinroy.recallr.subject;

import com.atinroy.recallr.subject.dto.SubjectRequest;
import com.atinroy.recallr.subject.dto.SubjectResponse;
import com.atinroy.recallr.subject.dto.SubjectUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public SubjectResponse createSubject(@RequestBody @Valid SubjectRequest request) {
        return subjectService.createSubject(request);
    }

    @GetMapping("/{subjectId}")
    public SubjectResponse getSubjectById(@PathVariable UUID subjectId) {
        return subjectService.getSubjectById(subjectId);
    }

    @PutMapping("/{subjectId}")
    public SubjectResponse updateSubject(@PathVariable UUID subjectId,
                                         @RequestBody @Valid SubjectUpdateRequest request) {
        return subjectService.updateSubject(subjectId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{subjectId}")
    public void deleteSubject(@PathVariable UUID subjectId) {
        subjectService.deleteSubject(subjectId);
    }
}
