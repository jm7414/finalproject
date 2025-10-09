package lx.project.dementia_care.controller;

import lx.project.dementia_care.dao.ConnectDAO;
import lx.project.dementia_care.vo.ConnectVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ConnectController {

    @Autowired
    private ConnectDAO connectDAO;

    /** 1) 연결 생성 */
    @PostMapping("/api/connect/connections")
    public ResponseEntity<?> create(@RequestBody ConnectVO vo) {
        try {
            if (vo.getPatientNo() == null || vo.getGuardian1No() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "patientNo, guardian1No는 필수입니다."));
            }
            int n = connectDAO.insertConnection(vo);
            if (n > 0) {
                return ResponseEntity.ok(Map.of(
                        "message", "연결이 생성되었습니다.",
                        "patientNo", vo.getPatientNo()
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "생성 실패"));
        } catch (DuplicateKeyException e) {
            // 예: UNIQUE(patient_no) 위반
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "이미 존재하는 환자 연결입니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 오류"));
        }
    }

    /** 2) guardian1 교체 (필수: guardianNo != null) */
    @PatchMapping("/api/connect/connections/{patientNo}/guardian1")
    public ResponseEntity<?> replaceGuardian1(@PathVariable int patientNo,
                                              @RequestParam Integer guardianNo) {
        try {
            if (guardianNo == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "guardianNo는 필수입니다."));
            }
            int n = connectDAO.replaceGuardian1(patientNo, guardianNo);
            if (n == 0) {
                // 대상 행이 없거나 기타 사유로 미반영
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "교체 실패(대상 없음 또는 조건 불충족)"));
            }
            return ResponseEntity.ok(Map.of("message", "guardian1 교체 완료"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 오류"));
        }
    }

    /** 3) 연결 삭제 (환자 기준) */
    @DeleteMapping("/api/connect/connections/{patientNo}")
    public ResponseEntity<?> delete(@PathVariable int patientNo) {
        try {
            int n = connectDAO.deleteByPatientNo(patientNo);
            if (n == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "삭제할 연결이 없습니다."));
            }
            return ResponseEntity.ok(Map.of("message", "삭제 완료"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 오류"));
        }
    }
}
