package com.socialpulse.app.common.exception.status;

import lombok.Getter;

@Getter
public enum CommentCode implements AppCode {
    COMMENT_NOT_FOUND(404, "Comment not found"),
    REPLY_TO_COMMENT_NOT_ALLOWED(400, "Cannot reply to a comment that is already a reply"),
    PARENT_MUST_BELONG_TO_SAME_POST(400, "Parent comment must belong to the same post"),
    CANNOT_REPLY_TO_DELETED_COMMENT(400, "Cannot reply to a deleted comment"),
    COMMENT_NOT_OWNER(403, "You are not the owner of this comment"),
    COMMENT_NOT_BELONG_TO_POST(400, "Comment does not belong to the specified post"),
    CANNOT_EDIT_DELETED_COMMENT(400, "Cannot edit a deleted comment"),
    COMMENT_ALREADY_DELETED(400, "Comment is already deleted");

    private final int code;
    private final String message;

    CommentCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
