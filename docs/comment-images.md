# 댓글 이미지 API

## DB 적용

기존 DB에는 `src/main/resources/sql/migrations/001_comments_image_key.sql`을 한 번 실행한 뒤 서버를 재시작한다.
신규 DB는 `sql/init/002_tables.sql`에 컬럼이 포함되어 있으므로 ALTER를 중복 실행하지 않는다.

```sql
ALTER TABLE comments ADD COLUMN image_key VARCHAR(500) NULL;
```

## 등록

기존 JSON 요청을 그대로 지원한다.

```http
POST /api/comments/place/1
Content-Type: application/json

{"content":"방문 후기"}
```

이미지 댓글은 같은 주소에 `multipart/form-data`로 전송한다.

| 필드 | 형식 | 설명 |
| --- | --- | --- |
| content | Text | 댓글 본문 |
| file | File | 선택, JPG/PNG 1장, 최대 5MB·2,000만 픽셀 |

Postman에서 Body → form-data를 사용한다. Content-Type과 boundary는 직접 설정하지 않는다.
JWT 쿠키 또는 Bearer 토큰으로 로그인해야 하며 성공 응답은 기존과 동일하게 본문 없는 201이다.
`/api/images`로 사전 업로드할 필요가 없다. JSON의 imageKey/imageUrl을 받아 저장하지 않는다.
두 장 이상, 다른 이름의 파일 필드, 답글 파일 첨부는 400으로 거부한다.
이미지를 선택하지 않았으면 file 필드를 생략한다. 빈 파일 자체를 보내면 이미지 검증에서 거부한다.

## 답글·수정

`POST /api/comments/{commentId}/replies`의 기존 JSON 요청을 유지한다.
multipart 본문만 있는 답글도 허용하지만 파일이 있으면 서비스에서 업로드 전에 거부한다.
`PUT /api/comments/{commentId}`는 기존 JSON 본문 수정만 지원하며 저장된 이미지를 유지한다.

## 조회

- `GET /api/comments/place/{placeId}`: 기존 댓글·답글 트리 응답에 imageUrl 추가. 이미지가 없으면 null.
- `GET /api/comments/place/{placeId}/photos`: 해당 장소의 일반 댓글 중 image_key IS NOT NULL인 댓글을 최신순으로 반환한다. 응답 항목은 기존 CommentResponse 형식이며 replies는 빈 배열이다.

imageUrl은 조회 시 새로 발급되며 최대 10분간 유효하다. 만료되면 목록 API를 다시 조회한다.
DB에는 S3 객체 키만 저장하며 응답은 표시용 URL을 제공한다.
방문자 사진 조회는 기존 댓글 조회와 같이 공개 API이며 현재 페이지네이션은 없다.

## 실패 및 삭제

- S3 업로드 실패: 댓글을 저장하지 않는다.
- 댓글 INSERT 실패/0행 저장: DB 롤백 후 방금 업로드한 객체 삭제를 시도한다.
- DB 커밋 결과가 불명확한 오류: 저장된 댓글의 이미지를 잘못 삭제하지 않도록 객체를 유지하고 키를 로그에 남긴다.
- `DELETE /api/comments/{commentId}`: 작성자 조건으로 행 잠금 → 댓글 삭제 및 DB 커밋 → S3 객체 삭제. 정상 완료는 204, 작성자 불일치/없는 댓글은 기존과 동일한 403.
- DB 삭제 실패: S3 파일을 유지한다.
- DB 삭제 후 S3 삭제 실패: 댓글은 삭제된 상태이며 502를 반환하고 commentId·객체 키를 로그에 남긴다.
- 등록 실패 후 S3 정리도 실패: 원래 DB 오류를 유지하고 정리 오류와 키를 로그에 남긴다.

S3와 DB의 원자적 처리는 보장하지 않는다. 별도 테이블/작업 큐가 없으므로 장애·프로세스 중단으로 남은 파일은 로그를 기반으로 수동 정리가 필요하다.
이 처리는 댓글 API를 통한 삭제에 적용된다. 장소/사용자 직접 삭제에 따른 DB 외래 키 CASCADE는 S3 삭제를 호출하지 않는다.

## 검증 범위

단위/MVC 테스트는 외부 DB와 S3를 대역으로 사용한다. 실제 MySQL ALTER 적용과 AWS 업로드·삭제는 별도로 확인해야 한다.
실제 확인 순서: ALTER 실행 → 로그인 → 이미지 댓글 등록 → DB image_key 확인 → 댓글/사진 조회 URL 열기 → 댓글 삭제 → DB 행과 S3 객체 삭제 확인.
