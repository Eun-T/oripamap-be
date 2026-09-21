package org.scoula.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RecentCommentPageResponse {

    private List<RecentCommentResponse> comments;
    private String nextCursor;
    private boolean hasNext;
}
