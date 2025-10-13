package lx.project.dementia_care.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lx.project.dementia_care.dao.PredictionDAO;
import lx.project.dementia_care.dto.PredictionRequestDTO;
import lx.project.dementia_care.dto.PredictionResponseDTO;
import lx.project.dementia_care.vo.LocationDataVO;

@Service
public class PredictionService {
	
	@Autowired
	private PredictionDAO predDAO;

	public List<PredictionResponseDTO> getPredLocation(PredictionRequestDTO dto) throws Exception{
		LocationDataVO vo = new LocationDataVO();
		vo.setUserNo(dto.getUserNo());
		vo.setMissingTime(dto.getMissingTime());
		return predDAO.getLocationData(vo);
	}
	
	public int insertPredictedLocation() {
		return 1;
	}

}
