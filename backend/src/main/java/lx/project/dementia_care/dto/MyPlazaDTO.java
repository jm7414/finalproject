package lx.project.dementia_care.dto;

public class MyPlazaDTO {
    private Integer plazaNo;
    private String plazaName;
    private String memberName;
    private Integer memberCount;
    private Boolean isOwner;  // 추가
    private Integer activeMemberCount;

    // Getters and Setters
    public Integer getPlazaNo() {
        return plazaNo;
    }

    public void setPlazaNo(Integer plazaNo) {
        this.plazaNo = plazaNo;
    }

    public String getPlazaName() {
        return plazaName;
    }

    public void setPlazaName(String plazaName) {
        this.plazaName = plazaName;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    // 공지 기능으로 인한 추가
    public Boolean isOwner() {
        return isOwner;
    }

    public void setIsOwner(Boolean isOwner) {
        this.isOwner = isOwner;
    }

    public Integer getActiveMemberCount() {
        return activeMemberCount;
    }

    public void setActiveMemberCount(Integer activeMemberCount) {
        this.activeMemberCount = activeMemberCount;
    }
}
