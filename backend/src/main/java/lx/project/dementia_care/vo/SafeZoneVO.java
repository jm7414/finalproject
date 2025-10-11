package lx.project.dementia_care.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SafeZoneVO {
    private Integer safeZoneNo;
    private String zoneType;              // '기본형' or '경로형'
    private String boundaryCoordinates;   // JSON 형식
    private Integer userNo;

    @Override
    public String toString() {
        return "SafeZoneVO [safeZoneNo=" + safeZoneNo + ", zoneType=" + zoneType 
                + ", userNo=" + userNo + "]";
    }
}

