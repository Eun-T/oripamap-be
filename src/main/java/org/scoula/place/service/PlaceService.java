package org.scoula.place.service;

import lombok.RequiredArgsConstructor;
import org.scoula.place.dto.PlaceResponse;
import org.scoula.place.mapper.PlaceMapper;
import org.scoula.place.vo.PlaceVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceMapper placeMapper;

    public List<PlaceResponse> getPlaces() {

        return placeMapper.findAll()
                .stream()
                .map(place -> PlaceResponse.builder()
                        .id(place.getId())
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
}
