package lx.project.dementia_care.vo;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserVO implements UserDetails {

    private Integer userNo;
    private String userId;
    private String userPw;
    private String name;
    private LocalDate birthDate;
    private String phoneNumber;
    private String invitationCode;
    private Integer subscriptionStatus;
    private Integer userStatus;
    private String profilePhoto;
    private Integer roleNo;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roleNo == null) {
            return Collections.emptyList();
        }
        String role;
        switch (roleNo) {
            case 1:
                role = "ROLE_GUARDIAN";  // 보호자
                break;
            case 2:
                role = "ROLE_PATIENT";   // 환자
                break;
            case 3:
                role = "ROLE_SUBSCRIBER"; // 구독자 (보호자와 동일한 권한)
                break;
            default:
                role = "ROLE_USER";
                break;
        }
        return Collections.singleton(() -> role);
    }

    @Override
    public String getPassword() {
        return this.userPw;
    }

    @Override
    public String getUsername() {
        return this.userId;
    }

    @Override
    public String toString() {
        return "UserVO [userNo=" + userNo + ", userId=" + userId + ", userPw=" + userPw 
                + ", name=" + name + ", birthDate=" + birthDate + ", phoneNumber=" + phoneNumber 
                + ", invitationCode=" + invitationCode + ", subscriptionStatus=" + subscriptionStatus 
                + ", userStatus=" + userStatus + ", profilePhoto=" + profilePhoto 
                + ", roleNo=" + roleNo + "]";
    }

}