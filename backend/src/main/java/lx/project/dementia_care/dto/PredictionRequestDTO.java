package lx.project.dementia_care.dto;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PredictionRequestDTO {

	private int userNo;
	private LocalDateTime missingTime;
}
