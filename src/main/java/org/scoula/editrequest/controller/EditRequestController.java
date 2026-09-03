package org.scoula.editrequest.controller;

import lombok.RequiredArgsConstructor;
import org.scoula.editrequest.dto.EditRequestRequest;
import org.scoula.editrequest.service.EditRequestService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/edit-requests")
@RequiredArgsConstructor
public class EditRequestController {

    private final EditRequestService editRequestService;

    @PostMapping
    public void addEditRequest(
            @RequestBody EditRequestRequest request
    ) {
        editRequestService.addEditRequest(request);
    }
}