package org.scoula.security.account.service;

import lombok.RequiredArgsConstructor;
import org.scoula.place.mapper.PlaceMapper;
import org.scoula.place.vo.PlaceVO;
import org.scoula.security.account.mapper.UserDetailsMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserDetailsMapper userDetailsMapper;
    private final PlaceMapper placeMapper;

    @Transactional
    public void assignOwner(Long userId, Long placeId) {
        PlaceVO place = placeMapper.findById(placeId);
        if (place == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다.");
        }
        if (!"ORIPA".equals(place.getType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "OWNER는 ORIPA 매장에만 연결할 수 있습니다.");
        }
        update(userId, "OWNER", placeId);
    }

    @Transactional
    public void assignAdmin(Long userId) {
        update(userId, "ADMIN", null);
    }

    @Transactional
    public void assignUser(Long userId) {
        update(userId, "USER", null);
    }

    private void update(Long userId, String role, Long placeId) {
        if (userDetailsMapper.updateRoleAndPlace(userId, role, placeId) != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
    }
}
