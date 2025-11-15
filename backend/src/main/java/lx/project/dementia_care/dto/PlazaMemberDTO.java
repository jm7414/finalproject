package lx.project.dementia_care.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 광장 멤버 정보 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlazaMemberDTO {
    private Integer userNo;        // 사용자 번호
    private String userName;       // 사용자 이름
    private String memberName;     // 멤버 역할 (방장/이웃)
    private String profilePhoto;   // 프로필 사진
}
