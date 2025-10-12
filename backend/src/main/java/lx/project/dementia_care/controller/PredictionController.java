package lx.project.dementia_care.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lx.project.dementia_care.dto.PredictionRequestDTO;
import lx.project.dementia_care.dto.PredictionResponseDTO;
import lx.project.dementia_care.service.PredictionService;

@RestController
@RequestMapping("/pred")
public class PredictionController {

	@Autowired
	private PredictionService predService;

	// 실제 URL에서 localhost:5173/pred/3/2025-10-12-17-27로 표시되게
	@GetMapping("/{userNo}/{datetime}")
	public List<PredictionResponseDTO> getPatientData(@PathVariable int userNo,
			@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd-HH-mm") LocalDateTime datetime) throws Exception {

		PredictionRequestDTO dto = new PredictionRequestDTO();
		dto.setUserNo(userNo);
		dto.setMissingTime(datetime);

		return predService.getPredLocation(dto);
	}
	
	// 예측한 정보를 DB에 저장 -> 추후 추가예정 (먼저 DB에 접근해서 python모델로 불러온 결과를 받아낸 다음에 진행예정)
	@PostMapping
	public int insertPredictedLocation() {
		return predService.insertPredictedLocation();
	}

}
