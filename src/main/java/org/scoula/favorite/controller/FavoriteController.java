package org.scoula.favorite.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.favorite.service.FavoriteService;
import org.scoula.place.dto.PlaceResponse;
import org.scoula.security.account.domain.CustomUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{placeId}")
    public ResponseEntity<Void> addFavorite(@PathVariable Long placeId,
                                            @AuthenticationPrincipal CustomUser user) {
        boolean created = favoriteService.addFavorite(user.getMember().getId(), placeId);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<Void> removeFavorite(@PathVariable Long placeId,
                                               @AuthenticationPrincipal CustomUser user) {
        boolean deleted = favoriteService.removeFavorite(user.getMember().getId(), placeId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{placeId}")
    public boolean isFavorite(@PathVariable Long placeId,
                              @AuthenticationPrincipal CustomUser user) {
        return favoriteService.isFavorite(user.getMember().getId(), placeId);
    }

    @GetMapping
    public List<PlaceResponse> getFavorites(@AuthenticationPrincipal CustomUser user) {
        return favoriteService.getFavorites(user.getMember().getId());
    }
}
