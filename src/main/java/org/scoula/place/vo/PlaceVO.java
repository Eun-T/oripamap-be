package org.scoula.place.vo;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class PlaceVO {

    private Long id;
    private String type;

    private String name;
    private String branchName;

    private String address;
    private String locationDetail;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private String imageUrl;
    private String businessHours;
    private String holidayInfo;

    private String phone;
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
