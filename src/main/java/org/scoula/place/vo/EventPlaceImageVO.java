package org.scoula.place.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventPlaceImageVO {
    private Long id;
    private String imageKey;
    private Integer sortOrder;
    private EventPlaceImageType imageType;
}
