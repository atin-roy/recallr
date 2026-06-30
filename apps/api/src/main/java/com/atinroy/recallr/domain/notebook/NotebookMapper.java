package com.atinroy.recallr.domain.notebook;

import com.atinroy.recallr.domain.notebook.dto.NotebookRequest;
import com.atinroy.recallr.domain.notebook.dto.NotebookResponse;
import com.atinroy.recallr.domain.user.User;
import org.springframework.stereotype.Component;

@Component
public class NotebookMapper {

    public Notebook toEntity(NotebookRequest request, User user) {
        Notebook notebook = new Notebook();
        notebook.setUser(user);
        notebook.setName(request.name());
        notebook.setDescription(request.description());
        return notebook;
    }

    public NotebookResponse toResponse(Notebook notebook) {
        return new NotebookResponse(
                notebook.getId(),
                notebook.getName(),
                notebook.getDescription(),
                notebook.getCreatedAt(),
                notebook.getUpdatedAt()
        );
    }
}
