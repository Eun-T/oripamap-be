package org.scoula.security.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.security.account.domain.MemberVO;
import org.scoula.security.util.JwtCookieUtil;
import org.scoula.security.util.JwtProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth/kakao")
@RequiredArgsConstructor
public class KakaoAuthController {

    private static final String AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProcessor jwtProcessor;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${kakao.rest-api-key}")
    private String clientId;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${frontend.redirect-uri:http://localhost:5173}")
    private String frontendRedirectUri;

    @GetMapping
    public ResponseEntity<Void> login(HttpServletResponse response) {
        byte[] stateBytes = new byte[32];
        SECURE_RANDOM.nextBytes(stateBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);

        response.addHeader(HttpHeaders.SET_COOKIE,
                "kakao_oauth_state=" + state
                        + "; Path=/api/auth/kakao; Max-Age=300; HttpOnly; SameSite=Lax");

        URI location = UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .encode()
                .toUri();

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(location)
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String code,
            @RequestParam String state,
            @CookieValue(value = "kakao_oauth_state", required = false) String savedState,
            HttpServletResponse response) {
        if (savedState == null || !savedState.equals(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 카카오 로그인 요청입니다.");
        }

        response.addHeader(HttpHeaders.SET_COOKIE,
                "kakao_oauth_state=; Path=/api/auth/kakao; Max-Age=0; HttpOnly; SameSite=Lax");

        try {
            String accessToken = requestAccessToken(code);
            JsonNode kakaoUser = requestUserInfo(accessToken);
            String providerId = kakaoUser.path("id").asText();
            if (providerId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 사용자 ID를 받지 못했습니다.");
            }

            MemberVO member = memberMapper.findByProvider("KAKAO", providerId);
            if (member == null) {
                JsonNode account = kakaoUser.path("kakao_account");
                String email = account.path("email").asText();
                if (email.isBlank()) {
                    email = "kakao_" + providerId + "@kakao.local";
                }
                String nickname = account.path("profile").path("nickname").asText();
                if (nickname.isBlank()) {
                    nickname = "카카오사용자";
                }

                member = MemberVO.builder()
                        .email(email)
                        .username(email)
                        .password(passwordEncoder.encode(randomValue()))
                        .nickname(nickname)
                        .provider("KAKAO")
                        .providerId(providerId)
                        .build();
                memberMapper.insertSocial(member);
                member = memberMapper.findByProvider("KAKAO", providerId);
            }

            String jwt = jwtProcessor.generateToken(member.getId());
            JwtCookieUtil.addAccessTokenCookie(response, jwt);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendRedirectUri))
                    .build();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "카카오 API 오류: " + e.getResponseBodyAsString(),
                    e);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 로그인 처리에 실패했습니다.", e);
        }
    }

    private String requestAccessToken(String code) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        String body;
        try {
            body = restTemplate.postForObject(TOKEN_URL, new HttpEntity<>(form, headers), String.class);
        } catch (HttpStatusCodeException e) {
            throw kakaoApiException("토큰 발급", e);
        }
        String accessToken = objectMapper.readTree(body).path("access_token").asText();
        if (accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "카카오 액세스 토큰을 받지 못했습니다.");
        }
        return accessToken;
    }

    private JsonNode requestUserInfo(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        String body;
        try {
            body = restTemplate.postForObject(USER_INFO_URL, new HttpEntity<>(headers), String.class);
        } catch (HttpStatusCodeException e) {
            throw kakaoApiException("사용자 정보 조회", e);
        }
        return objectMapper.readTree(body);
    }

    private ResponseStatusException kakaoApiException(String stage, HttpStatusCodeException e) {
        String detail = e.getResponseBodyAsString();
        if (detail == null || detail.isBlank()) {
            detail = "응답 본문 없음";
        }
        return new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "카카오 " + stage + " 실패 (HTTP " + e.getRawStatusCode() + "): " + detail,
                e);
    }

    private String randomValue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
