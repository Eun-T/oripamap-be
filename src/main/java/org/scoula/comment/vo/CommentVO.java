package org.scoula.comment.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {

    private Long id;
    private Long placeId;
    private Long userId;
    private Long parentCommentId;
    private String content;
    private String imageKey;

    private String nickname;
    private String placePublicId;
    private String placeName;
    private String placeBranchName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
