# ORIPA 상세 등록·수정

`PUT /api/places/{placeId}/oripa` — 관리자 전용. 기존 인증 및 CSRF 정책 적용.
`multipart/form-data`로 다음 파트를 전달한다.

- `data`: 아래 형식의 `application/json` 파트.
- `files`: 신규 이미지 파일만 같은 이름으로 반복 첨부. 신규 파일이 없으면 생략.

```json
{
  "summary": "매장 한 줄 소개",
  "introduction": "상세 소개",
  "socialLinks": [{"type": "instagram", "url": "https://example.com"}],
  "images": [{"id": 12}, {"fileIndex": 0}, {"id": 9}]
}
```

`images`는 필수이며 최종 이미지 전체 순서다. 배열 위치가 0부터 시작하는 `sortOrder`가 된다.
기존 이미지는 GET 응답의 `id`, 신규 이미지는 `files`의 0부터 시작하는 `fileIndex`를 사용한다.
각 항목에는 둘 중 하나만 지정한다. 중복 ID/인덱스, 범위 밖 인덱스, 참조되지 않은 파일,
다른 매장 또는 존재하지 않는 이미지 ID는 400이다.
목록에서 빠진 기존 이미지만 삭제한다. `images: []`는 전체 이미지 삭제다.
`socialLinks`도 배열이 필수이며 `[]`로 비운다. summary/introduction의 생략 또는 null은 값을 비운다.

성공은 200이며 기존 `GET /api/places/{id}`와 같은 PlaceResponse를 반환한다.
없는 매장은 404, ORIPA가 아닌 매장은 400이다. 기존 S3ImageService의 JPG/PNG/WebP,
파일당 5MB 검증을 재사용한다. 전체 multipart 요청 한도는 기존 20MB이다.

매장 행 잠금으로 수정과 매장 삭제를 직렬화한다. DB 롤백 시 이번 요청에서 업로드에 성공한
파일만 정리하고 기존 파일은 유지한다. 기존 댓글 로직처럼 커밋 결과가 UNKNOWN이면
이미 저장됐을 가능성이 있는 파일을 보존하고 확인용 로그를 남긴다.
기존 이미지의 S3 삭제는 DB 커밋 후 수행한다. 실패해도 나머지 삭제를 모두 시도하며
로그 및 502를 반환한다. 이때 DB 변경은 이미 저장된 상태다. 기존 정책에 자동 재시도 큐는 없다.
롤백 후 S3 정리 자체가 실패한 경우에도 키와 오류를 기록하므로 운영 정리가 필요하다.
