백엔드 - 내가 배운 것
------------------------------------------------------
1. SecurityConfig - 화이트리스트 방식
------------------------------------------------------

기존:
인증이 필요한 API만 따로 지정하고,
나머지 API는 기본적으로 허용
→ 보안 설정을 빼먹으면 해당 API가 누구에게나 공개될 수 있음

변경:
로그인 없이 사용할 API만 permitAll()로 명시하고,
나머지 모든 API는 authenticated()로 인증 필요
→ 새 API의 보안 설정을 빼먹어도 기본적으로 인증이 필요해서 더 안전함

------------------------------------------------------
2. 로그인 보안
------------------------------------------------------

| 항목 | 적용 내용 | 목적 |
|---|---|---|
| 이메일 중복 방지 | `users.email`에 `UNIQUE` 제약 적용 | 동시 가입 요청에서도 중복 계정 생성 방지 |
| CSRF 방어 | `XSRF-TOKEN` 쿠키 + `X-XSRF-TOKEN` 헤더 검증 | 쿠키 인증을 악용한 위조 요청 방지 |
| Refresh Token | Access Token 30분 고정 + Refresh Token 7일 | 장기 Access Token 사용 위험 감소 |
| 로그인 Rate Limit | 5분 내 5회 실패 시 10분 차단 | Brute Force 공격 방지 |
| OAuth 로그 보호 | 토큰·응답 원문 등 민감정보 로그 제거 | 로그를 통한 인증정보 노출 방지 |

### 1. 이메일 중복 방지
- `users.email`에 `NOT NULL + UNIQUE` 적용
- 애플리케이션의 사전 중복 검사와 별개로 DB에서도 중복을 최종 차단
- 동시 가입 요청(Race Condition)이 발생해도 DB `UNIQUE` 제약으로 방지
- 중복 발생 시 `409 Conflict` 반환

### 2. CSRF 방어
- Spring Security CSRF 활성화
- `XSRF-TOKEN` 쿠키 발급
- Axios가 `X-XSRF-TOKEN` 헤더로 토큰 전송
- 서버가 쿠키와 헤더의 CSRF 토큰을 검증
- Access/Refresh Token의 `HttpOnly` 설정은 그대로 유지
- `POST`, `PUT`, `PATCH`, `DELETE` 등 상태 변경 요청 보호

### 3. 토큰 관리
- Access Token은 `rememberMe`와 관계없이 30분으로 고정
- Refresh Token은 32바이트 이상의 랜덤 토큰으로 생성
- Refresh Token 원문은 `HttpOnly` 쿠키에 저장
- DB에는 원문 대신 `SHA-256 hash`만 저장
- Refresh할 때마다 기존 Refresh Token을 폐기하고 새 토큰으로 교체(Rotation)
- 로그아웃 시 현재 Refresh Token 폐기
- 비밀번호 변경 시 해당 사용자의 모든 Refresh Token 폐기

### 4. 로그인 시도 제한 (Rate Limit)
- `IP + email` 조합을 기준으로 로그인 실패 기록
- 5분 내 5번째 실패부터 10분간 로그인 차단
- 차단 시 `429 Too Many Requests` 반환
- `Retry-After: 600` 헤더 제공
- 로그인 성공 시 해당 실패 기록 초기화
- 현재는 메모리 기반이므로 서버 재시작 시 기록 초기화
- 다중 서버 환경에서는 기록이 공유되지 않으므로 추후 Redis 등의 공용 저장소 고려

### 5. OAuth 로그 보호
- 카카오/네이버 OAuth 오류 응답 본문 전체를 로그에 남기지 않음
- 예외 객체 전체 로그 제거
- Provider, HTTP Status, 검증된 Error Code 등 문제 추적에 필요한 최소 정보만 기록
- Access Token, Refresh Token, Authorization Code, Client Secret 등 민감정보 로그 금지

### 추후 보완
- Refresh Token 탈취 및 재사용 탐지가 필요해지면 `family_id`, `replaced_by_id`, Reuse Detection 도입 고려
- 운영 배포 시 HTTPS 적용
- 운영 환경에서 `JWT_COOKIE_SECURE=true` 적용
- 운영 도메인 기준 CORS 설정

## 1. 생성 API가 생성된 데이터를 반환하는 이유

생성 API가 응답 본문 없이 `201 Created`만 반환하면, 프론트는 생성된 데이터를 보여주기 위해 목록 API를 다시 호출해야 한다.

반대로 생성된 데이터를 응답으로 반환하면:

`POST → 생성된 데이터 반환 → 프론트 목록에 바로 추가`

할 수 있다.

**장점**
- 불필요한 GET 요청 감소
- 화면에 즉시 반영 가능
- 서버에서 생성된 `id`, `createdAt` 등의 정확한 값 사용 가능


## 2. FK - ON DELETE CASCADE vs SET NULL

### ON DELETE CASCADE
부모가 삭제되면 연결된 자식도 같이 삭제한다.

예:
`places 삭제 → oripa_place 삭제`

부모가 없어지면 자식도 존재할 이유가 없을 때 사용한다.

### ON DELETE SET NULL
부모가 삭제되어도 자식은 유지하고 FK만 `NULL`로 만든다.

예:
`places 삭제 → OWNER 계정 유지 + users.place_id = NULL`

자식 데이터 자체는 계속 존재해야 할 때 사용한다.

> 참고: DB의 CASCADE로 S3 파일까지 삭제되는 것은 아니다. S3 파일은 별도로 삭제해야 한다.


## 3. POST / PUT / PATCH 차이

- **POST**: 새로운 리소스 생성
- **PUT**: 특정 리소스의 전체 상태를 저장/교체
- **PATCH**: 기존 리소스의 일부만 수정

PUT은 반드시 수정만 의미하는 것은 아니다.

예:

`PUT /api/places/3/oripa`

- 데이터가 없으면 → 생성
- 데이터가 있으면 → 갱신

처럼 `upsert` 방식으로 설계할 수도 있다.

## 4. UUID

UUID는 **거의 겹치지 않는 고유 식별자**다.

```text
51c191c7-b6fd-4b21-9b36-d5648e5c0cfa
```

오맵에서는 기존 숫자 ID는 DB 내부용으로 유지하고, UUID는 URL 공개용으로 사용한다.

```text
id = 67                     → DB 내부용
public_id = UUID            → URL 공개용
```

예:

```text
/place/51c191c7-b6fd-4b21-9b36-d5648e5c0cfa
```

Java에서는 `UUID.randomUUID()`로 쉽게 생성할 수 있다.

------------------------------------------------------
3. `/me` API 패턴
------------------------------------------------------

`/me`는 `{id}`에 들어가는 값이 아니라 **현재 로그인한 사용자를 의미하는 고정 경로**다.

예:

```http
GET /api/inquiries/me
GET /api/inquiries/123
```

- `/me` → 현재 로그인한 사용자의 문의 조회
- `/123` → ID가 123인 특정 문의 조회

Spring에서는 `/me`와 `/{id}`를 서로 다른 경로로 매핑할 수 있다.

```java
@GetMapping("/me")
public List<InquiryResponse> getMyInquiries() {
    // 인증 정보에서 현재 userId 조회
}

@GetMapping("/{id}")
public InquiryResponse getInquiry(@PathVariable Long id) {
    // id로 특정 문의 조회
}
```

### 문의 API 구조

```text
POST  /api/inquiries
→ 문의 등록

GET   /api/inquiries/me
→ 내 문의 목록

GET   /api/admin/inquiries
→ 관리자 전체 문의 목록

GET   /api/admin/inquiries/{id}
→ 관리자 특정 문의 조회

PATCH /api/admin/inquiries/{id}/status
→ 문의 처리 상태 변경
```

로그인한 본인의 데이터는 프론트에서 `userId`를 직접 보내기보다 `/me`를 사용하고, 서버가 JWT 등 인증 정보에서 현재 사용자를 확인하는 방식이 깔끔하다.

------------------------------------------------------
4. 재배포 흐름
-----------------------------------------------------

로컬 백엔드 수정
↓
WAR 다시 빌드
↓
Docker 이미지 다시 빌드
↓
EC2로 이미지/소스 반영
↓
기존 컨테이너 내리고 새 컨테이너 실행
↓
Nginx는 그대로


------------------------------------------
5.df
-------------------------------------------
FOR UPDATE 잠금은 트랜잭션 안에서 써야 의미가 있어.
트랜잭션이 끝나면서 COMMIT 또는 ROLLBACK되면 잠금도 풀려.

Transaction → 작업을 하나로 묶음

FOR UPDATE → 그 Transaction 동안 해당 행을 잠금

INSERT IGNORE + UNIQUE → 이메일당 하나의 행만 존재하도록 경쟁 처리

ROLLBACK → 중간 실패 시 DB 변경 취소

noRollbackFor → 특정 예외가 나도 필요한 DB 변경은 살림

근데 네 구현이 유독 커 보이는 이유는 그냥 "메일 보내기"만 만든 게 아니기 때문이야.

지금 너는 사실상:

인증번호 생성
+ 인증번호 해시 저장
+ 5분 만료
+ 5회 시도 제한
+ 60초 재발송 제한
+ 동시 요청 방어
+ DB Transaction
+ FOR UPDATE
+ 기존 가입 이메일 검사
+ Resend 연동
+ 도메인 DNS 인증
+ 회원가입 시 서버에서 재검증

------------------------------------------
6. 회원탈퇴시
-------------------------------------------

회원 계정              → 삭제
즐겨찾기               → 삭제
내 댓글                → 삭제
내 댓글 첨부사진(S3)    → 삭제
문의                   → 삭제
정보 수정 요청          → 삭제
Refresh Token          → 삭제
로그인 쿠키             → 삭제

장소 대표사진(place_images) → 유지
└─ user_id만 NULL

오리파 공식 이미지       → 유지
이벤트 공식 이미지       → 유지
남이 작성한 대댓글       → 유지