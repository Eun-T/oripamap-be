package org.scoula.editrequest.service;

import lombok.RequiredArgsConstructor;
import org.scoula.editrequest.dto.EditRequestRequest;
import org.scoula.editrequest.mapper.EditRequestMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EditRequestService {

    private final EditRequestMapper editRequestMapper;

    private static final Long TEST_USER_ID = 1L;

    public void addEditRequest(EditRequestRequest request) {

        String requestTypes =
                String.join(",", request.getRequestTypes());

        editRequestMapper.insertEditRequest(
                request.getPlaceId(),
                TEST_USER_ID,
                requestTypes,
                request.getMemo()
        );
    }
}