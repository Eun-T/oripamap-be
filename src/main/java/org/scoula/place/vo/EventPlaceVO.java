package org.scoula.place.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class EventPlaceVO {
    private Long placeId;
    private EventType eventType;
    private String countryCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private String eventHours;
    private String benefits;
    private String notice;
    private String summary;
    private String introduction;
    private String socialLinks;
}
