package org.scoula.editrequest.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class EditRequestRequest {

    private Long placeId;
    private List<String> requestTypes;
    private String memo;
}