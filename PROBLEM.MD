# -----------------------------------------------------------------------
  1. OAuth/JWT 보안 점검 및 개선 목록
# -----------------------------------------------------------------------

마지막 점검일: 2026-09-06

## 상태 기준

- `[ ]` 미해결 또는 확인 필요
- `[x]` 코드에 반영됨
- 운영 환경에 실제 적용됐는지는 코드와 별도로 확인한다.

## 우선순위 요약

| 우선순위 | 항목 | 현재 상태 |
| --- | --- | --- |
| P0 | JWT 서명키를 환경변수로 분리 | 로컬 완료, 운영 배포 시 별도 키 필요 |
| P1 | 운영 쿠키에 `Secure` 적용 | 코드 반영, 운영 환경변수 설정 필요 |
| P1 | 외부 OAuth API 상세 오류 노출 방지 | 코드 반영됨 |
| P1 | 운영 DB의 소셜 계정 UNIQUE 제약 확인 | 스크립트 존재, 적용 여부 확인 필요 |
| P2 | 소셜 계정 중복 및 계정 연결 정책 확정 | 정책 결정 필요 |
| P2 | 소셜 회원의 일반 비밀번호 로그인 차단 | 코드 반영됨 |
| P3 | 카카오·네이버 PKCE 지원 및 적용 검토 | 조사 필요 |

## 1. JWT 서명키 하드코딩 제거

### 현재 상태

- [x] `JwtProcessor`의 하드코딩된 JWT 서명키를 제거했다.
- [x] Base64 디코딩 후 32바이트 이상의 키만 허용한다.
- [x] `JWT_SECRET`이 없거나 형식이 잘못되면 서버 시작이 실패한다.
- [x] 로컬 Windows 사용자 환경변수에 무작위 `JWT_SECRET`을 설정했다.
- [ ] 운영 배포 시 로컬과 다른 `JWT_SECRET`을 설정해야 한다.
- 액세스 토큰 유효시간은 30분이다.

### 문제점

- 소스 저장소가 노출되면 누구나 정상 형식의 JWT를 위조할 수 있다.
- 개발·운영 환경이 같은 키를 공유할 가능성이 있다.
- 키 교체가 어렵다.

### 개선 방향

- 충분히 긴 무작위 키를 톰캣 환경변수(예: `JWT_SECRET`)로 주입한다.
- 환경변수가 없으면 운영 서버가 시작되지 않도록 한다.
- 운영 키는 저장소나 로그에 남기지 않는다.
- 키 교체 시 기존 JWT가 모두 무효화된다는 점을 배포 계획에 반영한다.

## 2. OAuth state 쿠키 보안

### 현재 상태

- [x] `SecureRandom`으로 state를 생성한다.
- [x] `HttpOnly`와 `SameSite=Lax`를 적용한다.
- [x] 유효시간을 5분으로 제한한다.
- [x] 콜백에서 state를 비교한 후 쿠키를 삭제한다.
- [x] `OAUTH_STATE_COOKIE_SECURE` 설정으로 운영 환경에 `Secure`를 적용할 수 있다.

### 개선 방향

- 로컬 HTTP 환경에서는 `Secure=false`를 사용한다.
- 운영 HTTPS 환경에서는 반드시 `Secure=true`를 사용한다.
- 개발·운영 프로필 또는 환경변수로 쿠키 설정을 분리한다.
- 카카오와 네이버 state 쿠키에 동일한 정책을 적용한다.

### 운영 환경 필수 설정

운영 HTTPS 서버에는 아래 환경변수를 반드시 설정한다.

```text
OAUTH_STATE_COOKIE_SECURE=true
```

이 환경변수가 없으면 기본값이 `false`이므로 OAuth state 쿠키에 `Secure`가 적용되지 않는다.

## 3. JWT 쿠키 설정

### 현재 상태

`JwtCookieUtil` 기준 설정은 다음과 같다.

| 설정 | 현재 값 | 판단 |
| --- | --- | --- |
| 이름 | `accessToken` | 확인 완료 |
| `HttpOnly` | `true` | 적절함 |
| `Secure` | 환경변수로 결정 | 운영 환경에서 `true` 필요 |
| `SameSite` | `Lax` | 현재 OAuth 리다이렉트 흐름에 사용 가능 |
| `Path` | `/` | 전체 API 요청에 전달됨 |
| `Max-Age` | 기본 30분, 로그인 유지 시 7일 | JWT 만료시간과 동일 |

### 개선 방향

- [x] `JWT_COOKIE_SECURE` 설정으로 운영 HTTPS 환경에 `Secure=true`를 적용할 수 있다.
- [ ] 프론트엔드와 API가 서로 다른 사이트가 되는 배포 구조라면 `SameSite=None; Secure` 필요 여부를 확인한다.
- [x] 로그아웃 시 `Path`, `SameSite`, `Secure`가 발급 쿠키와 동일한 조건으로 삭제된다.

### 운영 환경 필수 설정

운영 HTTPS 서버에는 아래 환경변수를 반드시 설정한다.

```text
JWT_COOKIE_SECURE=true
```

이 환경변수가 없으면 기본값이 `false`이므로 JWT 쿠키에 `Secure`가 적용되지 않는다.

## 4. 외부 OAuth API 오류 정보 노출 방지

### 현재 상태

- [x] 카카오와 네이버 API의 상세 오류는 서버 로그에만 기록한다.
- [x] 클라이언트에는 공급자별 고정 오류 메시지만 반환한다.

### 문제점

- 외부 API의 상세 응답, 식별자 또는 디버깅 정보가 클라이언트에 노출될 수 있다.
- 운영 환경의 오류 응답 정책이 Spring 설정에 따라 달라질 수 있다.

### 개선 방향

- 상세 응답과 HTTP 상태는 서버 로그에만 기록한다.
- 클라이언트에는 아래처럼 고정된 메시지만 반환한다.
  - `카카오 로그인에 실패했습니다.`
  - `네이버 로그인에 실패했습니다.`
- 로그에도 액세스 토큰, client secret, 인가 코드를 기록하지 않는다.

## 5. 소셜 계정 식별 및 중복 정책

### 기본 정책

- [x] 소셜 회원의 기본 식별자는 `(provider, providerId)`를 사용한다.
- [x] 이메일이 같다는 이유만으로 LOCAL·KAKAO·NAVER 계정을 자동 병합하지 않는다.

예를 들어 아래 계정들은 이메일이 같아도 기본적으로 서로 다른 계정이다.

```text
LOCAL  / abc@gmail.com
KAKAO  / abc@gmail.com
NAVER  / abc@gmail.com
```

### 결정할 사항

- [ ] 동일 이메일의 여러 계정을 사용자에게 어떻게 보여줄지 정한다.
- [ ] 하나의 계정으로 묶어야 한다면, 로그인된 사용자가 추가 인증을 거치는 별도의 `계정 연결` 기능으로 구현한다.
- [ ] 이메일 제공 동의를 하지 않은 사용자에게 생성하는 대체 이메일의 사용 범위를 정한다.

## 6. DB의 소셜 계정 중복 생성 방지

### 현재 상태

- [x] `004_constraints.sql`에 `(provider, provider_id)` UNIQUE 제약이 정의되어 있다.
- [x] 로컬 DB에 `uk_users_provider(provider, provider_id)` 제약이 적용된 것을 확인했다.
- [ ] 운영 DB에 해당 제약이 실제 적용되었는지는 배포 시 확인해야 한다.
- [x] 동시 가입의 중복 키 예외가 발생하면 동일 소셜 회원을 다시 조회해 로그인을 계속한다.

### 문제점

현재 로그인 처리는 `findByProvider → insertSocial` 순서다. 동시에 같은 계정으로 로그인하면 두 요청이 모두 조회를 통과할 수 있으므로 애플리케이션 조회만으로는 중복 생성을 완전히 막을 수 없다.

### 개선 방향

- [x] 로컬 DB에서 `uk_users_provider` 제약 존재 여부를 확인했다.
- [ ] 운영 DB에서 `uk_users_provider` 제약 존재 여부를 확인한다.
- [x] 중복 키 예외가 발생하면 기존 회원을 다시 조회해 로그인을 계속하도록 처리한다.

확인 대상 제약:

```sql
UNIQUE (provider, provider_id)
```

## 7. 소셜 회원의 일반 비밀번호 로그인 차단

### 현재 상태

- [x] 소셜 가입 시 예측할 수 없는 랜덤 비밀번호를 해시하여 저장한다.
- [x] 일반 로그인 사용자 조회에 `provider = 'LOCAL'` 조건이 적용되어 있다.
- [x] 따라서 KAKAO·NAVER 회원은 이메일과 비밀번호 방식으로 로그인할 수 없다.

### 유지할 정책

- 소셜 회원이 별도로 비밀번호 설정 및 본인 확인 절차를 완료하지 않았다면 일반 로그인을 허용하지 않는다.
- 이후 쿼리나 인증 로직을 수정할 때 `provider = 'LOCAL'` 조건이 제거되지 않도록 테스트한다.

## 8. 카카오·네이버 OAuth 보안 정책 통일

두 로그인 구현에 아래 항목이 동일하게 적용되어야 한다.

- [x] 무작위 state 생성
- [x] state 쿠키 `HttpOnly`, `SameSite=Lax`, 5분 만료
- [x] 콜백에서 state 검증 및 사용 후 삭제
- [x] `(provider, providerId)`로 회원 조회
- [x] JWT를 `HttpOnly` 쿠키로 발급
- [x] 운영 환경의 state 쿠키에 `Secure` 적용 가능 (`OAUTH_STATE_COOKIE_SECURE=true` 필요)
- [x] 운영 환경의 JWT 쿠키에 `Secure` 적용 가능 (`JWT_COOKIE_SECURE=true` 필요)
- [x] 외부 API 상세 오류를 서버 로그로만 기록
- [ ] 운영 DB UNIQUE 제약의 실제 적용 확인
- [x] 동시 가입 요청의 중복 키 예외 처리

## 9. PKCE 지원 여부 확인

- [ ] 현재 사용하는 카카오·네이버 Authorization Code 흐름에서 PKCE를 지원하는지 공식 문서로 확인한다.
- [ ] 지원한다면 `code_verifier`와 `code_challenge` 적용을 검토한다.
- 우선순위는 JWT 서명키, 쿠키 `Secure`, 오류 정보 노출, DB UNIQUE 적용 확인보다 낮다.

## 권장 처리 순서

1. [x] JWT 서명키를 환경변수로 이동하고 로컬 `JWT_SECRET` 설정
2. [x] 개발·운영별 OAuth state/JWT 쿠키 `Secure` 설정 분리
3. [x] 카카오·네이버 API 상세 오류 노출 제거
4. [ ] 운영 DB의 `(provider, provider_id)` UNIQUE 제약 확인
5. [ ] 동시 소셜 가입의 중복 키 예외 처리
6. [ ] 계정 중복 및 계정 연결 정책 확정
7. [ ] PKCE 지원 여부 조사 및 적용 검토

----------------------------------------------------
2. 로그인 보안 이슈
------------------------------------------------------

지금 상태에서 진짜 먼저 할 것만 추리면 이거야:

1. 이메일 UNIQUE 제약 추가
애플리케이션 중복체크만으로는 동시 가입 중복 완전히 못 막음
DB에서 UNIQUE(email)로 최종 방어
중복키 예외 처리까지 하면 끝

2. CSRF 대책
너는 JWT를 쿠키로도 쓰니까 중요함
지금 csrf().disable()이면 최소한 SameSite 정책 + 실제 프론트/백 도메인 구조 기준으로 다시 검토해야 함
가능하면 CSRF 토큰 방식도 고려

3. Refresh Token / 토큰 폐기
지금 rememberMe가 사실상 “7일짜리 access token”
토큰 탈취 시 7일 동안 유효할 수 있음
비밀번호 바꿔도 기존 토큰이 살아있는 문제 있음
운영 로그인 제대로 잡으려면 이게 꽤 중요함

4. 로그인 rate limit
무차별 대입 방지
계정/IP 기준 시도 제한, 지연, 잠금 같은 방어
이건 공격 대응 쪽

5. HTTPS + 운영 쿠키/CORS
EC2 배포 후 https
JWT_COOKIE_SECURE=true
OAUTH_STATE_COOKIE_SECURE=true
CORS는 실제 프론트 도메인만 허용

6. OAuth 에러 로그 마스킹
응답 body 전체 로그는 위험할 수 있음
민감 필드 마스킹하거나 필요한 필드만 기록

7. 다음 기능은 아직 없습니다. 
- PKCE 
- 비밀번호 재설정 토큰 
- 이메일 인증 
- 약관 동의 이력 
- soft delete와 탈퇴 후 재가입 정책
- 의심 로그인 알림
- 중요 정보 변경 시 재인증 정책
- 관리자/일반 사용자 권한 분리
- 기기별 로그인 관리
- 명시적인 JWT 감사 로그 및 민감정보 마스킹 필터

✅ BCrypt
✅ JWT Secret 환경변수
✅ SecurityConfig 화이트리스트
✅ 타 사용자 수정 차단
✅ email UNIQUE
✅ Refresh Token + rotation
✅ 로그아웃 시 Refresh 폐기
✅ 비밀번호 변경 시 Refresh 전체 폐기
✅ 전체 테스트 통과

----------------------------------------------------
3. Capacitor 안드로이드 스튜디오 설정
------------------------------------------------------

config.addAllowedOrigin("http://localhost"); // Capacitor Android

public UrlBasedCorsConfigurationSource corsConfigurationSource()

http.cors()
.configurationSource(corsConfigurationSource());


-------------------------------------------------------
4. 
--------------------------------------------------------
# 2026-09-13 트러블슈팅 정리

오늘 모바일 Safari / Android 앱에서 로그인·로그아웃 테스트 중 인증 관련 문제를 몇 가지 해결했다.

---

## 1. 모바일 Safari에서 로그인 실패

### 증상

아이폰 Safari에서 다음 주소로 접속:

http://192.168.50.146:5173

로그인 시:

- "로그인 중 오류가 발생했습니다."
- POST /api/auth/login → 403 Forbidden

처음에는 Safari 쿠키 정책이나 OAuth 문제로 의심했다.

PC Chrome에서도 동일하게:

http://192.168.50.146:5173

로 접속해 로그인해보니 똑같이 403이 발생했다.

응답 내용을 확인해보니:

Invalid CORS request

가 반환되고 있었다.

### 원인

백엔드 CORS 허용 Origin은 기존에:

- http://localhost:5173
- http://localhost
- https://oripamap.com
- https://www.oripamap.com

만 허용하고 있었다.

하지만 모바일/PC에서 실제 접속한 Origin은:

http://192.168.50.146:5173

이었기 때문에 Spring CORS에서 요청을 거부하고 있었다.

즉 Safari 문제가 아니라 CORS 허용 Origin 누락 문제였다.

### 해결

SecurityConfig의 CORS 설정에 개발용 IP Origin 추가:

config.addAllowedOrigin("http://192.168.50.146:5173");

기존 Origin은 그대로 유지했다.

### 결과

POST /api/auth/login의 403 Invalid CORS request가 사라졌고,
아이폰 Safari에서도 정상 로그인 성공.

### 배운 점

localhost와 PC의 LAN IP는 브라우저 기준 서로 다른 Origin이다.

따라서 같은 PC에서 실행하는 Vite라도:

http://localhost:5173

과

http://192.168.50.146:5173

은 CORS에서 별도로 허용해야 한다.

---

## 2. 로그아웃 버튼을 눌렀는데 다시 로그인되는 문제

### 증상

로그아웃 버튼을 누르면 UI상으로는 로그아웃된 것처럼 보였다.

하지만 새로고침하거나 사용자 정보를 다시 조회하면 다시 로그인 상태로 돌아왔다.

Network를 확인해보니:

POST /api/auth/logout
→ 403 Forbidden

즉 실제 서버 로그아웃은 실패하고 있었다.

### 프론트 기존 코드

const logout = async () => {
try {
await api.post('/api/auth/logout')
} finally {
user.value = null
}
}

### 원인

finally는 API 성공/실패 여부와 상관없이 항상 실행된다.

따라서:

1. 서버 로그아웃 요청 → 403 실패
2. 그래도 finally 실행
3. user.value = null
4. 화면에서는 로그아웃된 것처럼 표시
5. 서버의 JWT/refresh 쿠키는 그대로 살아 있음
6. 다시 /api/users/me 등을 호출하면 로그인 상태 복구

즉 서버와 프론트 인증 상태가 서로 달라진 것이었다.

### 해결 방향

서버 로그아웃 성공 후에만 사용자 상태를 초기화하도록 변경:

const logout = async () => {
await api.post('/api/auth/logout')
user.value = null
}

### 배운 점

로그아웃처럼 서버 상태가 중요한 작업은 실패했는데도 클라이언트 상태를 무조건 초기화하면 안 된다.

---

## 3. 로그아웃 POST가 403 발생

### 증상

POST /api/auth/logout
→ 403 Forbidden

응답:

"권한이 부족합니다."

처음에는 OWNER / USER 같은 role 권한 문제로 보였다.

### 백엔드 확인 결과

/api/auth/logout은:

- permitAll()
- JWT 인증 필터에서도 logout 경로 제외
- refresh 쿠키가 없어도 controller 처리 가능
- AuthController에 진입하면 204 No Content 반환

즉 실제 권한 문제는 아니었다.

### 원인

Spring Security의 CSRF 검증 때문이었다.

기존 설정:

.csrf()
.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
.ignoringAntMatchers(
"/api/auth/login",
"/api/member"
)

logout은 CSRF 예외가 아니었다.

Spring은 POST 요청에서 기본적으로:

- XSRF-TOKEN 쿠키
- X-XSRF-TOKEN 요청 헤더

값이 일치하는지 확인한다.

둘 중 하나가 없거나 값이 다르면 CsrfFilter 단계에서 403을 반환한다.

permitAll()은 인가를 허용하는 설정이지 CSRF 검사를 제거하는 설정은 아니다.

따라서 permitAll()이어도 CSRF 검증에 실패하면 403이 발생할 수 있다.

---

## 4. CSRF가 무엇인지

CSRF는 다른 사이트가 사용자의 로그인 쿠키를 이용해서
사용자 몰래 상태 변경 요청을 보내는 공격이다.

예:

오맵 로그인 상태
→ 브라우저에 인증 쿠키 존재

악성 사이트 방문
→ 악성 사이트가 POST /api/auth/logout 요청 전송
→ 브라우저가 오맵 쿠키를 자동으로 붙일 수 있음
→ 서버가 정상 사용자 요청으로 착각할 수 있음

이런 공격을 막기 위해 서버는 인증 쿠키만 확인하지 않고
추가로 CSRF 토큰도 요구한다.

정상 요청:

인증 쿠키
+
CSRF 토큰
→ 서버 허용

수상한 요청:

인증 쿠키
+
CSRF 토큰 없음
→ 403

---

## 5. Android 앱에서 logout 403 발생

### 현재 앱 구조

Android Capacitor 앱에서는:

VITE_API_BASE_URL=https://next-bali-charged-strength.trycloudflare.com

을 사용해서 API를 직접 호출한다.

Axios 설정:

const api = axios.create({
baseURL: import.meta.env.VITE_API_BASE_URL || '',
timeout: 10000,
withCredentials: true,
withXSRFToken: true,
xsrfCookieName: 'XSRF-TOKEN',
xsrfHeaderName: 'X-XSRF-TOKEN',
})

Android 모드에서는:

if (import.meta.env.MODE === 'android') {
api.defaults.headers.common['X-Client-Type'] = 'APP'
}

를 사용한다.

### 문제

Capacitor WebView 문서 Origin은 보통:

http://localhost

API 도메인은:

https://next-bali-charged-strength.trycloudflare.com

이다.

API 도메인의 XSRF-TOKEN 쿠키를
http://localhost에서 실행되는 WebView JavaScript가 읽을 수 없다.

withCredentials: true는 API 요청에 쿠키를 첨부할 수 있게 해줄 뿐,
다른 도메인의 쿠키를 JavaScript가 읽게 해주는 기능은 아니다.

따라서 Axios가 정상적인 X-XSRF-TOKEN 헤더를 만들 수 없었고,
Spring CSRF 검사에서 403이 발생했다.

---

## 6. "권한이 부족합니다."인데 실제 권한 문제가 아니었던 이유

CSRF 검증 실패도 AccessDeniedException 계열로 처리된다.

현재 CustomAccessDeniedHandler는 AccessDeniedException 발생 시 모두:

403
"권한이 부족합니다."

를 반환하고 있었다.

그래서 실제 요청 흐름은:

POST /api/auth/logout
→ CsrfFilter
→ CSRF 검증 실패
→ CustomAccessDeniedHandler
→ 403 "권한이 부족합니다."

였다.

즉 403 응답 문구만 보고 role 문제라고 판단하면 안 됐다.

이를 구분하기 쉽게 CustomAccessDeniedHandler 로그에
예외 클래스명도 기록하도록 수정했다.

---

## 7. Android APP 요청만 조건부 CSRF 제외

### 목표

CSRF를 전체적으로 끄지 않고:

- 일반 웹 요청 → 기존 CSRF 보호 유지
- Android 앱의 logout / refresh → 정상 작동

하도록 처리했다.

### 해결

다음 조건을 모두 만족할 때만 CSRF 검증을 제외하도록 전용 RequestMatcher를 추가했다.

- HTTP Method = POST
- Path = /api/auth/logout 또는 /api/auth/refresh
- Header = X-Client-Type: APP

조건:

POST
AND
(/api/auth/logout OR /api/auth/refresh)
AND
X-Client-Type: APP

### 수정 파일

- src/main/java/org/scoula/security/config/AppAuthCsrfRequestMatcher.java
- src/main/java/org/scoula/security/config/SecurityConfig.java
- src/main/java/org/scoula/security/handler/CustomAccessDeniedHandler.java
- src/test/java/org/scoula/security/config/AppAuthCsrfRequestMatcherTest.java

### 왜 logout / refresh 전체를 CSRF 예외로 만들지 않았는가?

단순히:

ignoringAntMatchers(
"/api/auth/logout",
"/api/auth/refresh"
)

처럼 처리하면 일반 웹 요청까지 CSRF 보호가 사라진다.

그래서 앱에서만 보내는:

X-Client-Type: APP

헤더까지 함께 검사하도록 했다.

웹 요청은 APP 헤더가 없으므로 기존 CSRF 검증을 그대로 받는다.

CORS allowedOrigins도 *로 열지 않고 기존 명시 목록을 유지했다.

### 결과

Android 앱에서:

POST /api/auth/logout
→ 204 No Content

정상 로그아웃 성공.

refresh 요청도 Android APP 조건에서는 동일한 정책을 사용한다.

---

## 8. 웹 개발과 Android 빌드 환경 차이

웹 개발:

npm run dev

- Vite 개발 서버 실행
- 기본 mode = development
- .env.development 사용
- 코드 수정 시 HMR로 자동 반영

Android 빌드:

npm run build -- --mode android

- Vite 개발 서버를 실행하는 명령이 아님
- Android 앱에 들어갈 dist 결과물을 빌드
- mode = android
- .env.android 사용

Android용 환경변수 예:

VITE_API_BASE_URL=https://next-bali-charged-strength.trycloudflare.com

즉 웹 개발과 Android 앱은 같은 Vue 코드를 사용하지만
API 주소나 요청 환경은 mode별로 다르게 가져갈 수 있다.

---

# 최종 정리

오늘 해결한 핵심 문제는 크게 3개였다.

1. 모바일/PC LAN IP 로그인 403

원인:
http://192.168.50.146:5173 이 백엔드 CORS 허용 목록에 없었음

증상:
POST /api/auth/login
→ 403
→ Invalid CORS request

해결:
CORS allowedOrigins에

http://192.168.50.146:5173

추가

결과:
아이폰 Safari 로그인 성공


2. 로그아웃된 것처럼 보이는데 다시 로그인되는 문제

원인:
logout API가 403으로 실패해도 finally에서 user.value = null 실행

결과:
프론트만 로그아웃 상태
서버 인증 쿠키는 그대로 유지

해결 방향:
서버 로그아웃 성공 후에만 user.value = null 처리


3. Android 앱 logout 403

원인:
Android Capacitor WebView가 API 도메인의 XSRF-TOKEN 쿠키를 읽지 못해서
X-XSRF-TOKEN 헤더를 정상적으로 만들 수 없었음

Spring CsrfFilter에서 요청 차단

해결:
다음 조건일 때만 CSRF 검증 제외

POST
+
/api/auth/logout 또는 /api/auth/refresh
+
X-Client-Type: APP

일반 웹 요청은 기존 CSRF 검증 유지

결과:
Android 앱 logout 정상 성공
POST /api/auth/logout → 204 No Content


# 오늘 배운 점

- localhost와 LAN IP는 서로 다른 Origin이다.
- CORS 허용 목록에 정확한 Origin이 없으면 403 Invalid CORS request가 날 수 있다.
- 403이라고 무조건 role / 권한 문제는 아니다.
- permitAll()이어도 CSRF 검증은 별도로 수행된다.
- CSRF는 쿠키 기반 인증을 악용한 외부 요청을 막는 보안 장치다.
- Capacitor 앱과 일반 웹 브라우저는 쿠키 / Origin 동작이 다르다.
- withCredentials는 다른 도메인의 쿠키를 JavaScript가 읽게 해주는 옵션이 아니다.
- 앱 전용 요청을 구분하기 위해 X-Client-Type: APP 헤더를 사용했다.
- CSRF 전체를 끄는 대신 앱의 특정 요청에만 조건부 예외를 적용했다.
- 서버 요청이 실패했는데 프론트 상태만 바꾸면 서버와 클라이언트 인증 상태가 어긋날 수 있다.


ORIPA / 자판기     → 주소 + 좌표 필수
국내 OFFLINE       → 주소 + 좌표 필수
해외 OFFLINE       → 주소 필수 / 좌표 선택
ONLINE             → 위치 전부 선택


----------------------------------------------------------------------------
## 트러블슈팅 - 운영 환경 로그아웃 403 / CSRF 토큰 불일치
----------------------------------------------------------------------------

### 문제 상황

운영 환경 구조:

- Frontend: https://oripamap.com
- Backend API: https://api.oripamap.com
- 인증: JWT HttpOnly Cookie
- CSRF: Spring Security `CookieCsrfTokenRepository`
- Frontend HTTP Client: Axios

로그인은 정상적으로 성공했지만 로그아웃 요청에서 계속 `403 Forbidden` 발생.

POST /api/auth/logout
→ 403 Forbidden


### 1. 처음 확인한 문제 - X-XSRF-TOKEN 헤더가 전송되지 않음

브라우저에는 `XSRF-TOKEN` 쿠키가 존재했지만 Axios의 로그아웃 요청에는

X-XSRF-TOKEN

헤더가 없었음.

Axios 설정 자체는 다음과 같이 정상적으로 되어 있었음.

const api = axios.create({
baseURL: import.meta.env.VITE_API_BASE_URL || '',
timeout: 10000,
withCredentials: true,
withXSRFToken: true,
xsrfCookieName: 'XSRF-TOKEN',
xsrfHeaderName: 'X-XSRF-TOKEN',
})

원인은 프론트와 API가 서로 다른 서브도메인을 사용하고 있었기 때문.

프론트:
oripamap.com

API:
api.oripamap.com

기존 XSRF-TOKEN은 API 호스트 전용(host-only) 쿠키로 발급되어
oripamap.com에서 실행되는 JavaScript가 해당 쿠키를 읽을 수 없었음.

실제로 프론트에서:

document.cookie

를 실행했을 때 XSRF-TOKEN이 나오지 않았음.

따라서 Axios가 XSRF-TOKEN을 읽어서 X-XSRF-TOKEN 헤더로 복사할 수 없었음.


### 2. 해결 - CSRF 쿠키 Domain 설정

Spring Security의 CookieCsrfTokenRepository에 쿠키 Domain을 지정하도록 설정.

application.properties:

csrf.cookie-domain=${CSRF_COOKIE_DOMAIN:}

SecurityConfig:

@Value("${csrf.cookie-domain:}")
private String csrfCookieDomain;

CookieCsrfTokenRepository csrfTokenRepository =
CookieCsrfTokenRepository.withHttpOnlyFalse();

if (csrfCookieDomain != null && !csrfCookieDomain.isBlank()) {
csrfTokenRepository.setCookieDomain(csrfCookieDomain);
}

운영 환경:

CSRF_COOKIE_DOMAIN=oripamap.com

이렇게 설정하면 XSRF-TOKEN이 특정 API 호스트 전용 쿠키가 아니라
oripamap.com과 api.oripamap.com에서 사용할 수 있는 Domain Cookie로 발급됨.


### 3. 주의 - `.oripamap.com` 사용 시 Tomcat 500 발생

처음에는 다음처럼 설정했음.

CSRF_COOKIE_DOMAIN=.oripamap.com

그러자 모든 API 요청에서 500 Internal Server Error 발생.

Tomcat 에러:

java.lang.IllegalArgumentException:
An invalid domain [.oripamap.com] was specified for this cookie

Rfc6265CookieProcessor.validateDomain(...)

현재 Tomcat의 RFC6265 쿠키 검증에서 앞에 `.`이 붙은 Domain 값을 거부함.

따라서:

CSRF_COOKIE_DOMAIN=.oripamap.com

이 아니라:

CSRF_COOKIE_DOMAIN=oripamap.com

으로 설정.

앞의 `.` 없이 Domain을 지정해도 하위 도메인인 api.oripamap.com에 함께 적용되는 Domain Cookie로 사용할 수 있음.


### 4. X-XSRF-TOKEN 헤더가 생겼는데도 로그아웃 403 발생

Domain 수정 후 Axios 요청에서 드디어 다음 헤더가 정상적으로 전송됨.

X-XSRF-TOKEN: 71f64ab8-...

하지만 로그아웃은 여전히:

POST /api/auth/logout
→ 403 Forbidden

서버 로그 확인 결과:

InvalidCsrfTokenException

즉 이번에는 "CSRF 토큰이 없음"이 아니라
"전송된 CSRF 토큰이 서버가 확인한 토큰과 다름" 문제였음.


### 5. 최종 원인 - XSRF-TOKEN 쿠키가 2개 존재

Chrome DevTools의 Application → Cookies를 확인해보니
동일한 이름의 XSRF-TOKEN 쿠키가 2개 존재했음.

기존 쿠키:
XSRF-TOKEN
Domain = api.oripamap.com

새 쿠키:
XSRF-TOKEN
Domain = oripamap.com

즉 Domain 설정을 변경하기 전에 발급된 host-only 쿠키가 브라우저에 남아 있었고,
새로운 Domain Cookie가 추가되면서 같은 이름의 쿠키가 동시에 존재하게 됨.

Axios는 한 XSRF-TOKEN 값을 읽어서:

X-XSRF-TOKEN: A

를 전송했지만 Spring Security에서는 요청의 다른 XSRF-TOKEN 쿠키:

XSRF-TOKEN=B

를 기준으로 검증할 수 있었음.

결과:

Header Token != Cookie Token

→ InvalidCsrfTokenException
→ 403 Forbidden


### 6. 최종 해결

Chrome DevTools:

Application
→ Cookies
→ 기존 XSRF-TOKEN 쿠키 전부 삭제
→ 페이지 새로고침

이후 서버에서 새로운 XSRF-TOKEN을 다시 발급받음.

최종적으로 XSRF-TOKEN이 하나만 존재하고:

Domain = oripamap.com
Path = /

상태가 됨.

Axios 요청:

Cookie:
XSRF-TOKEN=<token>

Header:
X-XSRF-TOKEN=<token>

두 값이 동일해지면서 Spring Security CSRF 검증 통과.

POST /api/auth/logout
→ 정상 처리


### 최종 정리

운영 환경:

oripamap.com
↓ Axios
api.oripamap.com

CSRF 쿠키 설정:

CSRF_COOKIE_DOMAIN=oripamap.com

Spring Security:

CookieCsrfTokenRepository.withHttpOnlyFalse()
↓
setCookieDomain("oripamap.com")
↓
XSRF-TOKEN Domain Cookie 발급

Axios:

XSRF-TOKEN Cookie
↓
X-XSRF-TOKEN Header로 복사
↓
POST /api/auth/logout

Spring Security:

Cookie Token == Header Token
↓
CSRF 검증 성공
↓
로그아웃 성공


### 핵심 교훈

1. 프론트와 API가 서브도메인으로 분리되어 있다면 CSRF Cookie Domain 범위를 확인할 것.

2. XSRF-TOKEN이 API host-only 쿠키이면 프론트 JavaScript에서 읽지 못해 Axios가 X-XSRF-TOKEN 헤더를 만들지 못할 수 있음.

3. Tomcat 환경에서는 `.oripamap.com` 대신 `oripamap.com`처럼 앞의 점 없이 Domain을 지정할 것.

4. X-XSRF-TOKEN 헤더가 존재하는데도 InvalidCsrfTokenException이 발생한다면 토큰 "존재 여부"가 아니라 "값 불일치"를 확인할 것.

5. 쿠키 Domain 정책을 변경한 뒤에는 기존 host-only 쿠키가 브라우저에 남아 동일 이름의 쿠키가 중복될 수 있음.

6. DevTools → Application → Cookies에서 동일한 이름의 XSRF-TOKEN이 여러 개 존재하는지 확인할 것.

7. CSRF 문제 디버깅 시 다음 순서로 확인하면 빠름.

   XSRF-TOKEN 쿠키 존재 여부
   → document.cookie에서 접근 가능한지
   → X-XSRF-TOKEN 요청 헤더 존재 여부
   → 서버의 InvalidCsrfTokenException 여부
   → 동일 이름 XSRF-TOKEN 쿠키 중복 여부
   → Cookie Token과 Header Token 값 일치 여부