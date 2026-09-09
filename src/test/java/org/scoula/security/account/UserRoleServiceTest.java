package org.scoula.security.account;

import org.junit.jupiter.api.Test;
import org.scoula.place.mapper.PlaceMapper;
import org.scoula.place.vo.PlaceVO;
import org.scoula.security.account.mapper.UserDetailsMapper;
import org.scoula.security.account.service.UserRoleService;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserRoleServiceTest {

    Long updatedUserId;
    String updatedRole;
    Long updatedPlaceId;
    PlaceVO place = place("ORIPA");
    int updateCount = 1;

    final UserDetailsMapper users = (UserDetailsMapper) Proxy.newProxyInstance(
            UserDetailsMapper.class.getClassLoader(), new Class<?>[]{UserDetailsMapper.class},
            (proxy, method, args) -> {
                if ("updateRoleAndPlace".equals(method.getName())) {
                    updatedUserId = (Long) args[0];
                    updatedRole = (String) args[1];
                    updatedPlaceId = (Long) args[2];
                    return updateCount;
                }
                throw new UnsupportedOperationException(method.getName());
            });

    final PlaceMapper places = (PlaceMapper) Proxy.newProxyInstance(
            PlaceMapper.class.getClassLoader(), new Class<?>[]{PlaceMapper.class},
            (proxy, method, args) -> {
                if ("findById".equals(method.getName())) return place;
                throw new UnsupportedOperationException(method.getName());
            });

    final UserRoleService service = new UserRoleService(users, places);

    @Test
    void ownerCanBeAssignedToOneOripaPlace() {
        service.assignOwner(7L, 10L);
        assertEquals(7L, updatedUserId);
        assertEquals("OWNER", updatedRole);
        assertEquals(10L, updatedPlaceId);
    }

    @Test
    void ownerCannotBeAssignedToNonOripaPlace() {
        place = place("POKEMON_VENDING");
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.assignOwner(7L, 10L)).getRawStatusCode());
    }

    @Test
    void userAndAdminNeverKeepAPlaceAssignment() {
        service.assignAdmin(7L);
        assertEquals("ADMIN", updatedRole);
        assertEquals(null, updatedPlaceId);
        service.assignUser(7L);
        assertEquals("USER", updatedRole);
        assertEquals(null, updatedPlaceId);
    }

    @Test
    void missingUserOrPlaceIsRejected() {
        place = null;
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.assignOwner(7L, 10L)).getRawStatusCode());
        place = place("ORIPA");
        updateCount = 0;
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.assignOwner(7L, 10L)).getRawStatusCode());
    }

    private static PlaceVO place(String type) {
        PlaceVO place = new PlaceVO();
        place.setId(10L);
        place.setType(type);
        return place;
    }
}
