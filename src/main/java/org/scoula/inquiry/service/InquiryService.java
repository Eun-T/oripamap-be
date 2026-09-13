package org.scoula.inquiry.service;

import lombok.RequiredArgsConstructor;
import org.scoula.inquiry.dto.InquiryRequest;
import org.scoula.inquiry.dto.InquiryResponse;
import org.scoula.inquiry.dto.InquiryListResponse;
import org.scoula.inquiry.dto.InquiryDetailResponse;
import org.scoula.inquiry.dto.InquiryAnswerRequest;
import org.scoula.inquiry.mapper.InquiryMapper;
import org.scoula.inquiry.vo.InquiryStatus;
import org.scoula.inquiry.vo.InquiryVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int MAX_ANSWER_LENGTH = 2000;

    private final InquiryMapper inquiryMapper;

    @Transactional
    public InquiryResponse create(InquiryRequest request, Long userId) {
        validate(request);

        InquiryVO inquiry = new InquiryVO();
        inquiry.setUserId(userId);
        inquiry.setType(request.getType());
        inquiry.setTitle(request.getTitle().trim());
        inquiry.setContent(request.getContent().trim());
        inquiry.setStatus(InquiryStatus.PENDING);

        if (inquiryMapper.insert(inquiry) != 1 || inquiry.getId() == null) {
            throw new IllegalStateException("문의 저장에 실패했습니다.");
        }

        InquiryVO saved = inquiryMapper.findById(inquiry.getId());
        if (saved == null) {
            throw new IllegalStateException("생성된 문의를 조회할 수 없습니다.");
        }

        return InquiryResponse.builder()
                .id(saved.getId())
                .type(saved.getType())
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<InquiryListResponse> getMine(Long userId) {
        return inquiryMapper.findAllByUserId(userId).stream()
                .map(inquiry -> InquiryListResponse.builder()
                        .id(inquiry.getId())
                        .type(inquiry.getType())
                        .title(inquiry.getTitle())
                        .status(inquiry.getStatus())
                        .createdAt(inquiry.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public InquiryDetailResponse getMine(Long id, Long userId) {
        InquiryVO inquiry = inquiryMapper.findByIdAndUserId(id, userId);
        if (inquiry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다.");
        }

        return InquiryDetailResponse.builder()
                .id(inquiry.getId())
                .type(inquiry.getType())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .status(inquiry.getStatus())
                .answer(inquiry.getAnswer())
                .createdAt(inquiry.getCreatedAt())
                .answeredAt(inquiry.getAnsweredAt())
                .build();
    }

    @Transactional
    public void answer(Long id, InquiryAnswerRequest request) {
        validateAnswer(request);

        if (inquiryMapper.findById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다.");
        }
        if (inquiryMapper.updateAnswer(id, request.getAnswer().trim()) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다.");
        }
    }

    private void validateAnswer(InquiryAnswerRequest request) {
        if (request == null || request.getAnswer() == null || request.getAnswer().isBlank()) {
            throw badRequest("답변 내용을 입력해 주세요.");
        }
        if (length(request.getAnswer()) > MAX_ANSWER_LENGTH) {
            throw badRequest("답변은 2000자 이하로 입력해 주세요.");
        }
    }

    private void validate(InquiryRequest request) {
        if (request == null) {
            throw badRequest("문의 내용을 입력해 주세요.");
        }
        if (request.getType() == null) {
            throw badRequest("문의 유형을 선택해 주세요.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw badRequest("문의 제목을 입력해 주세요.");
        }
        if (length(request.getTitle()) > MAX_TITLE_LENGTH) {
            throw badRequest("문의 제목은 100자 이하로 입력해 주세요.");
        }
        if (request.getContent() == null || request.getContent().isBlank()) {
            throw badRequest("문의 내용을 입력해 주세요.");
        }
        if (length(request.getContent()) > MAX_CONTENT_LENGTH) {
            throw badRequest("문의 내용은 2000자 이하로 입력해 주세요.");
        }
    }

    private int length(String value) {
        return value.codePointCount(0, value.length());
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
