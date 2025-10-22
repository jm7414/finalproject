package lx.project.dementia_care.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScheduleRequest {
    private String title;
    private String content;
    private String date;            // YYYY-MM-DD 형식
    private String startTime;       // "오전 09:00" 형식
    private String endTime;         // "오전 11:00" 형식
    private List<LocationDto> locations;
    private List<CoordinateDto> routeCoordinates;
    private List<CoordinateDto> bufferCoordinates;
    private Integer bufferLevel;    // 안심존 단계 (1, 2, 3)

    @Override
    public String toString() {
        return "ScheduleRequest [title=" + title + ", date=" + date 
                + ", startTime=" + startTime + ", endTime=" + endTime + "]";
    }
}

