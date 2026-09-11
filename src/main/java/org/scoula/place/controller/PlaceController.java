package org.scoula.place.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.place.dto.PlaceResponse;
import org.scoula.place.dto.OripaPlaceRequest;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.scoula.place.service.PlaceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @PutMapping(value = "/{placeId}/oripa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PlaceResponse upsertOripa(
            @PathVariable("placeId") Long placeId,
            @RequestPart("data") OripaPlaceRequest data,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal CustomUser user) {
        return placeService.upsertOripa(
                placeId, data, files == null ? List.of() : files,
                user == null ? null : user.getMember());
    }

    @GetMapping("/{id}")
    public PlaceResponse getPlace(@PathVariable("id") Long id) {
        return placeService.getPlace(id);
    }

    @GetMapping("/public/{publicId}")
    public PlaceResponse getPlaceByPublicId(@PathVariable("publicId") String publicId) {
        return placeService.getPlaceByPublicId(publicId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlace(@PathVariable("id") Long id,
                            @AuthenticationPrincipal CustomUser user) {
        placeService.deletePlace(id, user == null ? null : user.getMember());
    }

    @GetMapping
    public List<PlaceResponse> getPlaces() {
        return placeService.getPlaces();
    }

    @GetMapping("/search")
    public List<PlaceResponse> searchPlaces(
            @RequestParam String keyword
    ) {
        return placeService.searchPlaces(keyword);
    }
}
