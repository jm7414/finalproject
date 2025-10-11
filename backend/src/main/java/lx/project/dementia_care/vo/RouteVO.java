package lx.project.dementia_care.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteVO {
    private Integer routeNo;
    private String routeCoordinates;      // JSON 형식
    private String bufferCoordinates;     // JSON 형식
    private Integer scheduleNo;

    @Override
    public String toString() {
        return "RouteVO [routeNo=" + routeNo + ", scheduleNo=" + scheduleNo + "]";
    }
}

