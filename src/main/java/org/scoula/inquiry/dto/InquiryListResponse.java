package org.scoula.inquiry.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;
import org.scoula.inquiry.vo.InquiryStatus;
import org.scoula.inquiry.vo.InquiryType;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryListResponse {

    private Long id;
    private InquiryType type;
    private String title;
    private InquiryStatus status;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
