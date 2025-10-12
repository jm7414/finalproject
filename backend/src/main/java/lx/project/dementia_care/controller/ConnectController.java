package lx.project.dementia_care.controller;

import lx.project.dementia_care.dao.ConnectDAO;
import lx.project.dementia_care.vo.ConnectVO;
import lx.project.dementia_care.vo.UserVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.util.Objects;

@RestController
public class ConnectController {

    @Autowired
    private ConnectDAO connectDAO;

    /** 연결 생성 (필수: patientNo, guardian1No or guardianNo[query]) */
    @PostMapping("/api/connect/connections")
    public Map<String, Object> create(@RequestBody(required = false) ConnectVO vo,
                                      @RequestParam(required = false, name = "guardianNo") Integer guardianNoParam) throws Exception {

        Map<String, Object> res = new HashMap<>();

        if (vo == null) vo = new ConnectVO();
        Integer patientNo = vo.getPatientNo();
        Integer guardian1No = vo.getGuardian1No();

        // 쿼리 파라미터 허용
        if ((guardian1No == null || guardian1No == 0) && guardianNoParam != null) {
            guardian1No = guardianNoParam;
        }

        if (patientNo == null || guardian1No == null) {
            res.put("ok", false);
            res.put("message", "patientNo, guardian1No는 필수");
            return res;
        }

        // 전역 중복: 이미 연결되어 있어도 같은 환자면 허용(멱등)
        boolean samePatient = false;
        if (connectDAO.isGuardianBusy(guardian1No)) {
            // 환자-보호자 조합이 이미 존재?
            if (connectDAO.isGuardianLinkedToPatient(patientNo, guardian1No)) {
                samePatient = true; // 멱등 허용
            } else {
                res.put("ok", false);
                res.put("message", "이미 다른 보호자와 연결된 환자입니다.");
                return res;
            }
        }

        // 환자 role에 따라 보호자 최대 인원 제한 (role=3이면 3명, 아니면 1명)
        // 멱등(samePatient)이면 개수 체크 생략
        if (!samePatient) {
            int currentCnt = 0;
            try { currentCnt = connectDAO.countGuardians(patientNo); } catch (Exception ignore) {}
            boolean patientIsRole3 = false;
            try { patientIsRole3 = connectDAO.isPatientRole3(patientNo); } catch (Exception ignore) {}

            int maxGuardians = patientIsRole3 ? 3 : 1;
            if (currentCnt >= maxGuardians) {
                res.put("ok", false);
                res.put("message", patientIsRole3
                        ? "보호자는 최대 3명까지 연결할 수 있습니다."
                        : "구독이 없는 경우 보호자는 1명까지만 연결할 수 있습니다.");
                return res;
            }
        }

        // 1) 최초 insert (존재하면 무시)
        try {
            ConnectVO insert = new ConnectVO();
            insert.setPatientNo(patientNo);
            insert.setGuardian1No(guardian1No);
            insert.setGuardian2No(null);
            insert.setGuardian3No(null);
            connectDAO.insertConnection(insert);
        } catch (Exception ignore) {
            // 이미 있으면 무시
        }

        // 2) 빈 슬롯 채우기 (안전 버전) — 이미 같은 환자면 0행이어도 성공 간주
        try {
            connectDAO.updateFillNextSlotSafe(patientNo, guardian1No);
        } catch (DataIntegrityViolationException e) {
            res.put("ok", false);
            res.put("message", "이미 다른 보호자와 연결된 환자입니다.");
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            res.put("ok", false);
            res.put("message", "연결 처리 중 오류가 발생했습니다.");
            return res;
        }

        // 3) 요약 반환
        Map<String, Object> patient = connectDAO.selectPatientBasic(patientNo);
        if (patient == null) {
            patient = new HashMap<>();
            patient.put("userNo", patientNo);
        }
        List<Map<String, Object>> guardians = connectDAO.selectGuardiansByPatientNo(patientNo);

        res.put("ok", true);
        res.put("message", "연결 완료");
        res.put("patient", patient);
        res.put("guardians", guardians);
        return res;
    }

    /** guardian1 교체 (멱등 허용 + 안전 버전 사용) */
    @PatchMapping("/api/connect/connections/{patientNo}/guardian1")
    public Map<String, Object> replaceGuardian1(@PathVariable int patientNo, @RequestParam int guardianNo)
            throws Exception {

        // 전역 중복이어도 같은 환자면 허용(멱등)
        if (connectDAO.isGuardianBusy(guardianNo)) {
            if (!connectDAO.isGuardianLinkedToPatient(patientNo, guardianNo)) {
                return Map.of("ok", false, "message", "이미 다른 보호자와 연결된 환자입니다.");
            }
            // 같은 환자면 계속 진행
        }

        try {
            int n = connectDAO.replaceGuardian1Safe(patientNo, guardianNo);
            return Map.of("ok", n > 0, "message", n > 0 ? "교체 완료" : "교체 실패");
        } catch (DataIntegrityViolationException e) {
            return Map.of("ok", false, "message", "이미 다른 보호자와 연결된 환자입니다.");
        }
    }

    /** 연결 삭제(환자 기준, 전체 행 삭제) – 유지 */
    @DeleteMapping("/api/connect/connections/{patientNo}")
    public Map<String, Object> delete(@PathVariable int patientNo) throws Exception {
        Map<String, Object> res = new HashMap<>();
        int n = connectDAO.deleteByPatientNo(patientNo);
        res.put("ok", n > 0);
        res.put("message", n > 0 ? "삭제 완료" : "삭제할 연결 없음");
        return res;
    }

    /** 초대코드 해석 */
    @GetMapping("/api/connect/resolve-invite")
    public ResponseEntity<?> resolveInvite(@RequestParam("code") String code) {
        try {
            UserVO patient = connectDAO.findPatientByInvitationCode(code);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "유효하지 않은 초대코드입니다."));
            }
            Map<String, Object> out = new HashMap<>();
            out.put("patientNo", patient.getUserNo());
            out.put("patientName", patient.getName());
            out.put("name", patient.getName());
            out.put("invitationCode", patient.getInvitationCode());
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "초대코드 확인 중 오류가 발생했습니다."));
        }
    }

    /** 연결 취소(현재 보호자만 제거) — 유일 보호자일 땐 행 삭제 */
    @DeleteMapping("/api/connect/connections")
    public Map<String, Object> cancel(@RequestParam int patientNo, @RequestParam int guardianNo) throws Exception {
        Map<String, Object> out = new HashMap<>();

        List<Map<String, Object>> gs = connectDAO.selectGuardiansByPatientNo(patientNo);
        if (gs == null) gs = Collections.emptyList();

        if (gs.size() == 1) {
            Object onlyId = gs.get(0).get("guardianId");
            int onlyGuardianId = (onlyId instanceof Number) ? ((Number) onlyId).intValue() : -1;
            if (onlyGuardianId == guardianNo) {
                connectDAO.deleteByPatientNo(patientNo);
            }
        } else {
            connectDAO.unsetGuardianSlot(patientNo, guardianNo);
            connectDAO.compactSlots(patientNo);
            connectDAO.deleteIfEmpty(patientNo);
        }

        Map<String, Object> patient = connectDAO.selectPatientBasic(patientNo);
        if (patient == null) {
            patient = new HashMap<>();
            patient.put("userNo", patientNo);
        }
        List<Map<String, Object>> guardians = connectDAO.selectGuardiansByPatientNo(patientNo);

        out.put("ok", true);
        out.put("message", "연결이 취소되었습니다.");
        out.put("patient", patient);
        out.put("guardians", guardians);
        return out;
    }

    /** (추가) 환자 요약 조회 */
    @GetMapping("/api/connect/patient/{patientNo}/basic")
    public Map<String, Object> getPatientBasic(@PathVariable int patientNo) throws Exception {
        Map<String, Object> patient = connectDAO.selectPatientBasic(patientNo);
        if (patient == null) {
            patient = new HashMap<>();
            patient.put("userNo", patientNo);
        }
        return patient;
    }

    /** (추가) 환자-보호자 목록 조회 */
    @GetMapping("/api/connect/patient/{patientNo}/guardians")
    public List<Map<String, Object>> getPatientGuardians(@PathVariable int patientNo) throws Exception {
        return connectDAO.selectGuardiansByPatientNo(patientNo);
    }
}
