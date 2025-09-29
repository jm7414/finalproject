package lx.project.dementia_care.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoordinateDto {
    private double latitude;
    private double longitude;

    public CoordinateDto() {}

    public CoordinateDto(double latitude, double longitude) {
        this.latitude  = latitude;
        this.longitude = longitude;
    }

}
