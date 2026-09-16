package org.scoula.place.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.scoula.common.service.S3ImageService;
import org.scoula.place.dto.OripaPlaceResponse;
import org.scoula.place.dto.EventPlaceRequest;
import org.scoula.place.dto.EventPlaceResponse;
import org.scoula.place.vo.EventPlaceVO;
import org.scoula.place.vo.EventPlaceImageType;
import org.scoula.place.vo.EventPlaceImageVO;
import org.scoula.place.vo.EventType;
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
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
                PlaceVO place = placeMapper.findById(placeId);
                if (!"ORIPA".equals(place.getType())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ORIPA 매장만 등록/수정할 수 있습니다.");
                }
                requireLocation(place);
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

    public PlaceResponse upsertEvent(Long placeId, EventPlaceRequest data, List<MultipartFile> files,
                                     MemberVO actor) {
        requirePlaceManager(actor, placeId);
        if (data == null || data.getImages() == null || data.getSocialLinks() == null
                || !data.getSocialLinks().isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "images and socialLinks must be arrays.");
        }
        Set<Long> retained = new HashSet<>();
        Set<Integer> indexes = new HashSet<>();
        for (EventPlaceRequest.Image image : data.getImages()) {
            if (image == null || (image.getId() == null) == (image.getFileIndex() == null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each image must specify exactly one of id or fileIndex.");
            }
            if (image.getId() != null) {
                if (image.getId() <= 0 || !retained.add(image.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Image IDs must be positive and unique.");
                }
            } else if (image.getImageType() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "New images must specify imageType.");
            } else if (image.getFileIndex() < 0 || image.getFileIndex() >= files.size()
                    || !indexes.add(image.getFileIndex())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "fileIndex values must be valid and unique.");
            }
        }
        if (indexes.size() != files.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Every uploaded file must be referenced by images.");
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
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found.");
                }
                PlaceVO place = placeMapper.findById(placeId);
                if (!"EVENT".equals(place.getType())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Only EVENT places can have event details.");
                }
                EventType eventType = data.getEventType() != null
                        ? data.getEventType()
                        : Objects.requireNonNullElse(place.getEventType(), EventType.OFFLINE);
                if (eventType == EventType.OFFLINE) {
                    requireLocation(place);
                }
                var existing = placeMapper.findEventImagesByPlaceId(placeId);
                Set<Long> owned = new HashSet<>();
                existing.forEach(image -> owned.add(image.getId()));
                if (!owned.containsAll(retained)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "An image ID does not belong to this event or does not exist.");
                }
                Map<Long, EventPlaceImageVO> existingById = existing.stream()
                        .collect(Collectors.toMap(
                                EventPlaceImageVO::getId,
                                Function.identity()));
                EventPlaceVO detail = new EventPlaceVO();
                detail.setPlaceId(placeId);
                detail.setEventType(eventType);
                detail.setStartDate(data.getStartDate());
                detail.setEndDate(data.getEndDate());
                detail.setEventHours(data.getEventHours());
                detail.setBenefits(data.getBenefits());
                detail.setNotice(data.getNotice());
                detail.setSummary(data.getSummary());
                detail.setIntroduction(data.getIntroduction());
                detail.setSocialLinks(data.getSocialLinks().toString());
                placeMapper.upsertEvent(detail);
                for (var image : existing) {
                    if (!retained.contains(image.getId())) {
                        if (placeMapper.deleteEventImage(placeId, image.getId()) != 1) {
                            throw new IllegalStateException("Failed to delete the event image.");
                        }
                        deleted.add(image.getImageKey());
                    }
                }
                Map<EventPlaceImageType, Integer> nextOrder = new EnumMap<>(EventPlaceImageType.class);
                for (var image : data.getImages()) {
                    EventPlaceImageType imageType = image.getImageType();
                    if (image.getId() != null && imageType == null) {
                        imageType = existingById.get(image.getId()).getImageType();
                    }
                    if (imageType == null) {
                        throw new IllegalStateException("Existing event image has no imageType.");
                    }
                    int order = nextOrder.getOrDefault(imageType, 0);
                    nextOrder.put(imageType, order + 1);
                    if (image.getId() != null) {
                        placeMapper.updateEventImageOrder(placeId, image.getId(), order, imageType);
                    } else {
                        String key = s3ImageService.upload(files.get(image.getFileIndex()));
                        uploaded.add(key);
                        if (placeMapper.insertEventImage(placeId, key, order, imageType) != 1) {
                            throw new IllegalStateException("Failed to save the event image.");
                        }
                    }
                }
                return getPlace(placeId);
            });
        } catch (RuntimeException e) {
            if (completion[0] == TransactionSynchronization.STATUS_ROLLED_BACK) {
                for (String key : uploaded) {
                    try { s3ImageService.delete(key); }
                    catch (RuntimeException cleanupError) {
                        e.addSuppressed(cleanupError);
                        log.error("EVENT save rollback S3 cleanup failed: placeId={}, key={}",
                                placeId, key, cleanupError);
                    }
                }
            } else if (!uploaded.isEmpty()) {
                log.error("EVENT commit result requires verification: placeId={}, keys={}",
                        placeId, uploaded, e);
            }
            throw e;
        }
        RuntimeException failure = null;
        for (String key : deleted.stream().distinct().toList()) {
            try { s3ImageService.delete(key); }
            catch (RuntimeException e) {
                log.error("EVENT post-commit S3 delete failed: placeId={}, key={}", placeId, key, e);
                if (failure == null) failure = e;
                else failure.addSuppressed(e);
            }
        }
        if (failure != null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Event details were saved, but image file cleanup failed.", failure);
        }
        return response;
    }

    public PlaceResponse getPlace(Long id) {
        PlaceVO place = placeMapper.findById(id);
        if (place == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다.");
        }
        OripaPlaceResponse oripa = null;
        EventPlaceResponse event = null;
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
        if ("EVENT".equals(place.getType())) {
            EventPlaceVO detail = placeMapper.findEventByPlaceId(id);
            if (detail != null) {
                event = EventPlaceResponse.builder()
                        .placeId(detail.getPlaceId())
                        .eventType(detail.getEventType())
                        .startDate(detail.getStartDate())
                        .endDate(detail.getEndDate())
                        .eventHours(detail.getEventHours())
                        .benefits(detail.getBenefits())
                        .notice(detail.getNotice())
                        .summary(detail.getSummary())
                        .introduction(detail.getIntroduction())
                        .socialLinks(parseSocialLinks(detail.getSocialLinks()))
                        .images(placeMapper.findEventImagesByPlaceId(id).stream()
                                .map(image -> EventPlaceResponse.Image.builder()
                                         .id(image.getId())
                                         .sortOrder(image.getSortOrder())
                                         .imageType(image.getImageType())
                                         .imageUrl(s3ImageService.createPresignedGetUrl(image.getImageKey()))
                                        .build())
                                .toList())
                        .build();
            }
        }
        return PlaceResponse.builder()
                .id(place.getId()).publicId(place.getPublicId())
                .type(place.getType()).eventType(place.getEventType()).name(place.getName())
                .branchName(place.getBranchName()).address(place.getAddress())
                .locationDetail(place.getLocationDetail())
                .latitude(place.getLatitude()).longitude(place.getLongitude())
                .businessHours(place.getBusinessHours()).holidayInfo(place.getHolidayInfo())
                .phone(place.getPhone()).description(place.getDescription())
                .imageUrl(resolvePlaceImageUrl(place)).oripaPlace(oripa).eventPlace(event)
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
            List<String> imageKeys = new ArrayList<>();
            imageKeys.addAll(placeMapper.findOripaImagesByPlaceId(id).stream()
                    .map(image -> image.getImageKey()).toList());
            imageKeys.addAll(placeMapper.findEventImagesByPlaceId(id).stream()
                    .map(image -> image.getImageKey()).toList());
            if (placeMapper.deletePlace(id) != 1) {
                throw new IllegalStateException("장소 삭제에 실패했습니다.");
            }
            return imageKeys.stream().distinct().toList();
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
                        .eventType(place.getEventType())
                        .name(place.getName())
                        .branchName(place.getBranchName())
                        .address(place.getAddress())
                        .locationDetail(place.getLocationDetail())
                        .latitude(place.getLatitude())
                        .longitude(place.getLongitude())
                        .businessHours(place.getBusinessHours())
                        .imageUrl(resolvePlaceImageUrl(place))
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
                        .eventType(place.getEventType())
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
                        .imageUrl(resolvePlaceImageUrl(place))
                        .build())
                .toList();
    }

    private String resolvePlaceImageUrl(PlaceVO place) {
        if (!"EVENT".equals(place.getType())) {
            return place.getImageUrl();
        }
        return place.getEventImageKey() == null
                ? null
                : s3ImageService.createPresignedGetUrl(place.getEventImageKey());
    }

    private void requireLocation(PlaceVO place) {
        if (place.getAddress() == null || place.getAddress().isBlank()
                || place.getLatitude() == null || place.getLongitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "address, latitude, and longitude are required for this place type.");
        }
    }

    private void requireOripaManager(MemberVO actor, Long placeId) {
        requirePlaceManager(actor, placeId);
    }

    private void requirePlaceManager(MemberVO actor, Long placeId) {
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
