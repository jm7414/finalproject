package lx.project.dementia_care.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationDto {
    private String name;
    private Double latitude;
    private Double longitude;
    private Integer sequenceOrder;

    @Override
    public String toString() {
        return "LocationDto [name=" + name + ", latitude=" + latitude 
                + ", longitude=" + longitude + ", sequenceOrder=" + sequenceOrder + "]";
    }
}

