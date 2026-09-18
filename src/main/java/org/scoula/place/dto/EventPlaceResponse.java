package org.scoula.place.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import org.scoula.place.vo.EventPlaceImageType;
import org.scoula.place.vo.EventType;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class EventPlaceResponse {
    private Long placeId;
    private EventType eventType;
    private String countryCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endDate;
    private String eventHours;
    private String benefits;
    private String notice;
    private String summary;
    private String introduction;
    private JsonNode socialLinks;
    private List<Image> images;

    @Getter
    @Builder
    public static class Image {
        private Long id;
        private String imageUrl;
        private Integer sortOrder;
        private EventPlaceImageType imageType;
    }
}
