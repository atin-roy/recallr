package com.atinroy.recallr.domain.notebook;

public class NotebookNameAlreadyExistsException extends RuntimeException {
    public NotebookNameAlreadyExistsException(String name) {
        super("You already have a notebook named \"" + name + "\"");
    }
}
