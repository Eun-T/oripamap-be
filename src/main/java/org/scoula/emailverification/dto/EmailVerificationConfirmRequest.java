package org.scoula.emailverification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmailVerificationConfirmRequest {
    private String email;
    private String code;
}
