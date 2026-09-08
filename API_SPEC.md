# 오리파맵 API 명세서

> 기준: 현재 백엔드 구현 + 회원 PUT 보안, 이메일 중복 확인, 장소 수정 요청 사용자 식별 개선 반영

## 1. 공통 정보

  -----------------------------------------------------------------------
항목                                내용
  ----------------------------------- -----------------------------------
로컬 API 진입점                     `http://localhost` (Nginx `:80` →
backend `:8080`)

인증 방식                           `Authorization: Bearer <JWT>` 또는
`Cookie: accessToken=<JWT>`

JWT 쿠키                            `accessToken`

CSRF                                비활성화

전체 API                            Controller 매핑 27개 + Spring
Security 로그인 필터 1개 = 총 28개
작업
  -----------------------------------------------------------------------

## 2. API 목록

### 2.1 장소

  ----------------------------------------------------------------------------------------------
Method      Endpoint               인증        요청        응답                    설명
  ----------- ---------------------- ----------- ----------- ----------------------- -----------
GET         `/api/places`          X           없음        `List<PlaceResponse>`   전체 장소
목록 조회

GET         `/api/places/search`   X           Query:      `List<PlaceResponse>`   키워드 장소
`keyword`                           검색
  ----------------------------------------------------------------------------------------------

### 2.2 댓글 / 답글

  -----------------------------------------------------------------------------------------------------------------------------------
Method      Endpoint                                 인증        요청                        응답                      설명
  ----------- ---------------------------------------- ----------- --------------------------- ------------------------- ------------
GET         `/api/comments/place/{placeId}`          X           Path: `placeId`, Query:     `CommentPageResponse`     부모 댓글 페이징 및
                                                              `page=0`, `size=5`                                  해당 답글 전체 조회

GET         `/api/comments/place/{placeId}/photos`   X           Path: `placeId`             `List<CommentResponse>`   사진 포함
댓글 조회

POST        `/api/comments/place/{placeId}`          O           JSON                        `201`                     텍스트 댓글
`CommentRequest(content)`                             작성

POST        `/api/comments/place/{placeId}`          O           `multipart/form-data`:      `201`                     이미지 첨부
`content`, `file`                                     가능 댓글
작성

POST        `/api/comments/{commentId}/replies`      O           JSON                        `201`                     텍스트 답글
`CommentRequest(content)`                             작성

POST        `/api/comments/{commentId}/replies`      O           `multipart/form-data`       `201`                     multipart
답글 작성
(이미지
첨부는
서비스에서
거부)

PUT         `/api/comments/{commentId}`              O           JSON                        `204`                     본인 댓글
`CommentRequest(content)`                             수정

DELETE      `/api/comments/{commentId}`              O           Path: `commentId`           `204`                     본인 댓글
삭제
  -----------------------------------------------------------------------------------------------------------------------------------

**댓글 제약** - `content`: 유니코드 코드 포인트 기준 최대 300자 - 일반
댓글 이미지 최대 1개 - multipart 파일 필드명: `file` - 답글 이미지 첨부
불가

### 2.3 즐겨찾기

  ----------------------------------------------------------------------------------------------------
Method      Endpoint                     인증        요청        응답                    설명
  ----------- ---------------------------- ----------- ----------- ----------------------- -----------
POST        `/api/favorites/{placeId}`   O           Path:       `201` 또는 `204`        즐겨찾기
`placeId`                           추가

DELETE      `/api/favorites/{placeId}`   O           Path:       `204`                   즐겨찾기
`placeId`                           삭제

GET         `/api/favorites/{placeId}`   O           Path:       `boolean`               즐겨찾기
`placeId`                           여부 조회

GET         `/api/favorites`             O           없음        `List<PlaceResponse>`   현재
사용자의
즐겨찾기
목록
  ----------------------------------------------------------------------------------------------------

### 2.4 장소 정보 수정 요청

  ---------------------------------------------------------------------------------------------
Method      Endpoint               인증        요청                   응답        설명
  ----------- ---------------------- ----------- ---------------------- ----------- -----------
POST        `/api/edit-requests`   O           JSON                   `void`      장소 정보
`EditRequestRequest`               수정 요청
등록

  ---------------------------------------------------------------------------------------------

> 요청 작성자는 요청 본문이 아니라 JWT의 인증 사용자 ID로 저장한다.

### 2.5 이미지

  --------------------------------------------------------------------------------------------------------
Method      Endpoint        인증        요청                     응답                        설명
  ----------- --------------- ----------- ------------------------ --------------------------- -----------
POST        `/api/images`   O           `multipart/form-data`,   `201 ImageUploadResponse`   S3 업로드
`file`                                               후 key와
URL 반환

  --------------------------------------------------------------------------------------------------------

### 2.6 회원

  ---------------------------------------------------------------------------------------------------------------------
Method      Endpoint                                  인증        요청                  응답              설명
  ----------- ----------------------------------------- ----------- --------------------- ----------------- -----------
GET         `/api/member/checkusername/{email}`       X           Path: `email`         `Boolean`         이메일 중복
확인(호환용 deprecated alias)

GET         `/api/member/checkemail/{email}`          X           Path: `email`         `Boolean`         이메일 중복
확인

POST        `/api/member`                             X           Form binding          `201 MemberDTO`   로컬
`MemberJoinDTO`                         회원가입

PUT         `/api/member/{username}`                  O           Path `username` +     `200 MemberDTO`   프로필
`MemberUpdateDTO`                       수정. JWT
사용자와
URL
사용자가
다르면 403

PUT         `/api/member/{username}/changepassword`   O           Path `username` +     `200`             비밀번호
JSON                                    변경. JWT
`ChangePasswordDTO`                     사용자와
URL
사용자가
다르면 403
  ---------------------------------------------------------------------------------------------------------------------

> 회원 PUT 서비스는 요청 DTO의 username이 아니라 인증된 username을 수정
> 대상으로 사용한다.
>
> 이메일 중복 확인은 Controller, Service, Mapper에서 `existsByEmail(email)`로
> 통일하며 SQL은 `SELECT EXISTS`를 사용한다.

### 2.7 현재 사용자

  ---------------------------------------------------------------------------------
Method      Endpoint          인증        요청        응답            설명
  ----------- ----------------- ----------- ----------- --------------- -----------
GET         `/api/users/me`   O           없음        `UserInfoDTO`   현재 인증
사용자 조회

  ---------------------------------------------------------------------------------

### 2.8 인증 / OAuth

  -----------------------------------------------------------------------------------------------
Method      Endpoint                     인증        요청         응답            설명
  ----------- ---------------------------- ----------- ------------ --------------- -------------
POST        `/api/auth/login`            로그인 전   JSON         `UserInfoDTO` / 로컬 로그인
`LoginDTO`   `401`           및 JWT 쿠키
발급

POST        `/api/auth/logout`           X           없음         `204`           accessToken
쿠키 삭제

GET         `/api/auth/kakao`            X           없음         `302`           카카오 인증
페이지로 이동

GET         `/api/auth/kakao/callback`   X           Query:       `302`           카카오
`code`,                      로그인/가입
`state`;                     후 JWT 발급
state 쿠키

GET         `/api/auth/naver`            X           없음         `302`           네이버 인증
페이지로 이동

GET         `/api/auth/naver/callback`   X           Query:       `302`           네이버
`code`,                      로그인/가입
`state`;                     후 JWT 발급
state 쿠키
  -----------------------------------------------------------------------------------------------

## 3. 주요 DTO

  -----------------------------------------------------------------------
DTO                                 필드
  ----------------------------------- -----------------------------------
`PlaceResponse`                     id, type, name, branchName,
address, locationDetail, latitude,
longitude, businessHours,
holidayInfo, phone, description,
imageUrl

`CommentRequest`                    content

`CommentResponse`                   id, userId, parentCommentId,
nickname, content, imageUrl,
createdAt, updatedAt, replies

`CommentPageResponse`               comments, page, size, hasNext, totalCount

`EditRequestRequest`                placeId, requestTypes, memo

`ImageUploadResponse`               key, url

`MemberJoinDTO`                     username, password, email, nickname

`MemberUpdateDTO`                   username, password, email

`ChangePasswordDTO`                 username, oldPassword, newPassword

`MemberDTO`                         id, username, email, nickname,
provider, regDate, updateDate,
authList

`UserInfoDTO`                       id, username, email, nickname,
roles

`LoginDTO`                          email (`username` alias 허용),
password, rememberMe
  -----------------------------------------------------------------------

## 4. 인증 및 제한

-   보호 API는 JWT Bearer 토큰 또는 `accessToken` 쿠키로 인증한다.
-   공개 API만 `permitAll()`로 명시하며 그 외 요청은 기본적으로 인증이
    필요하다.
-   회원 PUT API는 `/api/member/**`에 대해 인증이 필요하다.
-   프로필/비밀번호 변경 시 URL username과 JWT 인증 사용자가 다르면
    `403 Forbidden`이다.
-   `rememberMe=false`: JWT/쿠키 30분
-   `rememberMe=true`: JWT/쿠키 7일
-   이미지 파일은 백엔드에서 파일당 최대 5MB로 제한한다.
-   Nginx `client_max_body_size`는 파일 크기가 아니라 HTTP 요청 전체
    크기 제한이다.

## 5. HTTP Method 집계

Method                 API 작업 수
  ---------- -----------------------
GET                             13
POST         10 (로그인 필터 포함)
PUT                              3
PATCH                            0
DELETE                           2
**총계**                    **28**

## 6. 현재 확인 필요 항목

-   현재 확인된 보안 및 데이터 정합성 개선 항목은 모두 반영되었다.
