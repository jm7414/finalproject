package lx.project.dementia_care.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 광장 생성 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePlazaRequestDTO {
    private String plazaName;      // 광장 이름
    private Double centerLat;      // 중심 위도
    private Double centerLng;      // 중심 경도
    private Integer radiusMeters;  // 반경 (미터)
}
