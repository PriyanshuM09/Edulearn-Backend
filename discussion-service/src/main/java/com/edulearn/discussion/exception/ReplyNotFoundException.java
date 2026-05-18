package com.edulearn.discussion.exception;

public class ReplyNotFoundException extends RuntimeException {

    public ReplyNotFoundException(Integer replyId) {
        super("Reply not found with id: " + replyId);
    }

    public ReplyNotFoundException(String message) {
        super(message);
    }
}