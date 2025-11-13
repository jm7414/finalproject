package lx.project.dementia_care.vo;

import java.math.BigDecimal;
import java.sql.Timestamp;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PredictVO {
    private int userNo;
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    private Timestamp recordTime;
}
