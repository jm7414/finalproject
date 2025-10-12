package lx.project.dementia_care.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationDataVO {

	private int userNo;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private LocalDateTime missingTime;
}
