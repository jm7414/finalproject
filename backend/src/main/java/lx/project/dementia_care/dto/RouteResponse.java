package lx.project.dementia_care.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteResponse {
    private List<CoordinateDto> coordinates;

    public RouteResponse() {}

    public RouteResponse(List<CoordinateDto> coordinates) {
        this.coordinates = coordinates;
    }

    public List<CoordinateDto> getCoordinates() { return coordinates; }
    public void setCoordinates(List<CoordinateDto> coordinates) { this.coordinates = coordinates; }
}