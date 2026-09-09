package org.scoula.place.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class OripaPlaceRequest {
    private String summary;
    private String introduction;
    private JsonNode socialLinks;
    // Required final image order. Omitted existing IDs are deleted.
    private List<Image> images;

    @Getter
    @Setter
    public static class Image {
        private Long id;
        private Integer fileIndex;
    }
}
