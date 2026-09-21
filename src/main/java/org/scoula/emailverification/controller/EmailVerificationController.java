package org.scoula.emailverification.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.emailverification.dto.EmailVerificationConfirmRequest;
import org.scoula.emailverification.dto.EmailVerificationRequest;
import org.scoula.emailverification.service.EmailVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {
    private final EmailVerificationService emailVerificationService;

    @PostMapping
    public ResponseEntity<Void> send(@RequestBody EmailVerificationRequest request) {
        emailVerificationService.send(request.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verify(@RequestBody EmailVerificationConfirmRequest request) {
        emailVerificationService.verify(request.getEmail(), request.getCode());
        return ResponseEntity.noContent().build();
    }
}
