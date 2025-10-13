package lx.project.dementia_care.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class PredictionResponseDTO {

	private int userNo;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private LocalDateTime time;
}
