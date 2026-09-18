package org.scoula.place.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.scoula.place.vo.EventPlaceImageType;
import org.scoula.place.vo.EventType;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class EventPlaceRequest {
    private EventType eventType;
    private String countryCode;
    @Setter(AccessLevel.NONE)
    private boolean countryCodePresent;
    private LocalDate startDate;
    private LocalDate endDate;
    private String eventHours;
    private String benefits;
    private String notice;
    private String summary;
    private String introduction;

    @JsonAlias("social_links")
    private JsonNode socialLinks;

    // Required final image order. Omitted existing IDs are deleted.
    private List<Image> images;

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
        this.countryCodePresent = true;
    }

    @JsonIgnore
    public boolean isCountryCodePresent() {
        return countryCodePresent;
    }

    @Getter
    @Setter
    public static class Image {
        private Long id;
        private Integer fileIndex;
        private EventPlaceImageType imageType;
    }
}
