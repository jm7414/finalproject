package lx.project.dementia_care.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 위치 업데이트 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLocationRequestDTO {
    private Double latitude;   // 위도
    private Double longitude;  // 경도
}
