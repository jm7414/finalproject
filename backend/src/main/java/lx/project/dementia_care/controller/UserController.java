package lx.project.dementia_care.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @PostMapping("/api/register")
    public ResponseEntity<?> register(@RequestBody UserVO user) {
        try {
            // 중복 체크
            if (userDAO.findById(user.getUserId()) != null) {
                return ResponseEntity.badRequest().body(Map.of("message", "이미 존재하는 아이디입니다."));
            }

            // 비밀번호 암호화
            user.setUserPw(passwordEncoder.encode(user.getUserPw()));

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

    /* 내정보 수정 */
    @PostMapping("/api/user/update")
    public ResponseEntity<?> updateCurrentUser(@RequestBody Map<String, Object> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            UserVO current = (UserVO) auth.getPrincipal();

            UserVO patch = new UserVO();
            patch.setUserNo(current.getUserNo());

            if (body.get("name") != null) {
                patch.setName((String) body.get("name"));
            }

            if (body.get("birthDate") != null) {
                String bd = (String) body.get("birthDate");
                if (bd != null && !bd.isBlank()) {
                    if (!bd.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "생년월일 형식이 올바르지 않습니다. (예: 1970-01-31)"));
                    }
                    try {
                        LocalDate parsed = LocalDate.parse(bd); // "yyyy-MM-dd"
                        patch.setBirthDate(parsed);
                    } catch (DateTimeParseException e) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "생년월일 파싱에 실패했습니다. (예: 1970-01-31)"));
                    }
                }
                // 빈 문자열("")이면 패치하지 않음(필요 시 null로 초기화하는 로직을 별도로 추가)
            }

            if (body.get("phoneNumber") != null) {
                patch.setPhoneNumber((String) body.get("phoneNumber"));
            }
            if (body.get("profilePhoto") != null) {
                patch.setProfilePhoto((String) body.get("profilePhoto"));
            }

            int updated = userDAO.updateById(patch);
            if (updated <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "업데이트 대상이 없거나 변경 사항이 없습니다."));
            }

            UserVO fresh = userDAO.findByUserNo(current.getUserNo());
            // 세션의 Principal 갱신
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    fresh, // 최신 UserVO로 교체
                    auth.getCredentials(),
                    auth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            
            return ResponseEntity.ok(fresh);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "사용자 정보 업데이트 중 오류가 발생했습니다."));
        }
    }

}
