package lx.project.dementia_care.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NeighborFriendVO {
    private Integer friendshipId;
    private Integer userNo;
    private String userId;  // 지현 수정: 추가
    private String name;
    private String profilePhoto;
    private String phoneNumber;  // 지현 수정: 추가
}
