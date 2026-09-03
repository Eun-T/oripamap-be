package org.scoula.place.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.place.dto.PlaceResponse;
import org.scoula.place.service.PlaceService;
import org.scoula.place.vo.PlaceVO;
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
