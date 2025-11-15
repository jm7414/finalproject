package lx.project.dementia_care.dto;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * 광장 일정 DTO
 * - 광장 멤버들이 공유하는 일정 정보
 */
public class PlazaScheduleDTO {
    private Integer scheduleNo;
    private String scheduleTitle;
    private String content;
    private Date scheduleDate;
    private Integer userNo;
    private String authorName; // 작성자 이름
    private Integer plazaNo;
    private Timestamp createdAt;

    // 기본 생성자
    public PlazaScheduleDTO() {
    }

    // Getters and Setters
    public Integer getScheduleNo() {
        return scheduleNo;
    }

    public void setScheduleNo(Integer scheduleNo) {
        this.scheduleNo = scheduleNo;
    }

    public String getScheduleTitle() {
        return scheduleTitle;
    }

    public void setScheduleTitle(String scheduleTitle) {
        this.scheduleTitle = scheduleTitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(Date scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public Integer getUserNo() {
        return userNo;
    }

    public void setUserNo(Integer userNo) {
        this.userNo = userNo;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Integer getPlazaNo() {
        return plazaNo;
    }

    public void setPlazaNo(Integer plazaNo) {
        this.plazaNo = plazaNo;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "PlazaScheduleDTO{" +
                "scheduleNo=" + scheduleNo +
                ", scheduleTitle='" + scheduleTitle + '\'' +
                ", content='" + content + '\'' +
                ", scheduleDate=" + scheduleDate +
                ", userNo=" + userNo +
                ", authorName='" + authorName + '\'' +
                ", plazaNo=" + plazaNo +
                ", createdAt=" + createdAt +
                '}';
    }
}
