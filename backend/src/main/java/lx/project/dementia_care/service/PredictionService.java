package lx.project.dementia_care.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lx.project.dementia_care.dao.PredictionDAO;
import lx.project.dementia_care.dto.PredictionRequestDTO;
import lx.project.dementia_care.dto.PredictionResponseDTO;
import lx.project.dementia_care.vo.PredictVO;

@Service
public class PredictionService {

	@Autowired
	private PredictionDAO predDAO;

	public List<PredictionResponseDTO> getPredLocation(PredictionRequestDTO dto) throws Exception {
		PredictVO vo = new PredictVO();
		vo.setUserNo(dto.getUserNo());
		vo.setRecordTime(dto.getMissingTime()); // 바로 대입 가능

		return predDAO.getLocationData(vo);
	}
}
