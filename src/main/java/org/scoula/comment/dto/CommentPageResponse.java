package org.scoula.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CommentPageResponse {

    private List<CommentResponse> comments;
    private int page;
    private int size;
    private boolean hasNext;
    private long totalCount;
}
