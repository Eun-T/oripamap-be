package org.scoula.inquiry.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import org.scoula.inquiry.vo.InquiryStatus;
import org.scoula.inquiry.vo.InquiryType;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryDetailResponse {

    private Long id;
    private InquiryType type;
    private String title;
    private String content;
    private InquiryStatus status;
    private String answer;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime answeredAt;
}
