package org.scoula.place;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.scoula.common.service.S3ImageService;
import org.scoula.place.dto.PlaceResponse;
import org.scoula.place.controller.PlaceController;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.scoula.place.mapper.PlaceMapper;
import org.scoula.place.service.PlaceService;
import org.scoula.security.account.domain.MemberVO;
import org.scoula.place.vo.OripaPlaceImageVO;
import org.scoula.place.vo.OripaPlaceVO;
import org.scoula.place.vo.PlaceVO;
import org.scoula.place.vo.TagVO;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

class PlaceServiceTest {
    final List<String> events = new ArrayList<>();
    PlaceVO place = new PlaceVO();
    OripaPlaceVO detail = new OripaPlaceVO();
    List<OripaPlaceImageVO> images = List.of(image(2L, "first", 0), image(1L, "second", 1));
    List<TagVO> tags = List.of(tag(1L, "포켓몬", "TCG"), tag(2L, "매입가능", "SERVICE"));
    boolean failCommit;
    boolean failDelete;
    boolean failS3;
    boolean failInsert;
    int uploads;
    int failUploadAt;

    final PlaceMapper mapper = (PlaceMapper) Proxy.newProxyInstance(
            PlaceMapper.class.getClassLoader(), new Class<?>[]{PlaceMapper.class}, (proxy, method, args) -> {
                events.add(method.getName());
                return switch (method.getName()) {
                    case "findById" -> place;
                    case "findByPublicId" -> place != null && place.getPublicId().equals(args[0]) ? place : null;
                    case "findTagsByPlaceId" -> tags;
                    case "findIdForUpdate" -> place == null ? null : 1L;
                    case "findOripaByPlaceId" -> detail;
                    case "findOripaImagesByPlaceId" -> images;
                    case "findAll", "searchPlaces" -> List.of(place);
                    case "upsertOripa" -> { detail = (OripaPlaceVO) args[0]; yield 1; }
                    case "deleteOripaImage" -> {
                        assertEquals(1L, args[0]);
                        images = images.stream().filter(i -> !i.getId().equals(args[1])).toList();
                        yield 1;
                    }
                    case "updateOripaImageOrder" -> {
                        assertEquals(1L, args[0]);
                        images.stream().filter(i -> i.getId().equals(args[1]))
                                .forEach(i -> i.setSortOrder((Integer) args[2]));
                        yield 1;
                    }
                    case "insertOripaImage" -> {
                        if (failInsert) throw new IllegalStateException("insert failed");
                        images = new ArrayList<>(images);
                        images.add(image(100L + uploads, (String) args[1], (Integer) args[2]));
                        yield 1;
                    }
                    case "deletePlace" -> {
                        if (failDelete) throw new IllegalStateException("delete failed");
                        yield 1;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                };
            });
    final S3ImageService s3 = new S3ImageService(null, null, "test") {
        @Override public String upload(org.springframework.web.multipart.MultipartFile file) {
            uploads++;
            if (uploads == failUploadAt) throw new IllegalStateException("upload failed");
            events.add("upload:new" + uploads);
            return "new" + uploads;
        }
        @Override public String createPresignedGetUrl(String key) {
            events.add("sign:" + key);
            return "https://example.test/" + key;
        }
        @Override public void delete(String key) {
            events.add("s3:" + key);
            if (failS3) throw new IllegalStateException("S3 failed: " + key);
        }
    };
    final AbstractPlatformTransactionManager transactions = new AbstractPlatformTransactionManager() {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object tx, TransactionDefinition definition) {
            assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definition.getPropagationBehavior());
            events.add("begin");
        }
        @Override protected void doCommit(DefaultTransactionStatus status) {
            events.add("commit");
            if (failCommit) throw new TransactionSystemException("commit failed");
        }
        @Override protected void doRollback(DefaultTransactionStatus status) { events.add("rollback"); }
    };
    final PlaceService service = new PlaceService(mapper, s3, transactions);
    final MemberVO admin = actor("ADMIN", null);

    PlaceServiceTest() {
        place.setId(1L);
        place.setPublicId("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
        place.setType("ORIPA");
        place.setImageUrl("https://legacy.test/image.png");
        detail.setPlaceId(1L);
        detail.setSummary("summary");
        detail.setIntroduction("introduction");
        detail.setSocialLinks("[{\"type\":\"instagram\",\"url\":\"https://example.test\"}]");
    }

    @Test void detailReturnsJsonArrayAndSignedImages() throws Exception {
        PlaceResponse response = service.getPlace(1L);
        assertEquals(place.getPublicId(), response.getPublicId());
        assertEquals("summary", response.getOripaPlace().getSummary());
        assertEquals("introduction", response.getOripaPlace().getIntroduction());
        assertEquals(List.of(2L, 1L), response.getOripaPlace().getImages().stream().map(i -> i.getId()).toList());
        assertEquals("https://example.test/first", response.getOripaPlace().getImages().get(0).getImageUrl());
        var json = new ObjectMapper().valueToTree(response);
        assertTrue(json.get("oripaPlace").get("socialLinks").isArray());
        assertFalse(json.get("oripaPlace").get("images").get(0).has("imageKey"));
        assertEquals("포켓몬", json.get("tags").get(0).get("name").asText());
        assertEquals("TCG", json.get("tags").get(0).get("category").asText());
    }

    @Test void publicIdDetailReusesNumericIdDetailAndMissingReturns404() {
        PlaceResponse response = service.getPlaceByPublicId(place.getPublicId());

        assertEquals(place.getId(), response.getId());
        assertEquals(place.getPublicId(), response.getPublicId());
        assertEquals(List.of("findByPublicId", "findById", "findOripaByPlaceId",
                "findOripaImagesByPlaceId", "sign:first", "sign:second", "findTagsByPlaceId"), events);

        events.clear();
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.getPlaceByPublicId("missing-public-id")).getRawStatusCode());
        assertEquals(List.of("findByPublicId"), events);
    }

    @Test void vendingLoadsCommonTagsWithoutOripaQueries() {
        place.setType("POKEMON_VENDING");
        PlaceResponse response = service.getPlace(1L);
        assertEquals(place.getImageUrl(), response.getImageUrl());
        assertEquals(List.of("포켓몬", "매입가능"), response.getTags().stream().map(tag -> tag.getName()).toList());
        assertFalse(new ObjectMapper().valueToTree(response).has("oripaPlace"));
        assertEquals(List.of("findById", "findTagsByPlaceId"), events);
    }

    @Test void listAndSearchDoNotLoadOripaData() {
        PlaceResponse listed = service.getPlaces().get(0);
        PlaceResponse searched = service.searchPlaces("test").get(0);
        assertNull(listed.getOripaPlace());
        assertNull(searched.getOripaPlace());
        assertFalse(new ObjectMapper().valueToTree(listed).has("tags"));
        assertFalse(new ObjectMapper().valueToTree(searched).has("tags"));
        assertEquals(List.of("findAll", "searchPlaces"), events);
    }

    @Test void missingExtensionDoesNotBreakDetail() {
        detail = null;
        assertNull(service.getPlace(1L).getOripaPlace());
    }

    @Test void nullLinksAndEmptyImagesAreArrays() {
        detail.setSocialLinks(null);
        images = List.of();
        assertTrue(service.getPlace(1L).getOripaPlace().getSocialLinks().isArray());
        assertTrue(service.getPlace(1L).getOripaPlace().getImages().isEmpty());
        detail.setSocialLinks("null");
        assertTrue(service.getPlace(1L).getOripaPlace().getSocialLinks().isArray());
    }

    @Test void invalidSocialLinksAreNotReturnedAsStringsOrObjects() {
        for (String value : List.of("{}", "broken")) {
            detail.setSocialLinks(value);
            assertThrows(IllegalStateException.class, () -> service.getPlace(1L));
        }
    }

    @Test void missingPlaceReturns404ForReadAndDelete() {
        place = null;
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.getPlace(1L)).getRawStatusCode());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.deletePlace(1L, admin)).getRawStatusCode());
        assertFalse(events.contains("deletePlace"));
    }

    @Test void deletesImagesOnlyAfterCommit() {
        service.deletePlace(1L, admin);
        assertEquals(List.of("begin", "findIdForUpdate", "findOripaImagesByPlaceId", "deletePlace",
                "commit", "s3:first", "s3:second"), events);
    }

    @Test void dbFailurePreservesS3Objects() {
        failDelete = true;
        assertThrows(IllegalStateException.class, () -> service.deletePlace(1L, admin));
        assertTrue(events.contains("rollback"));
        assertFalse(events.stream().anyMatch(e -> e.startsWith("s3:")));
    }

    @Test void commitFailurePreservesS3Objects() {
        failCommit = true;
        assertThrows(TransactionSystemException.class, () -> service.deletePlace(1L, admin));
        assertFalse(events.stream().anyMatch(e -> e.startsWith("s3:")));
    }

    @Test void s3FailureStillAttemptsAllKeys() {
        failS3 = true;
        assertEquals(502, assertThrows(ResponseStatusException.class, () -> service.deletePlace(1L, admin)).getRawStatusCode());
        assertTrue(events.containsAll(List.of("s3:first", "s3:second")));
    }

    @Test void deletingVendingWithoutOripaImagesDoesNotCallS3() {
        place.setType("POKEMON_VENDING");
        images = List.of();
        service.deletePlace(1L, admin);
        assertTrue(events.contains("commit"));
        assertFalse(events.stream().anyMatch(e -> e.startsWith("s3:")));
    }

    @Test void mapperLoadsAndOrdersImagesDeterministically() throws Exception {
        Configuration config = new Configuration();
        String path = "org/scoula/place/mapper/PlaceMapper.xml";
        try (var input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, config, path, config.getSqlFragments()).parse();
        }
        String sql = config.getMappedStatement(PlaceMapper.class.getName() + ".findOripaImagesByPlaceId")
                .getBoundSql(1L).getSql().replaceAll("\\s+", " ");
        assertTrue(sql.contains("ORDER BY sort_order ASC, id ASC"));
        String tagSql = config.getMappedStatement(PlaceMapper.class.getName() + ".findTagsByPlaceId")
                .getBoundSql(1L).getSql().replaceAll("\\s+", " ");
        assertTrue(tagSql.contains("INNER JOIN place_tags"));
        assertTrue(tagSql.contains("WHERE pt.place_id = ?"));
        assertTrue(tagSql.contains("ORDER BY t.id ASC"));
        String publicIdSql = config.getMappedStatement(PlaceMapper.class.getName() + ".findByPublicId")
                .getBoundSql("6ba7b810-9dad-11d1-80b4-00c04fd430c8").getSql().replaceAll("\\s+", " ");
        assertTrue(publicIdSql.contains("WHERE public_id = ?"));
    }

    @Test void controllerRoutesDetailSearchAndDelete() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new PlaceController(service))
                .setCustomArgumentResolvers(
                        new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .build();
        var response = mvc.perform(get("/api/places/1")).andReturn().getResponse();
        assertEquals(200, response.getStatus());
        assertTrue(new ObjectMapper().readTree(response.getContentAsString())
                .get("oripaPlace").get("socialLinks").isArray());
        assertEquals(200, mvc.perform(get("/api/places/search").param("keyword", "test"))
                .andReturn().getResponse().getStatus());
        assertEquals(200, mvc.perform(get("/api/places/public/" + place.getPublicId()))
                .andReturn().getResponse().getStatus());
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(authentication(admin));
        try {
            assertEquals(204, mvc.perform(delete("/api/places/1"))
                    .andReturn().getResponse().getStatus());
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
        place = null;
        assertEquals(404, mvc.perform(get("/api/places/999")).andReturn().getResponse().getStatus());
    }

    private static OripaPlaceImageVO image(Long id, String key, int order) {
        OripaPlaceImageVO image = new OripaPlaceImageVO();
        image.setId(id);
        image.setImageKey(key);
        image.setSortOrder(order);
        return image;
    }

    private static TagVO tag(Long id, String name, String category) {
        TagVO tag = new TagVO();
        tag.setId(id);
        tag.setName(name);
        tag.setCategory(category);
        return tag;
    }

    private org.scoula.place.dto.OripaPlaceRequest request(String imageJson) throws Exception {
        return new ObjectMapper().readValue("{\"summary\":\"updated\",\"introduction\":\"intro\","
                + "\"socialLinks\":[],\"images\":" + imageJson + "}", org.scoula.place.dto.OripaPlaceRequest.class);
    }

    private List<org.springframework.web.multipart.MultipartFile> files(int count) {
        return java.util.stream.IntStream.range(0, count).mapToObj(i ->
                (org.springframework.web.multipart.MultipartFile) new org.springframework.mock.web.MockMultipartFile(
                        "files", "test.png", "image/png", new byte[]{1})).toList();
    }

    @Test void createsMissingDetailWithMultipleImages() throws Exception {
        detail = null;
        images = List.of();
        var response = service.upsertOripa(1L, request("[{\"fileIndex\":1},{\"fileIndex\":0}]"), files(2), admin);
        assertEquals("updated", response.getOripaPlace().getSummary());
        assertEquals(2, response.getOripaPlace().getImages().size());
        assertEquals(List.of(0, 1), images.stream().map(i -> i.getSortOrder()).toList());
        assertEquals(2, uploads);
    }

    @Test void mixesKeepDeleteNewAndOrderWithoutReuploadingExisting() throws Exception {
        service.upsertOripa(1L, request("[{\"fileIndex\":0},{\"id\":2}]"), files(1), admin);
        assertEquals(1, uploads);
        assertEquals(1, images.stream().filter(i -> i.getId() == 2L).findFirst().orElseThrow().getSortOrder());
        assertTrue(events.indexOf("commit") < events.indexOf("s3:second"));
        assertFalse(events.contains("s3:first"));
        assertFalse(events.contains("s3:new1"));
    }

    @Test void reorderOnlyDoesNotTouchS3() throws Exception {
        service.upsertOripa(1L, request("[{\"id\":1},{\"id\":2}]"), List.of(), admin);
        assertEquals(0, uploads);
        assertFalse(events.stream().anyMatch(e -> e.startsWith("s3:")));
        assertEquals(0, images.get(1).getSortOrder());
    }

    @Test void emptyArrayDeletesAllImagesAfterCommit() throws Exception {
        service.upsertOripa(1L, request("[]"), List.of(), admin);
        assertTrue(images.isEmpty());
        assertTrue(events.indexOf("s3:first") > events.indexOf("commit"));
        assertTrue(events.contains("s3:second"));
    }

    @Test void rejectsForeignOrMissingIdsBeforeMutation() throws Exception {
        var data = request("[{\"id\":999}]");
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.upsertOripa(1L, data, List.of(), admin)).getRawStatusCode());
        assertFalse(events.contains("upsertOripa"));
        assertEquals(0, uploads);
    }

    @Test void rejectsInvalidImageManifest() throws Exception {
        for (String manifest : List.of("null", "[null]", "[{}]", "[{\"id\":1,\"fileIndex\":0}]",
                "[{\"id\":1},{\"id\":1}]", "[{\"fileIndex\":-1}]", "[{\"fileIndex\":0}]")) {
            var data = request(manifest);
            assertEquals(400, assertThrows(ResponseStatusException.class,
                    () -> service.upsertOripa(1L, data, List.of(), admin)).getRawStatusCode());
        }
        var data = request("[]");
        assertThrows(ResponseStatusException.class, () -> service.upsertOripa(1L, data, files(1), admin));
        data.setSocialLinks(new ObjectMapper().createObjectNode());
        assertThrows(ResponseStatusException.class, () -> service.upsertOripa(1L, data, List.of(), admin));
        assertTrue(events.isEmpty());
    }

    @Test void rejectsMissingAndNonOripaPlaces() throws Exception {
        var data = request("[]");
        place.setType("POKEMON_VENDING");
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.upsertOripa(1L, data, List.of(), admin)).getRawStatusCode());
        place = null;
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.upsertOripa(1L, data, List.of(), admin)).getRawStatusCode());
        assertFalse(events.contains("upsertOripa"));
    }

    @Test void insertFailureCleansNewUploadButPreservesOldS3Images() throws Exception {
        failInsert = true;
        var data = request("[{\"fileIndex\":0}]");
        assertThrows(IllegalStateException.class, () -> service.upsertOripa(1L, data, files(1), admin));
        assertTrue(events.indexOf("s3:new1") > events.indexOf("rollback"));
        assertFalse(events.contains("s3:first"));
        assertFalse(events.contains("s3:second"));
    }

    @Test void partialUploadFailureCleansEarlierUploads() throws Exception {
        failUploadAt = 2;
        var data = request("[{\"fileIndex\":0},{\"fileIndex\":1}]");
        assertThrows(IllegalStateException.class, () -> service.upsertOripa(1L, data, files(2), admin));
        assertTrue(events.contains("s3:new1"));
        assertFalse(events.contains("s3:first"));
    }

    @Test void unknownCommitPreservesPotentiallyCommittedUploads() throws Exception {
        failCommit = true;
        var data = request("[{\"fileIndex\":0}]");
        assertThrows(TransactionSystemException.class, () -> service.upsertOripa(1L, data, files(1), admin));
        assertFalse(events.stream().anyMatch(e -> e.startsWith("s3:")));
    }

    @Test void cleanupFailureDoesNotMaskDbFailure() throws Exception {
        failInsert = true;
        failS3 = true;
        var data = request("[{\"fileIndex\":0}]");
        var failure = assertThrows(IllegalStateException.class, () -> service.upsertOripa(1L, data, files(1), admin));
        assertEquals("insert failed", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
    }

    @Test void postCommitDeleteFailureAttemptsEveryDeletedKey() throws Exception {
        failS3 = true;
        var data = request("[]");
        assertEquals(502, assertThrows(ResponseStatusException.class,
                () -> service.upsertOripa(1L, data, List.of(), admin)).getRawStatusCode());
        assertTrue(events.containsAll(List.of("commit", "s3:first", "s3:second")));
    }

    @Test void ownerCanUpdateOnlyAssignedOripaAndAdminCanUpdateAnyOripa() throws Exception {
        service.upsertOripa(1L, request("[{\"id\":2},{\"id\":1}]"), List.of(), actor("OWNER", 1L));
        assertTrue(events.contains("upsertOripa"));

        events.clear();
        service.upsertOripa(1L, request("[{\"id\":2},{\"id\":1}]"), List.of(), admin);
        assertTrue(events.contains("upsertOripa"));
    }

    @Test void userAndOwnerOfAnotherPlaceCannotUpdateOripa() throws Exception {
        var data = request("[]");
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.upsertOripa(1L, data, List.of(), actor("USER", null)))
                .getRawStatusCode());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.upsertOripa(1L, data, List.of(), actor("OWNER", 2L)))
                .getRawStatusCode());
        assertTrue(events.isEmpty());
    }

    @Test void ownerAssignmentMustPointToAnOripaPlace() throws Exception {
        place.setType("POKEMON_VENDING");
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.upsertOripa(1L, request("[]"), List.of(), actor("OWNER", 1L)))
                .getRawStatusCode());
        assertFalse(events.contains("upsertOripa"));
    }

    @Test void onlyAdminCanDeleteAtServiceBoundary() {
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.deletePlace(1L, actor("OWNER", 1L))).getRawStatusCode());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> service.deletePlace(1L, actor("USER", null))).getRawStatusCode());
        assertFalse(events.contains("deletePlace"));
    }

    @Test void multipartPutBindsJsonAndFiles() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new PlaceController(service))
                .setCustomArgumentResolvers(
                        new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .build();
        var data = new org.springframework.mock.web.MockMultipartFile("data", "", "application/json",
                new ObjectMapper().writeValueAsBytes(request("[{\"id\":2},{\"fileIndex\":0}]")));
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(authentication(admin));
        org.springframework.mock.web.MockHttpServletResponse response;
        try {
            response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                    .multipart(org.springframework.http.HttpMethod.PUT, "/api/places/1/oripa")
                    .file(data).file((org.springframework.mock.web.MockMultipartFile) files(1).get(0)))
                    .andReturn().getResponse();
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
        assertEquals(200, response.getStatus());
        assertEquals("updated", new ObjectMapper().readTree(response.getContentAsString())
                .get("oripaPlace").get("summary").asText());
    }

    private static MemberVO actor(String role, Long placeId) {
        return MemberVO.builder().id(7L).username("actor@example.com").password("unused")
                .role(role).placeId(placeId).authList(List.of()).build();
    }

    private static org.springframework.security.core.Authentication authentication(MemberVO member) {
        var principal = new org.scoula.security.account.domain.CustomUser(member);
        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
    }
}
