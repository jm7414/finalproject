package lx.project.dementia_care.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lx.project.dementia_care.dao.ConnectDAO;
import lx.project.dementia_care.dao.UserDAO;
import lx.project.dementia_care.vo.UserVO;

@RestController
public class UserController {
	


    @Autowired
    private UserDAO userDAO;
    
    @Autowired
    private ConnectDAO connectDAO;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/api/user/check-duplicate")
    public ResponseEntity<?> checkDuplicateId(@RequestParam String userId) {
        try {
            UserVO existingUser = userDAO.findById(userId);
            return ResponseEntity.ok(Map.of("isDuplicate", existingUser != null));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "중복 확인 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserVO user) {
        try {
            // 중복 체크
            if (userDAO.findById(user.getUserId()) != null) {
                return ResponseEntity.badRequest().body(Map.of("message", "이미 존재하는 아이디입니다."));
            }
            
            // 비밀번호 암호화
            user.setUserPw(passwordEncoder.encode(user.getPassword()));
            
            // 역할 설정 (보호자 체크박스에 따라)
            user.setRoleNo(user.getRoleNo() == 1 ? 1 : 2); // 1: 보호자, 2: 환자
            
            // 초대 코드 생성 (환자일 경우에만)
            if (user.getRoleNo() == 2) {
                String invitationCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                user.setInvitationCode(invitationCode);
            } else {
                user.setInvitationCode(null); // 보호자는 초대코드 없음
            }
            
            // 회원가입 처리
            userDAO.insertUser(user);
            
            return ResponseEntity.ok(Map.of("message", "회원가입이 완료되었습니다."));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "회원가입 처리 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/api/user/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }
            
            UserVO currentUser = (UserVO) auth.getPrincipal();
            return ResponseEntity.ok(currentUser);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "사용자 정보 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 보호자가 관리하는 환자 정보 조회
     * GET /api/user/my-patient
     */
    @GetMapping("/api/user/my-patient")
    public ResponseEntity<?> getMyPatient() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }
            
            UserVO currentGuardian = (UserVO) auth.getPrincipal();
            
            // 보호자만 접근 가능
            if (currentGuardian.getRoleNo() != 1 && currentGuardian.getRoleNo() != 3) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "보호자만 접근할 수 있습니다."));
            }
            
            // 보호자가 관리하는 환자 조회
            UserVO patient = connectDAO.getPatientByGuardianNo(currentGuardian.getUserNo());
            
            if (patient == null) {
                return ResponseEntity.ok(Map.of("message", "관리하는 환자가 없습니다."));
            }
            
            return ResponseEntity.ok(patient);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "환자 정보 조회 중 오류가 발생했습니다."));
        }
    }

	@PostMapping(value = "/api/payments/confirm", consumes = "application/json", produces = "application/json")
	public ResponseEntity<?> confirmPayment(@RequestBody Map<String, Object> req) {
		try {
			boolean agreed = "true".equalsIgnoreCase(String.valueOf(req.get("agreeTerm")))
					&& "true".equalsIgnoreCase(String.valueOf(req.get("agreePrivacy")));
			if (!agreed) {
				Map<String, Object> body = new HashMap<>();
				body.put("status", "FAILED");
				body.put("message", "약관/개인정보 동의가 필요합니다.");
				return ResponseEntity.badRequest().body(body);
			}
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated()) {
				Map<String, Object> body = new HashMap<>();
				body.put("status", "FAILED");
				body.put("message", "로그인이 필요합니다.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
			}
			String username = auth.getName(); // 현재 로그인 user_id
			lx.project.dementia_care.vo.UserVO me = userDAO.findById(username);
			if (me == null) {
				Map<String, Object> body = new HashMap<>();
				body.put("status", "FAILED");
				body.put("message", "사용자 조회 실패");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
			}
			// 1) 보호자: role_no 유지, subscription_status=1
			lx.project.dementia_care.vo.UserVO g = new lx.project.dementia_care.vo.UserVO();
			g.setUserNo(me.getUserNo());
			g.setSubscriptionStatus(1);
			int updatedGuardian = userDAO.updateById(g);
			// 2) 환자: guardian1_no 기준 1명 → role_no=3, subscription_status=1
			Integer patientNo = connectDAO.findPatientNoByGuardian1No(me.getUserNo());
			int updatedPatients = 0;
			if (patientNo != null) {
				lx.project.dementia_care.vo.UserVO p = new lx.project.dementia_care.vo.UserVO();
				p.setUserNo(patientNo);
				p.setRoleNo(3);
				p.setSubscriptionStatus(1);
				updatedPatients = userDAO.updateById(p);
			}
			if (updatedGuardian <= 0) {
				Map<String, Object> body = new HashMap<>();
				body.put("status", "FAILED");
				body.put("message", "보호자 구독 상태 업데이트 실패");
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
			}
			Map<String, Object> ok = new HashMap<>();
			ok.put("status", "PAID");
			ok.put("updatedGuardian", updatedGuardian);
			ok.put("updatedPatients", updatedPatients);
			return ResponseEntity.ok(ok);
		} catch (Exception e) {
			e.printStackTrace();
			Map<String, Object> body = new HashMap<>();
			body.put("status", "FAILED");
			body.put("message", "결제 처리 중 오류가 발생했습니다.");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
		}
	}

}
