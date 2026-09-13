package org.scoula.inquiry.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class InquiryVO {

    private Long id;
    private Long userId;
    private InquiryType type;
    private String title;
    private String content;
    private InquiryStatus status;
    private String answer;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
