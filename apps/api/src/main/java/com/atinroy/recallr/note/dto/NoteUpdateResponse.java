package com.atinroy.recallr.note.dto;

public record NoteUpdateResponse (
        String id,
        String title,
        String content
){
}
