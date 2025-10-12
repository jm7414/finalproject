package lx.project.dementia_care.controller;

import lx.project.dementia_care.dao.ConnectDAO;
import lx.project.dementia_care.vo.ConnectVO;
import lx.project.dementia_care.vo.UserVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * connect 영역 전용 컨트롤러 - 서비스 없이 DAO/매퍼 직접 호출 - 연결/해제/조회 시 요약(patient, guardians)
 * 응답
 */
@RestController
public class ConnectController {

	@Autowired
	private ConnectDAO connectDAO; // DB 접근을 위한 DAO 주입

	/** 연결 생성 (필수: patientNo, guardian1No or guardianNo[query]) */
	@PostMapping("/api/connect/connections")
	public Map<String, Object> create(@RequestBody(required = false) ConnectVO vo,
			@RequestParam(required = false, name = "guardianNo") Integer guardianNoParam) throws Exception {

		Map<String, Object> res = new HashMap<>(); // 표준 응답 맵

		if (vo == null)
			vo = new ConnectVO(); // body가 없을 때도 NPE 없이 진행하기 위한 방어 코드
		Integer patientNo = vo.getPatientNo(); // 필수: 연결 대상 환자 번호
		Integer guardian1No = vo.getGuardian1No(); // 필수: 연결할 보호자 번호(기본 슬롯)

		// 쿼리 파라미터로 guardianNo가 들어오면 수용
		if ((guardian1No == null || guardian1No == 0) && guardianNoParam != null) {
			guardian1No = guardianNoParam; // body가 비었거나 0일 때 query의 guardianNo를 대체값으로 사용
		}

		if (patientNo == null || guardian1No == null) {
			res.put("ok", false);
			res.put("message", "patientNo, guardian1No는 필수"); // 필수값 누락 응답
			return res;
		}

		// 1) 최초 insert (ON CONFLICT DO NOTHING)
		try {
			ConnectVO insert = new ConnectVO(); // 기본 연결 행 생성 시도
			insert.setPatientNo(patientNo);
			insert.setGuardian1No(guardian1No);
			insert.setGuardian2No(null); // 새로 만들 때 나머지 슬롯은 비워둠
			insert.setGuardian3No(null);
			connectDAO.insertConnection(insert); // 중복 시 예외 발생 가능(매퍼 설정에 따라 무시)
		} catch (Exception ignore) {
			// 동일 환자 행이 이미 있을 수 있으므로 실패해도 흐름 유지
		}

		// 2) 빈 슬롯 채우기 (이미 있으면 변화 없음)
		Map<String, Object> p = new HashMap<>();
		p.put("patientNo", patientNo);
		p.put("guardianNo", guardian1No);
		try {
			connectDAO.updateFillNextSlot(p); // guardian2/guardian3 중 비어있는 첫 슬롯을 채우는 로직
		} catch (Exception e) {
			e.printStackTrace(); // 운영 시 로깅 전환 권장
		}

		// 3) 요약 조회
		Map<String, Object> patient = connectDAO.selectPatientBasic(patientNo); // 환자 요약 정보
		if (patient == null) {
			patient = new HashMap<>();
			patient.put("userNo", patientNo); // 조회 실패 시 최소 식별자만 보전
		}
		List<Map<String, Object>> guardians = connectDAO.selectGuardiansByPatientNo(patientNo); // 보호자 목록

		res.put("ok", true); // 성공 플래그
		res.put("message", "연결 완료"); // 메시지
		res.put("patient", patient); // 환자 요약
		res.put("guardians", guardians); // 보호자 목록
		return res; // 프론트가 즉시 상태 갱신 가능
	}

	/** guardian1 교체 (유지) */
	@PatchMapping("/api/connect/connections/{patientNo}/guardian1")
	public Map<String, Object> replaceGuardian1(@PathVariable int patientNo, @RequestParam int guardianNo)
			throws Exception {
		Map<String, Object> res = new HashMap<>(); // 응답 맵
		int n = connectDAO.replaceGuardian1(patientNo, guardianNo); // 주 보호자 슬롯을 특정 보호자로 교체
		res.put("ok", n > 0); // 반영된 행 수 기준 성공 여부
		res.put("message", n > 0 ? "교체 완료" : "교체 실패"); // 결과 메시지
		return res;
	}

	/** 연결 삭제(환자 기준, 전체 행 삭제) – 유지 */
	@DeleteMapping("/api/connect/connections/{patientNo}")
	public Map<String, Object> delete(@PathVariable int patientNo) throws Exception {
		Map<String, Object> res = new HashMap<>(); // 응답 맵
		int n = connectDAO.deleteByPatientNo(patientNo); // 환자 기준 연결 행 자체 삭제
		res.put("ok", n > 0); // 삭제 성공 여부
		res.put("message", n > 0 ? "삭제 완료" : "삭제할 연결 없음");
		return res;
	}

	/** 초대코드 해석 */
	@GetMapping("/api/connect/resolve-invite")
	public ResponseEntity<?> resolveInvite(@RequestParam("code") String code) {
		try {
			UserVO patient = connectDAO.findPatientByInvitationCode(code); // 초대코드로 환자 조회
			if (patient == null) {
				// 유효하지 않은 코드거나 만료된 경우
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "유효하지 않은 초대코드입니다."));
			}
			Map<String, Object> out = new HashMap<>(); // 성공 시 프론트 호환 키 포함 응답
			out.put("patientNo", patient.getUserNo()); // 환자 식별자
			out.put("patientName", patient.getName()); // 호환 키(기존 프론트 사용 대비)
			out.put("name", patient.getName()); // 프론트 신규 키(단일화 목적)
			out.put("invitationCode", patient.getInvitationCode()); // 코드 에코백(로그/검증용)
			return ResponseEntity.ok(out); // 200 OK 응답
		} catch (Exception e) {
			e.printStackTrace(); // 예외 상황 로깅
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "초대코드 확인 중 오류가 발생했습니다.")); // 500 응답
		}
	}

	/** 연결 취소(현재 보호자만 제거) — 유일 보호자일 땐 행 삭제 */
	@DeleteMapping("/api/connect/connections")
	public Map<String, Object> cancel(@RequestParam int patientNo, @RequestParam int guardianNo) throws Exception {
		Map<String, Object> out = new HashMap<>(); // 응답 맵

		// 현재 연결된 보호자 목록
		List<Map<String, Object>> gs = connectDAO.selectGuardiansByPatientNo(patientNo);
		if (gs == null)
			gs = Collections.emptyList(); // NPE 방지를 위한 빈 리스트

		if (gs.size() == 1) {
			// 보호자가 한 명뿐인 경우
			Object onlyId = gs.get(0).get("guardianId"); // 유일 보호자의 id
			int onlyGuardianId = (onlyId instanceof Number) ? ((Number) onlyId).intValue() : -1;
			if (onlyGuardianId == guardianNo) {
				// 유일 보호자가 본인일 때: 행 자체 삭제(컬럼 NOT NULL 제약 회피)
				connectDAO.deleteByPatientNo(patientNo);
			} else {
				// 유일 보호자가 본인이 아니라면 무시(권한 체크를 통해 403 고려 여지)
			}
		} else {
			// 보호자가 둘 이상인 경우: 대상 보호자만 안전하게 제거
			connectDAO.unsetGuardianSlot(patientNo, guardianNo); // 해당 guardianNo가 들어있는 슬롯을 null 처리
			connectDAO.compactSlots(patientNo); // null로 비는 칸을 좌측으로 압축 정렬
			connectDAO.deleteIfEmpty(patientNo); // 모든 슬롯이 null이면 행 삭제
		}

		// 요약 재조회로 최신 상태 전달
		Map<String, Object> patient = connectDAO.selectPatientBasic(patientNo);
		if (patient == null) {
			patient = new HashMap<>();
			patient.put("userNo", patientNo); // 최소 식별자 보전
		}
		List<Map<String, Object>> guardians = connectDAO.selectGuardiansByPatientNo(patientNo); // 해제 후 보호자 목록

		out.put("ok", true); // 성공 플래그(아이들포턴시 고려 시 상태 코드 분리 가능)
		out.put("message", "연결이 취소되었습니다."); // 사용자 메시지
		out.put("patient", patient); // 환자 요약
		out.put("guardians", guardians); // 최신 보호자 목록
		return out;
	}

	/** (추가) 환자 요약 조회 */
	@GetMapping("/api/connect/patient/{patientNo}/basic")
	public Map<String, Object> getPatientBasic(@PathVariable int patientNo) throws Exception {
		Map<String, Object> patient = connectDAO.selectPatientBasic(patientNo); // 환자 기본 정보 조회
		if (patient == null) {
			patient = new HashMap<>();
			patient.put("userNo", patientNo); // 조회 실패 시 최소 식별자만 반환
		}
		return patient; // 클라이언트에서 단독 조회 시 사용
	}

	/** (추가) 환자-보호자 목록 조회 */
	@GetMapping("/api/connect/patient/{patientNo}/guardians")
	public List<Map<String, Object>> getPatientGuardians(@PathVariable int patientNo) throws Exception {
		return connectDAO.selectGuardiansByPatientNo(patientNo); // guardianId, name, relation 등 목록
	}
}
