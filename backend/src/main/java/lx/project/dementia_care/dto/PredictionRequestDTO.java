package lx.project.dementia_care.dto;

import java.sql.Timestamp;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class PredictionRequestDTO {

	private int userNo;
	private Timestamp missingTime;
}
