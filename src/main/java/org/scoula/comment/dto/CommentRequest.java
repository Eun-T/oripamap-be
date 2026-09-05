package org.scoula.comment.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequest {

    private String content;

    @JsonAnySetter
    public void rejectJsonImage(String name, Object value) {
        if (Set.of("file", "files", "image", "images", "imageKey", "imageUrl", "image_key").contains(name)) {
            throw new IllegalArgumentException("이미지는 일반 댓글의 multipart file 필드로만 전송할 수 있습니다.");
        }
    }
}
