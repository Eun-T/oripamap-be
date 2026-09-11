package org.scoula.place.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private OripaPlaceResponse oripaPlace;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<TagResponse> tags;

    private Long id;
    private String publicId;
    private String type;
    private String name;
    private String branchName;
    private String address;
    private String locationDetail;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private String businessHours;
    private String holidayInfo;
    private String phone;
    private String description;

    private String imageUrl;
}
