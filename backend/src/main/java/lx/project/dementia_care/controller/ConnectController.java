package lx.project.dementia_care.controller;

import lx.project.dementia_care.dao.ConnectDAO;
import lx.project.dementia_care.vo.ConnectVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ConnectController {

	@Autowired
	private ConnectDAO connectDAO;

	// 1) 연결 생성 (필수: patientNo, guardian1No)
	@PostMapping("/api/connect/connections")
	public Map<String, Object> create(@RequestBody ConnectVO vo) throws Exception {
		Map<String, Object> res = new HashMap<>();
		if (vo.getPatientNo() == null || vo.getGuardian1No() == null) {
			res.put("ok", false);
			res.put("message", "patientNo, guardian1No는 필수");
			return res;
		}
		int n = connectDAO.insertConnection(vo);
		res.put("ok", n > 0);
		res.put("message", n > 0 ? "생성 완료" : "생성 실패");
		res.put("patientNo", vo.getPatientNo());
		return res;
	}

	// 2) guardian1 교체 (쿼리파라미터: guardianNo) @path는 url경로에있는 값 들고오는거
	@PatchMapping("/api/connect/connections/{patientNo}/guardian1")
	public Map<String, Object> replaceGuardian1(@PathVariable int patientNo, @RequestParam int guardianNo)
			throws Exception {
		Map<String, Object> res = new HashMap<>();
		int n = connectDAO.replaceGuardian1(patientNo, guardianNo);
		res.put("ok", n > 0);
		res.put("message", n > 0 ? "교체 완료" : "교체 실패");
		return res;
	}

	// 3) 연결 삭제 환자기준
	@DeleteMapping("/api/connect/connections/{patientNo}")
	public Map<String, Object> delete(@PathVariable int patientNo) throws Exception {
		Map<String, Object> res = new HashMap<>();
		int n = connectDAO.deleteByPatientNo(patientNo);
		res.put("ok", n > 0);
		res.put("message", n > 0 ? "삭제 완료" : "삭제할 연결 없음");
		return res;
	}
}