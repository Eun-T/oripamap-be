package org.scoula.inquiry.dto;

import lombok.Getter;
import lombok.Setter;
import org.scoula.inquiry.vo.InquiryType;

@Getter
@Setter
public class InquiryRequest {

    private InquiryType type;
    private String title;
    private String content;
}
