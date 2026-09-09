package org.scoula.place.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class OripaPlaceResponse {
    private Long placeId;
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
    }
}
