package com.atinroy.recallr.domain.note;

import com.atinroy.recallr.domain.note.dto.NoteLinkResponse;
import org.springframework.stereotype.Component;

@Component
public class NoteLinkMapper {

    public NoteLinkResponse toResponse(NoteLink link) {
        return new NoteLinkResponse(
                link.getId(),
                link.getSource().getId(),
                link.getTarget().getId()
        );
    }
}
