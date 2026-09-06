package org.scoula.editrequest.controller;

import org.junit.jupiter.api.Test;
import org.scoula.editrequest.dto.EditRequestRequest;
import org.scoula.editrequest.mapper.EditRequestMapper;
import org.scoula.editrequest.service.EditRequestService;
import org.scoula.security.account.domain.CustomUser;
import org.scoula.security.account.domain.MemberVO;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EditRequestControllerTest {

    @Test
    void savesAuthenticatedUserId() {
        TrackingEditRequestMapper mapper = new TrackingEditRequestMapper();
        EditRequestController controller = new EditRequestController(new EditRequestService(mapper));
        EditRequestRequest request = new EditRequestRequest();
        request.setPlaceId(10L);
        request.setRequestTypes(Arrays.asList("LOCATION", "NAME"));
        request.setMemo("수정 요청");

        controller.addEditRequest(request, authenticatedUser(42L));

        assertEquals(10L, mapper.placeId);
        assertEquals(42L, mapper.userId);
        assertEquals("LOCATION,NAME", mapper.requestTypes);
        assertEquals("수정 요청", mapper.memo);
    }

    private CustomUser authenticatedUser(Long userId) {
        return new CustomUser(MemberVO.builder()
                .id(userId)
                .username("owner@example.com")
                .password("unused")
                .authList(Collections.emptyList())
                .build());
    }

    private static class TrackingEditRequestMapper implements EditRequestMapper {
        private Long placeId;
        private Long userId;
        private String requestTypes;
        private String memo;

        @Override
        public int insertEditRequest(Long placeId, Long userId, String requestTypes, String memo) {
            this.placeId = placeId;
            this.userId = userId;
            this.requestTypes = requestTypes;
            this.memo = memo;
            return 1;
        }
    }
}
