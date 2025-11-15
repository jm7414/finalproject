package lx.project.dementia_care.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 활성 멤버 DTO (광장 버퍼 안에 있는 이웃)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActiveMemberDTO {
    private Integer userNo;              // 사용자 번호
    private String name;                 // 사용자 이름
    private String profilePhoto;         // 프로필 사진
    private BigDecimal latitude;         // 현재 위도
    private BigDecimal longitude;        // 현재 경도
    private Double distanceFromCenter;   // 중심으로부터의 거리 (미터)
}
