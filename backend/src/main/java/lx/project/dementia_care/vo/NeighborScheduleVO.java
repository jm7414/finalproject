package lx.project.dementia_care.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class NeighborScheduleVO {
    private int scheduleNo;
    private String scheduleTitle;
    private String content;
    private LocalDate scheduleDate;
    private int userNo;              // 일정 작성자
    private String userName;         // 작성자 이름 (조회 결과용)
    private LocalDateTime createdAt;
    
    // Getters and Setters
    public int getScheduleNo() { return scheduleNo; }
    public void setScheduleNo(int scheduleNo) { this.scheduleNo = scheduleNo; }
    
    public String getScheduleTitle() { return scheduleTitle; }
    public void setScheduleTitle(String scheduleTitle) { this.scheduleTitle = scheduleTitle; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public LocalDate getScheduleDate() { return scheduleDate; }
    public void setScheduleDate(LocalDate scheduleDate) { this.scheduleDate = scheduleDate; }
    
    public int getUserNo() { return userNo; }
    public void setUserNo(int userNo) { this.userNo = userNo; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
