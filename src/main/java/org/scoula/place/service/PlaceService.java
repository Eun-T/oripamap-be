package org.scoula.place.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.scoula.common.service.S3ImageService;
import org.scoula.place.dto.OripaPlaceResponse;
import org.scoula.place.vo.OripaPlaceVO;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import org.scoula.place.dto.PlaceResponse;
import org.scoula.place.dto.TagResponse;
import org.scoula.place.mapper.PlaceMapper;
import org.scoula.place.vo.PlaceVO;
import org.scoula.security.account.domain.MemberVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Objects;
import org.scoula.place.dto.OripaPlaceRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Log4j2
public class PlaceService {

    private final PlaceMapper placeMapper;
    private final S3ImageService s3ImageService;
    private final PlatformTransactionManager transactionManager;
    private static final ObjectMapper JSON = new ObjectMapper();

    public PlaceResponse upsertOripa(Long placeId, OripaPlaceRequest data, List<MultipartFile> files,
                                     MemberVO actor) {
        requireOripaManager(actor, placeId);
        if (data == null || data.getImages() == null || data.getSocialLinks() == null
                || !data.getSocialLinks().isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "images와 socialLinks 배열이 필요합니다.");
        }
        Set<Long> retained = new HashSet<>();
        Set<Integer> indexes = new HashSet<>();
        for (OripaPlaceRequest.Image image : data.getImages()) {
            if (image == null || (image.getId() == null) == (image.getFileIndex() == null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "각 이미지에는 id 또는 fileIndex 하나만 지정해야 합니다.");
            }
            if (image.getId() != null) {
                if (image.getId() <= 0 || !retained.add(image.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못되거나 중복된 이미지 ID입니다.");
                }
            } else if (image.getFileIndex() < 0 || image.getFileIndex() >= files.size()
                    || !indexes.add(image.getFileIndex())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못되거나 중복된 fileIndex입니다.");
            }
        }
        if (indexes.size() != files.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "모든 신규 파일을 images에 한 번씩 지정해야 합니다.");
        }

        List<String> uploaded = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        int[] completion = {TransactionSynchronization.STATUS_ROLLED_BACK};
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        PlaceResponse response;
        try {
            response = transaction.execute(status -> {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int result) { completion[0] = result; }
                });
                if (placeMapper.findIdForUpdate(placeId) == null) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다.");
                }
                if (!"ORIPA".equals(placeMapper.findById(placeId).getType())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ORIPA 매장만 등록/수정할 수 있습니다.");
                }
                var existing = placeMapper.findOripaImagesByPlaceId(placeId);
                Set<Long> owned = new HashSet<>();
                existing.forEach(image -> owned.add(image.getId()));
                if (!owned.containsAll(retained)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "해당 매장에 속하지 않거나 존재하지 않는 이미지 ID입니다.");
                }
                OripaPlaceVO detail = new OripaPlaceVO();
                detail.setPlaceId(placeId);
                detail.setSummary(data.getSummary());
                detail.setIntroduction(data.getIntroduction());
                detail.setSocialLinks(data.getSocialLinks().toString());
                placeMapper.upsertOripa(detail);
                for (var image : existing) {
                    if (!retained.contains(image.getId())) {
                        if (placeMapper.deleteOripaImage(placeId, image.getId()) != 1) {
                            throw new IllegalStateException("이미지 삭제에 실패했습니다.");
                        }
                        deleted.add(image.getImageKey());
                    }
                }
                for (int order = 0; order < data.getImages().size(); order++) {
                    var image = data.getImages().get(order);
                    if (image.getId() != null) {
                        placeMapper.updateOripaImageOrder(placeId, image.getId(), order);
                    } else {
                        String key = s3ImageService.upload(files.get(image.getFileIndex()));
                        uploaded.add(key);
                        if (placeMapper.insertOripaImage(placeId, key, order) != 1) {
                            throw new IllegalStateException("이미지 저장에 실패했습니다.");
                        }
                    }
                }
                return getPlace(placeId);
            });
        } catch (RuntimeException e) {
            // Match comment uploads: never remove potentially committed images on UNKNOWN.
            if (completion[0] == TransactionSynchronization.STATUS_ROLLED_BACK) {
                for (String key : uploaded) {
                    try { s3ImageService.delete(key); }
                    catch (RuntimeException cleanupError) {
                        e.addSuppressed(cleanupError);
                        log.error("ORIPA 저장 실패 후 S3 정리 실패: placeId={}, key={}", placeId, key, cleanupError);
                    }
                }
            } else if (!uploaded.isEmpty()) {
                log.error("ORIPA 커밋 결과 확인 필요: placeId={}, keys={}", placeId, uploaded, e);
            }
            throw e;
        }
        RuntimeException failure = null;
        for (String key : deleted.stream().distinct().toList()) {
            try { s3ImageService.delete(key); }
            catch (RuntimeException e) {
                log.error("ORIPA 저장 후 S3 삭제 실패: placeId={}, key={}", placeId, key, e);
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "매장 정보는 저장되었지만 이미지 파일 정리에 실패했습니다.", failure);
        }
        return response;
    }

    public PlaceResponse getPlace(Long id) {
        PlaceVO place = placeMapper.findById(id);
        if (place == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다.");
        }
        OripaPlaceResponse oripa = null;
        if ("ORIPA".equals(place.getType())) {
            OripaPlaceVO detail = placeMapper.findOripaByPlaceId(id);
            if (detail != null) {
                oripa = OripaPlaceResponse.builder()
                        .placeId(detail.getPlaceId())
                        .summary(detail.getSummary())
                        .introduction(detail.getIntroduction())
                        .socialLinks(parseSocialLinks(detail.getSocialLinks()))
                        .images(placeMapper.findOripaImagesByPlaceId(id).stream()
                                .map(image -> OripaPlaceResponse.Image.builder()
                                        .id(image.getId())
                                        .sortOrder(image.getSortOrder())
                                        .imageUrl(s3ImageService.createPresignedGetUrl(image.getImageKey()))
                                        .build())
                                .toList())
                        .build();
            }
        }
        return PlaceResponse.builder()
                .id(place.getId()).publicId(place.getPublicId())
                .type(place.getType()).name(place.getName())
                .branchName(place.getBranchName()).address(place.getAddress())
                .locationDetail(place.getLocationDetail())
                .latitude(place.getLatitude()).longitude(place.getLongitude())
                .businessHours(place.getBusinessHours()).holidayInfo(place.getHolidayInfo())
                .phone(place.getPhone()).description(place.getDescription())
                .imageUrl(place.getImageUrl()).oripaPlace(oripa)
                .tags(placeMapper.findTagsByPlaceId(id).stream()
                        .map(tag -> TagResponse.builder()
                                .id(tag.getId())
                                .name(tag.getName())
                                .category(tag.getCategory())
                                .build())
                        .toList())
                .build();
    }

    public PlaceResponse getPlaceByPublicId(String publicId) {
        PlaceVO place = placeMapper.findByPublicId(publicId);
        if (place == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다.");
        }
        return getPlace(place.getId());
    }

    private JsonNode parseSocialLinks(String value) {
        if (value == null) return JSON.createArrayNode();
        try {
            JsonNode links = JSON.readTree(value);
            if (links != null && links.isNull()) return JSON.createArrayNode();
            if (links == null || !links.isArray()) {
                throw new IllegalStateException("social_links must be a JSON array");
            }
            return links;
        } catch (IOException e) {
            throw new IllegalStateException("Invalid social_links JSON", e);
        }
    }

    public void deletePlace(Long id, MemberVO actor) {
        requireAdmin(actor);
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        List<String> keys = transaction.execute(status -> {
            if (placeMapper.findIdForUpdate(id) == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다.");
            }
            List<String> imageKeys = placeMapper.findOripaImagesByPlaceId(id).stream()
                    .map(image -> image.getImageKey()).distinct().toList();
            if (placeMapper.deletePlace(id) != 1) {
                throw new IllegalStateException("장소 삭제에 실패했습니다.");
            }
            return imageKeys;
        });
        // 기존 댓글 삭제와 동일하게 DB 커밋 후 S3 객체를 정리한다.
        RuntimeException failure = null;
        for (String key : keys) {
            try {
                s3ImageService.delete(key);
            } catch (RuntimeException e) {
                log.error("장소는 삭제됐으나 S3 이미지 정리 실패: placeId={}, key={}", id, key, e);
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "장소는 삭제되었지만 이미지 파일 정리에 실패했습니다.", failure);
        }
    }

    public List<PlaceResponse> getPlaces() {

        return placeMapper.findAll()
                .stream()
                .map(place -> PlaceResponse.builder()
                        .id(place.getId())
                        .publicId(place.getPublicId())
                        .type(place.getType())
                        .name(place.getName())
                        .branchName(place.getBranchName())
                        .address(place.getAddress())
                        .locationDetail(place.getLocationDetail())
                        .latitude(place.getLatitude())
                        .longitude(place.getLongitude())
                        .businessHours(place.getBusinessHours())
                        .imageUrl(place.getImageUrl())
                        .holidayInfo(place.getHolidayInfo())
                        .phone(place.getPhone())
                        .description(place.getDescription())
                        .build())
                .toList();
    }

    public List<PlaceResponse> searchPlaces(String keyword) {
        return placeMapper.searchPlaces(keyword)
                .stream()
                .map(place -> PlaceResponse.builder()
                        .id(place.getId())
                        .publicId(place.getPublicId())
                        .type(place.getType())
                        .name(place.getName())
                        .branchName(place.getBranchName())
                        .address(place.getAddress())
                        .locationDetail(place.getLocationDetail())
                        .latitude(place.getLatitude())
                        .longitude(place.getLongitude())
                        .businessHours(place.getBusinessHours())
                        .holidayInfo(place.getHolidayInfo())
                        .phone(place.getPhone())
                        .description(place.getDescription())
                        .imageUrl(place.getImageUrl())
                        .build())
                .toList();
    }

    private void requireOripaManager(MemberVO actor, Long placeId) {
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if ("ADMIN".equals(actor.getRole())) {
            return;
        }
        if ("OWNER".equals(actor.getRole()) && Objects.equals(actor.getPlaceId(), placeId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    private void requireAdmin(MemberVO actor) {
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (!"ADMIN".equals(actor.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
