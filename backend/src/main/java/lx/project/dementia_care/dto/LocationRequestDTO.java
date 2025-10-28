package lx.project.dementia_care.dto;

import lombok.Data;

@Data
public class LocationRequestDTO {
    private Integer userNo;        // 환자 번호
    private Double latitude;       // 위도
    private Double longitude;      // 경도
    private Long timestamp;        // 타임스탬프 (선택사항)
}
