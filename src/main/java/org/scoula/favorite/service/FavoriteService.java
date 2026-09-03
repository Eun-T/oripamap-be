package org.scoula.favorite.service;

import lombok.RequiredArgsConstructor;
import org.scoula.favorite.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;

    private static final Long TEST_USER_ID = 1L;

    public void addFavorite(Long placeId) {
        favoriteMapper.insertFavorite(
                TEST_USER_ID,
                placeId
        );
    }

    public void removeFavorite(Long placeId) {
        favoriteMapper.deleteFavorite(
                TEST_USER_ID,
                placeId
        );
    }

    public boolean isFavorite(Long placeId) {
        return favoriteMapper.existsFavorite(
                TEST_USER_ID,
                placeId
        ) > 0;
    }
}