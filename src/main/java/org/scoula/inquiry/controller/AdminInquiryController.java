package org.scoula.inquiry.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.inquiry.dto.InquiryAnswerRequest;
import org.scoula.inquiry.service.InquiryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @PutMapping("/{id}/answer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void answer(@PathVariable Long id, @RequestBody InquiryAnswerRequest request) {
        inquiryService.answer(id, request);
    }
}
