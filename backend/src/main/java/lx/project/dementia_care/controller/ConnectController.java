package lx.project.dementia_care.controller;

import lx.project.dementia_care.dao.ConnectDAO;
import lx.project.dementia_care.dao.UserDAO;
import lx.project.dementia_care.vo.ConnectVO;
import lx.project.dementia_care.vo.UserVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@RestController
public class ConnectController {

   @Autowired
   private ConnectDAO connectDAO;

   @Autowired
   private UserDAO userDAO;

   /** 연결 생성 (필수: patientNo, guardian1No or guardianNo[query]) */
   @PostMapping("/api/connect/connections")
   public Map<String, Object> create(@RequestBody(required = false) ConnectVO vo,
         @RequestParam(required = false, name = "guardianNo") Integer guardianNoParam) throws Exception {

      Map<String, Object> res = new HashMap<>();

      if (vo == null)
         vo = new ConnectVO();
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
         if (connectDAO.isGuardianLinkedToPatient(patientNo, guardian1No)) {
            samePatient = true; // 멱등 허용
         } else {
            res.put("ok", false);
            res.put("message", "이미 다른 보호자와 연결된 환자입니다.");
            return res;
         }
      }

      // 환자 role에 따라 보호자 최대 인원 제한 (실시간 구독 유효 여부 기반으로 변경)
      if (!samePatient) {

         // 만료된 구독이면 2/3번 슬롯 정리
         try {
            connectDAO.expireExtraSlots();
         } catch (Exception ignore) {
         }

         // 현재 연결 수
         int currentCnt = 0;
         try {
            currentCnt = connectDAO.countGuardians(patientNo);
         } catch (Exception ignore) {
         }

         // 실시간 구독 유효 여부 계산
         boolean subscriptionActive = false;

         // 환자에 이미 연결된 보호자가 있다면, 그 중 1명의 구독 만료시각으로 판단
         if (currentCnt > 0) {
            try {
               List<Map<String, Object>> gs = connectDAO.selectGuardiansByPatientNo(patientNo);
               if (gs != null && !gs.isEmpty()) {
                  Object gidObj = gs.get(0).get("guardianId");
                  int gid = (gidObj instanceof Number) ? ((Number) gidObj).intValue() : -1;
                  if (gid > 0) {
                     Map<String, Object> sub = connectDAO.selectSubscriptionByGuardian(gid);
                     if (sub != null && sub.get("end") != null) {
                        Object endObj = sub.get("end"); // 보통 java.sql.Timestamp
                        java.time.Instant endTs = (endObj instanceof java.sql.Timestamp)
                              ? ((java.sql.Timestamp) endObj).toInstant()
                              : null;
                        if (endTs != null && endTs.isAfter(java.time.Instant.now())) {
                           subscriptionActive = true;
                        }
                     }
                  }
               }
            } catch (Exception ignore) {
            }
         }

         // 호환용: 위에서 판단 못했으면 role=3 기준으로 보조 판단
         if (!subscriptionActive) {
            try {
               subscriptionActive = connectDAO.isPatientRole3(patientNo);
            } catch (Exception ignore) {
            }
         }

         // [변경] 역할이 아닌 "실시간 구독 유효" 기준으로 상한 결정
         int maxGuardians = subscriptionActive ? 3 : 1;

         if (currentCnt >= maxGuardians) {
            res.put("ok", false);
            res.put("message", subscriptionActive ? "보호자는 최대 3명까지 연결할 수 있습니다." : "구독이 없는 경우 보호자는 1명까지만 연결할 수 있습니다.");
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

      // --- 환자 구독이 이미 활성화된 상태라면, 이번에 붙은 보호자도 구독 ON ---
      try {
         boolean subscriptionActive = false;

         // (a) 구독 기간 컬럼 기반: end > now?
         Map<String, Object> sub = connectDAO.selectSubscriptionByGuardian(guardian1No);
         if (sub != null && sub.get("end") != null) {
            Object endObj = sub.get("end"); // 보통 java.sql.Timestamp
            java.time.Instant endTs = (endObj instanceof java.sql.Timestamp)
                  ? ((java.sql.Timestamp) endObj).toInstant()
                  : null;
            if (endTs != null && endTs.isAfter(java.time.Instant.now())) {
               subscriptionActive = true;
            }
         }

         // (b) 과거 호환: 환자 role_no=3이면 구독 중으로 간주
         if (!subscriptionActive) {
            try {
               subscriptionActive = connectDAO.isPatientRole3(patientNo);
            } catch (Exception ignore) {
            }
         }

         if (subscriptionActive) {
            UserVO g = new UserVO();
            g.setUserNo(guardian1No);
            g.setSubscriptionStatus(1);
            userDAO.updateById(g);
         }
      } catch (Exception ignore) {
         // 구독 반영 실패해도 연결 흐름은 유지
      }
      // --- [END] ---

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

      if (connectDAO.isGuardianBusy(guardianNo)) {
         if (!connectDAO.isGuardianLinkedToPatient(patientNo, guardianNo)) {
            return Map.of("ok", false, "message", "이미 다른 보호자와 연결된 환자입니다.");
         }
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
      if (gs == null)
         gs = Collections.emptyList();

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

   // === [SUMMARY API START] ===
   @GetMapping("/api/subscriptions/summary")
   public Map<String, Object> getSubscriptionSummary(
         @RequestHeader(value = "X-Mock-User", required = false) String mockUser,
         @RequestParam(required = false, name = "guardianNo") Integer guardianNoParam) {
      try {
         // guardianNo 결정: 헤더 > 쿼리 파라미터
         Integer guardianNo = null;
         if (mockUser != null && !mockUser.isBlank()) {
            try {
               guardianNo = Integer.parseInt(mockUser.trim());
            } catch (Exception ignore) {
            }
         }
         if (guardianNo == null && guardianNoParam != null)
            guardianNo = guardianNoParam;
         if (guardianNo == null)
            return Map.of("ok", false, "message", "guardianNo 필요");

         // 구독 기간(end > now?) → 실시간 활성
         boolean subscriptionActive = false;
         Map<String, Object> sub = connectDAO.selectSubscriptionByGuardian(guardianNo);
         if (sub != null && sub.get("end") != null) {
            Object endObj = sub.get("end"); // java.sql.Timestamp
            java.time.Instant endTs = (endObj instanceof java.sql.Timestamp)
                  ? ((java.sql.Timestamp) endObj).toInstant()
                  : null;
            if (endTs != null && endTs.isAfter(java.time.Instant.now()))
               subscriptionActive = true;
         }

         // 연결된 환자
         Integer patientNo = connectDAO.findPatientNoByGuardian(guardianNo);

         // 화면 분기용 간단 플래그(실무 조건: guardian/patient의 users.subscription_status=1 & patient
         // role=3 이지만
         // 결제 시 컨트롤러가 그렇게 세팅하므로 여기선 gpc 활성 + 환자 존재로 간주)
         boolean plus = subscriptionActive && patientNo != null;

         Map<String, Object> out = new HashMap<>();
         out.put("ok", true);
         out.put("guardianNo", guardianNo);
         out.put("patientNo", patientNo);
         out.put("subscriptionActive", subscriptionActive);
         out.put("plus", plus);
         out.put("subscriptionEndTime", sub != null ? sub.get("end") : null);
         return out;
      } catch (Exception e) {
         e.printStackTrace();
         return Map.of("ok", false, "message", "조회 오류");
      }
   }
   // === [SUMMARY API END] ===

   @PostMapping("/api/payments/confirm")
   public ResponseEntity<?> confirmPayment(@RequestHeader(value = "X-Mock-User", required = false) String mockUser,
         @RequestBody ConfirmReq req) {
      try {
         // 1) guardianNo 결정: 헤더 > 바디 > 에러
         Integer guardianNo = null;
         if (mockUser != null && !mockUser.isBlank()) {
            try {
               guardianNo = Integer.parseInt(mockUser.trim());
            } catch (Exception ignore) {
            }
         }
         if (guardianNo == null && req.getGuardianNo() != null) {
            guardianNo = req.getGuardianNo();
         }
         if (guardianNo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                  .body(Map.of("message", "보호자 정보가 없습니다. (헤더 X-Mock-User 또는 body.guardianNo 필요)"));
         }

         // 2) 플랜 → 개월 수
         final int months = "12month".equalsIgnoreCase(req.getSelectedPlan()) ? 12 : 1;

         // 3) 자바에서 시간 계산(UTC)
         final OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC);
         final OffsetDateTime end = start.plusMonths(months);

         // 4) DB 저장 (mapper-connect.setSubscription 호출)
         int updated = connectDAO.setSubscription(guardianNo, start, end);
         if (updated == 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "연결 레코드를 찾을 수 없습니다."));
         }

         /* === [ADD] users 테이블도 갱신 === */

         // 보호자 구독 ON
         UserVO g = new UserVO();
         g.setUserNo(guardianNo);
         g.setSubscriptionStatus(1);
         userDAO.updateById(g);

         // 연결된 환자 찾기 → 환자 role=3 + 구독 ON
         Integer patientNo = connectDAO.findPatientNoByGuardian(guardianNo);
         if (patientNo != null) {
            UserVO p = new UserVO();
            p.setUserNo(patientNo);
            p.setRoleNo(3); // 환자 → 구독자
            p.setSubscriptionStatus(1); // 구독 ON
            userDAO.updateById(p);
         }

         // 5) 저장값 조회해서 응답
         Map<String, Object> sub = connectDAO.selectSubscriptionByGuardian(guardianNo);
         return ResponseEntity.ok(Map.of("status", "PAID", "plan", req.getSelectedPlan(), "subscription", sub, // {
                                                                                                               // start,
                                                                                                               // end }
               "message", "OK"));

      } catch (Exception e) {
         e.printStackTrace();
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "구독 처리 중 오류가 발생했습니다."));
      }
   }

   /**
    * 구독 취소 API
    * POST /api/subscriptions/cancel
    */
   @PostMapping("/api/subscriptions/cancel")
   public ResponseEntity<?> cancelSubscription(
         @RequestHeader(value = "X-Mock-User", required = false) String mockUser,
         @RequestBody Map<String, Object> body) {
      try {
         // 1) guardianNo 결정: 헤더 > 바디
         Integer guardianNo = null;
         if (mockUser != null && !mockUser.isBlank()) {
            try {
               guardianNo = Integer.parseInt(mockUser.trim());
            } catch (Exception ignore) {
            }
         }
         if (guardianNo == null && body.get("guardianNo") != null) {
            guardianNo = ((Number) body.get("guardianNo")).intValue();
         }
         if (guardianNo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                  .body(Map.of("message", "보호자 정보가 없습니다."));
         }

         // 2) 구독 만료 처리 (subscribe_endtime을 현재 시간으로 설정)
         OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
         int updated = connectDAO.cancelSubscription(guardianNo, now);

         if (updated == 0) {
            return ResponseEntity.badRequest()
                  .body(Map.of("message", "취소할 구독을 찾을 수 없습니다."));
         }

         // 3) users 테이블 업데이트: 보호자 구독 상태 OFF
         UserVO guardian = new UserVO();
         guardian.setUserNo(guardianNo);
         guardian.setSubscriptionStatus(0);
         userDAO.updateById(guardian);

         // 4) 연결된 환자 찾기 → 환자 role 2로 변경, 구독 OFF
         Integer patientNo = connectDAO.findPatientNoByGuardian(guardianNo);
         if (patientNo != null) {
            UserVO patient = new UserVO();
            patient.setUserNo(patientNo);
            patient.setRoleNo(2); // 구독자(3) → 환자(2)
            patient.setSubscriptionStatus(0);
            userDAO.updateById(patient);
         }

         // 5) 보호자 2, 3번 슬롯 정리 (PLUS 플랜 해지로 1명만 가능)
         try {
            connectDAO.expireExtraSlots();
         } catch (Exception ignore) {
         }

         return ResponseEntity.ok(Map.of(
               "status", "CANCELLED",
               "message", "구독이 취소되었습니다."));

      } catch (Exception e) {
         e.printStackTrace();
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
               .body(Map.of("message", "구독 취소 처리 중 오류가 발생했습니다."));
      }
   }

   // 결제/구독 요청 바디
   public static class ConfirmReq {
      private String selectedPlan; // "1month" | "12month"
      private Integer guardianNo; // (옵션) 헤더가 없을 때 바디로 받기 위함
      private Boolean agreeTerm; // 프론트에서 오긴 하지만 서버에서는 사용 안 함(데모)
      private Boolean agreePrivacy;
      private String cardNumber;
      private String expiry;
      private String cvc;
      private String owner;

      public String getSelectedPlan() {
         return selectedPlan;
      }

      public void setSelectedPlan(String selectedPlan) {
         this.selectedPlan = selectedPlan;
      }

      public Integer getGuardianNo() {
         return guardianNo;
      }

      public void setGuardianNo(Integer guardianNo) {
         this.guardianNo = guardianNo;
      }

      public Boolean getAgreeTerm() {
         return agreeTerm;
      }

      public void setAgreeTerm(Boolean agreeTerm) {
         this.agreeTerm = agreeTerm;
      }

      public Boolean getAgreePrivacy() {
         return agreePrivacy;
      }

      public void setAgreePrivacy(Boolean agreePrivacy) {
         this.agreePrivacy = agreePrivacy;
      }

      public String getCardNumber() {
         return cardNumber;
      }

      public void setCardNumber(String cardNumber) {
         this.cardNumber = cardNumber;
      }

      public String getExpiry() {
         return expiry;
      }

      public void setExpiry(String expiry) {
         this.expiry = expiry;
      }

      public String getCvc() {
         return cvc;
      }

      public void setCvc(String cvc) {
         this.cvc = cvc;
      }

      public String getOwner() {
         return owner;
      }

      public void setOwner(String owner) {
         this.owner = owner;
      }
   }
}
