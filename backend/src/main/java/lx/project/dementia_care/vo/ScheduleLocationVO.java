package lx.project.dementia_care.vo;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleLocationVO {
    private Integer locationNo;
    private String locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer sequenceOrder;
    private Integer scheduleNo;

    @Override
    public String toString() {
        return "ScheduleLocationVO [locationNo=" + locationNo + ", locationName=" + locationName 
                + ", latitude=" + latitude + ", longitude=" + longitude 
                + ", sequenceOrder=" + sequenceOrder + ", scheduleNo=" + scheduleNo + "]";
    }
}

