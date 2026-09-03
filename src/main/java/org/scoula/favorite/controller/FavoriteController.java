package org.scoula.favorite.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.favorite.service.FavoriteService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{placeId}")
    public void addFavorite(@PathVariable Long placeId) {
        favoriteService.addFavorite(placeId);
    }

    @DeleteMapping("/{placeId}")
    public void removeFavorite(@PathVariable Long placeId) {
        favoriteService.removeFavorite(placeId);
    }

    @GetMapping("/{placeId}")
    public boolean isFavorite(@PathVariable Long placeId) {
        return favoriteService.isFavorite(placeId);
    }
}