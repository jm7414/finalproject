package lx.project.dementia_care.vo;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 광장 멤버 VO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlazaVO {
    private Integer plazaMemberNo;     // 멤버 고유 번호 (PK)
    private Integer plazaNo;           // 광장 번호 (그룹핑)
    private String plazaName;          // 광장 이름
    private String memberName;         // 멤버 역할 (방장/이웃)
    private Integer userNo;            // 사용자 번호
}
