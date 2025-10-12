package lx.project.dementia_care.dao;

import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lx.project.dementia_care.dto.PredictionResponseDTO;
import lx.project.dementia_care.vo.LocationDataVO;

@Component
public class PredictionDAO {

	@Autowired
	SqlSession session;
	
	public List<PredictionResponseDTO> getLocationData(LocationDataVO vo) {
		return session.selectList("lx.project.dementia_care.dao.PredictionDAO.getLocationData", vo);
	}
}
