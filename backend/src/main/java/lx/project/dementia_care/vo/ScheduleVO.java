package lx.project.dementia_care.vo;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleVO {
    private Integer scheduleNo;
    private String scheduleTitle;
    private String content;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer userNo;

    @Override
    public String toString() {
        return "ScheduleVO [scheduleNo=" + scheduleNo + ", scheduleTitle=" + scheduleTitle 
                + ", content=" + content + ", scheduleDate=" + scheduleDate 
                + ", startTime=" + startTime + ", endTime=" + endTime 
                + ", userNo=" + userNo + "]";
    }
}

