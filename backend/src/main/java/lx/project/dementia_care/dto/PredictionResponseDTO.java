package lx.project.dementia_care.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class PredictionResponseDTO {

	private int userNo;
	private BigDecimal latitude;
	private BigDecimal longitude;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
	private OffsetDateTime recordTime;
}
