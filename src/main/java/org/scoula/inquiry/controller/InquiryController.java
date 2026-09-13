package org.scoula.inquiry.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.inquiry.dto.InquiryRequest;
import org.scoula.inquiry.dto.InquiryResponse;
import org.scoula.inquiry.dto.InquiryListResponse;
import org.scoula.inquiry.dto.InquiryDetailResponse;
import org.scoula.inquiry.service.InquiryService;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<InquiryResponse> create(
            @RequestBody InquiryRequest request,
            @AuthenticationPrincipal CustomUser user
    ) {
        InquiryResponse response = inquiryService.create(request, user.getMember().getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    public List<InquiryListResponse> getMine(@AuthenticationPrincipal CustomUser user) {
        return inquiryService.getMine(user.getMember().getId());
    }

    @GetMapping("/{id}")
    public InquiryDetailResponse getMine(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUser user
    ) {
        return inquiryService.getMine(id, user.getMember().getId());
    }
}
