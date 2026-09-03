package org.scoula.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResponse {

    private Long id;
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