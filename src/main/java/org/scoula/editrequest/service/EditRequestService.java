package org.scoula.editrequest.service;

import lombok.RequiredArgsConstructor;
import org.scoula.editrequest.dto.EditRequestRequest;
import org.scoula.editrequest.mapper.EditRequestMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EditRequestService {

    private final EditRequestMapper editRequestMapper;

    public void addEditRequest(EditRequestRequest request, Long userId) {

        String requestTypes =
                String.join(",", request.getRequestTypes());

        editRequestMapper.insertEditRequest(
                request.getPlaceId(),
                userId,
                requestTypes,
                request.getMemo()
        );
    }
}
