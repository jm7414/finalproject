package lx.project.dementia_care.controller;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lx.project.dementia_care.dto.PredictionRequestDTO;
import lx.project.dementia_care.dto.PredictionResponseDTO;
import lx.project.dementia_care.service.PredictionService;

@RestController
@RequestMapping("/api/pred")
public class PredictionController {

	@Autowired
	private PredictionService predService;

	// 실제 URL에서 localhost:5173/pred/3/2025-10-12%2017:27:ss로 표시되게
	@GetMapping("/{userNo}")
	public List<PredictionResponseDTO> getPatientData(@PathVariable int userNo, @RequestParam Long datetime) throws Exception{

		Timestamp timestamp = new Timestamp(datetime);
		PredictionRequestDTO dto = new PredictionRequestDTO();
		dto.setUserNo(userNo);
		dto.setMissingTime(timestamp);

		return predService.getPredLocation(dto);
	}
}