package org.scoula.favorite.service;

import lombok.RequiredArgsConstructor;
import org.scoula.favorite.mapper.FavoriteMapper;
import org.scoula.place.dto.PlaceResponse;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;

    public boolean addFavorite(Long userId, Long placeId) {
        return favoriteMapper.insertFavorite(userId, placeId) > 0;
    }

    public boolean removeFavorite(Long userId, Long placeId) {
        return favoriteMapper.deleteFavorite(userId, placeId) > 0;
    }

    public boolean isFavorite(Long userId, Long placeId) {
        return favoriteMapper.existsFavorite(userId, placeId) > 0;
    }

    public List<PlaceResponse> getFavorites(Long userId) {
        return favoriteMapper.findPlacesByUserId(userId).stream()
                .map(place -> PlaceResponse.builder()
                        .id(place.getId()).publicId(place.getPublicId())
                        .type(place.getType()).name(place.getName())
                        .branchName(place.getBranchName()).address(place.getAddress())
                        .locationDetail(place.getLocationDetail())
                        .latitude(place.getLatitude()).longitude(place.getLongitude())
                        .businessHours(place.getBusinessHours()).holidayInfo(place.getHolidayInfo())
                        .phone(place.getPhone()).description(place.getDescription())
                        .imageUrl(place.getImageUrl()).build())
                .toList();
    }
}
