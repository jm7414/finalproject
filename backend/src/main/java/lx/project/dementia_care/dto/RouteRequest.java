package lx.project.dementia_care.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RouteRequest {
    private String startName; // 출발지 이름
    private double startX;
    private double startY;
    private String endName;	// 도착지 이름
    private double endX;
    private double endY;
    private String reqCoordType;   // 요청 좌표계
    private String resCoordType;   // 응답 좌표계
    private String searchOption;   // 검색 옵션

    public RouteRequest() {}

    // 생성자 (필요 시 사용)
    public RouteRequest(String startName, double startX, double startY,
                        String endName,   double endX,   double endY,
                        String reqCoordType, String resCoordType, String searchOption) {
        this.startName     = startName;
        this.startX        = startX;
        this.startY        = startY;
        this.endName       = endName;
        this.endX          = endX;
        this.endY          = endY;
        this.reqCoordType  = reqCoordType;
        this.resCoordType  = resCoordType;
        this.searchOption  = searchOption;
    }
}
