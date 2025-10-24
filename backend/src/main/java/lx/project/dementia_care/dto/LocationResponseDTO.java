package lx.project.dementia_care.dto;

import lombok.Data;

@Data
public class LocationResponseDTO {
    private Integer userNo;        // 환자 번호
    private Double latitude;       // 위도
    private Double longitude;      // 경도
    private Long timestamp;        // 위치 기록 시간
    private String status;         // 상태 (online/offline)
    private Long lastUpdateTime;   // 마지막 업데이트 시간
}
