package lx.project.dementia_care.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationDataVO {
    
    private Integer locationNo;        // DB의 location_no (PK)
    private Integer userNo;            // 사용자 번호
    private BigDecimal latitude;       // 위도
    private BigDecimal longitude;      // 경도
    private LocalDateTime recordTime;  // 기록 시간
}
