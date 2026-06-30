package com.atinroy.recallr.domain.notebook;

import com.atinroy.recallr.domain.notebook.dto.NotebookRequest;
import com.atinroy.recallr.domain.notebook.dto.NotebookResponse;
import com.atinroy.recallr.domain.notebook.dto.NotebookUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/notebooks")
@RequiredArgsConstructor
public class NotebookController {

    private final NotebookService notebookService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public NotebookResponse createNotebook(@RequestBody @Valid NotebookRequest request) {
        return notebookService.createNotebook(request);
    }

    @GetMapping("/{notebookId}")
    public NotebookResponse getNotebookById(@PathVariable UUID notebookId) {
        return notebookService.getNotebookById(notebookId);
    }

    @PutMapping("/{notebookId}")
    public NotebookResponse updateNotebook(@PathVariable UUID notebookId,
                                           @RequestBody @Valid NotebookUpdateRequest request) {
        return notebookService.updateNotebook(notebookId, request);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{notebookId}")
    public void deleteNotebook(@PathVariable UUID notebookId) {
        notebookService.deleteNotebook(notebookId);
    }
}
