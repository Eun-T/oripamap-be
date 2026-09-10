package org.scoula.place.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TagResponse {
    private Long id;
    private String name;
    private String category;
}
