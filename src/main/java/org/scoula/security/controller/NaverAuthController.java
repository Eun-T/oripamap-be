package org.scoula.security.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.scoula.member.mapper.MemberMapper;
import org.scoula.security.account.domain.MemberVO;
import org.scoula.security.service.SocialAccountRegistrationService;
import org.scoula.security.util.JwtCookieUtil;
import org.scoula.security.util.JwtProcessor;
import org.scoula.security.util.OAuthStateCookieUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth/naver")
@RequiredArgsConstructor
@Log4j2
public class NaverAuthController {

    private static final String AUTHORIZE_URL = "https://nid.naver.com/oauth2.0/authorize";
    private static final String TOKEN_URL = "https://nid.naver.com/oauth2.0/token";
    private static final String USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProcessor jwtProcessor;
    private final JwtCookieUtil jwtCookieUtil;
    private final SocialAccountRegistrationService socialAccountRegistrationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    @Value("${naver.redirect-uri}")
    private String redirectUri;

    @Value("${frontend.redirect-uri:http://localhost:5173}")
    private String frontendRedirectUri;

    @Value("${oauth.state-cookie.secure:false}")
    private boolean stateCookieSecure;

    @GetMapping
    public ResponseEntity<Void> login(HttpServletResponse response) {
        String state = randomValue();

        OAuthStateCookieUtil.addStateCookie(
                response,
                "naver_oauth_state",
                state,
                "/api/auth/naver",
                stateCookieSecure);

        URI location = UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
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
            @CookieValue(value = "naver_oauth_state", required = false) String savedState,
            HttpServletResponse response) {
        if (savedState == null || !savedState.equals(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 네이버 로그인 요청입니다.");
        }

        OAuthStateCookieUtil.deleteStateCookie(
                response,
                "naver_oauth_state",
                "/api/auth/naver",
                stateCookieSecure);

        try {
            String accessToken = requestAccessToken(code, state);
            JsonNode naverUser = requestUserInfo(accessToken).path("response");
            String providerId = naverUser.path("id").asText();
            if (providerId.isBlank()) {
                log.warn("네이버 사용자 정보 응답에 사용자 ID가 없습니다.");
                throw naverLoginException(null);
            }

            MemberVO member = memberMapper.findByProvider("NAVER", providerId);
            if (member == null) {
                String email = naverUser.path("email").asText();
                if (email.isBlank()) {
                    email = "naver_" + providerId + "@naver.local";
                }
                String nickname = naverUser.path("nickname").asText();
                if (nickname.isBlank()) {
                    nickname = naverUser.path("name").asText();
                }
                if (nickname.isBlank()) {
                    nickname = "네이버사용자";
                }

                member = MemberVO.builder()
                        .email(email)
                        .username(email)
                        .password(passwordEncoder.encode(randomValue()))
                        .nickname(nickname)
                        .provider("NAVER")
                        .providerId(providerId)
                        .build();
                member = socialAccountRegistrationService.insertOrGetExisting(member);
            }

            String jwt = jwtProcessor.generateToken(member.getId());
            jwtCookieUtil.addAccessTokenCookie(response, jwt);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(frontendRedirectUri))
                    .build();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            logNaverApiError("로그인 처리", e);
            throw naverLoginException(e);
        } catch (Exception e) {
            log.error("네이버 로그인 처리 중 오류가 발생했습니다.", e);
            throw naverLoginException(e);
        }
    }

    private String requestAccessToken(String code, String state) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("state", state);

        String body;
        try {
            body = restTemplate.postForObject(TOKEN_URL, new HttpEntity<>(form, headers), String.class);
        } catch (HttpStatusCodeException e) {
            throw naverApiException("토큰 발급", e);
        }
        JsonNode tokenResponse = objectMapper.readTree(body);
        String accessToken = tokenResponse.path("access_token").asText();
        if (accessToken.isBlank()) {
            String errorDescription = tokenResponse.path("error_description").asText();
            log.warn("네이버 토큰 응답에 access_token이 없습니다: 오류={}",
                    errorDescription.isBlank() ? "상세 정보 없음" : errorDescription);
            throw naverLoginException(null);
        }
        return accessToken;
    }

    private JsonNode requestUserInfo(String accessToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        String body;
        try {
            body = restTemplate.exchange(
                    USER_INFO_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class).getBody();
        } catch (HttpStatusCodeException e) {
            throw naverApiException("사용자 정보 조회", e);
        }

        JsonNode result = objectMapper.readTree(body);
        if (!"00".equals(result.path("resultcode").asText())) {
            log.warn("네이버 사용자 정보 조회 실패: resultcode={}, 메시지={}",
                    result.path("resultcode").asText(),
                    result.path("message").asText());
            throw naverLoginException(null);
        }
        return result;
    }

    private ResponseStatusException naverApiException(String stage, HttpStatusCodeException e) {
        logNaverApiError(stage, e);
        return naverLoginException(e);
    }

    private void logNaverApiError(String stage, HttpStatusCodeException e) {
        String detail = e.getResponseBodyAsString();
        if (detail == null || detail.isBlank()) {
            detail = "응답 본문 없음";
        }
        log.warn("네이버 {} 실패: HTTP {}, 응답={}", stage, e.getRawStatusCode(), detail);
    }

    private ResponseStatusException naverLoginException(Exception cause) {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "네이버 로그인에 실패했습니다.", cause);
    }

    private String randomValue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
