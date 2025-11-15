package lx.project.dementia_care.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 광장 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlazaInfoDTO {
    private Integer plazaNo;       // 광장 번호
    private String plazaName;      // 광장 이름
    private Double centerLat;      // 중심 위도
    private Double centerLng;      // 중심 경도
    private Integer radius;        // 반경 (미터)
}
