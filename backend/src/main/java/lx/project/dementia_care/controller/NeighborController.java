package lx.project.dementia_care.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lx.project.dementia_care.dto.ActiveMemberDTO;
import lx.project.dementia_care.dto.CreatePlazaRequestDTO;
import lx.project.dementia_care.dto.PlazaInfoDTO;
import lx.project.dementia_care.dto.PlazaMemberDTO;
import lx.project.dementia_care.dto.UpdateLocationRequestDTO;
import lx.project.dementia_care.service.NeighborService;
import lx.project.dementia_care.vo.NeighborFriendVO;
import lx.project.dementia_care.vo.PlazaVO;
import lx.project.dementia_care.vo.UserVO;

@RestController
@RequestMapping("/NH/api/neighbor")
public class NeighborController {

    @Autowired
    private NeighborService neighborService;

    /**
     * Spring Security에서 현재 로그인한 사용자 가져오기
     */
    private UserVO getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserVO) {
            return (UserVO) principal;
        }
        return null;
    }

    // ==================== 친구 관리 ====================

    @PostMapping("/friends/{friendUserNo}")
    public ResponseEntity<?> addFriend(@PathVariable Integer friendUserNo) {
        try {
            UserVO loginUser = getLoginUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            Integer myUserNo = loginUser.getUserNo();
            neighborService.addFriend(myUserNo, friendUserNo);

            return ResponseEntity.ok(Map.of("message", "친구가 추가되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "친구 추가 중 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/friends/{friendUserNo}")
    public ResponseEntity<?> removeFriend(@PathVariable Integer friendUserNo) {
        try {
            UserVO loginUser = getLoginUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            Integer myUserNo = loginUser.getUserNo();
            neighborService.removeFriend(myUserNo, friendUserNo);

            return ResponseEntity.ok(Map.of("message", "친구가 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "친구 삭제 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/friends")
    public ResponseEntity<?> getMyFriends() {
        try {
            UserVO loginUser = getLoginUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            Integer myUserNo = loginUser.getUserNo();
            List<NeighborFriendVO> friends = neighborService.getMyFriends(myUserNo);

            return ResponseEntity.ok(friends);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "친구 목록 조회 중 오류가 발생했습니다."));
        }
    }

    // ==================== 광장 관리 ====================

    @PostMapping("/plazas/create")
    public ResponseEntity<?> createPlaza(@RequestBody CreatePlazaRequestDTO request) {
        try {
            UserVO loginUser = getLoginUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            Integer ownerUserNo = loginUser.getUserNo();
            Integer plazaNo = neighborService.createPlazaWithSafeZone(
                    ownerUserNo,
                    request.getPlazaName(),
                    request.getCenterLat(),
                    request.getCenterLng(),
                    request.getRadiusMeters()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("plazaNo", plazaNo);
            response.put("message", "광장이 생성되었습니다.");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "광장 생성 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/plazas/{plazaNo}")
    public ResponseEntity<?> getPlazaInfo(@PathVariable Integer plazaNo) {
        try {
            PlazaInfoDTO plazaInfo = neighborService.getPlazaInfo(plazaNo);
            return ResponseEntity.ok(plazaInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "광장 정보 조회 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/plazas/{plazaNo}/members")
    public ResponseEntity<?> getPlazaMembers(@PathVariable Integer plazaNo) {
        try {
            List<PlazaMemberDTO> members = neighborService.getPlazaMembers(plazaNo);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "멤버 목록 조회 중 오류가 발생했습니다."));
        }
    }

    @GetMapping("/plazas/{plazaNo}/active-members")
    public ResponseEntity<?> getActiveMembersInPlaza(@PathVariable Integer plazaNo) {
        try {
            List<ActiveMemberDTO> activeMembers = neighborService.getActiveMembersInPlaza(plazaNo);
            return ResponseEntity.ok(activeMembers);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "활성 멤버 조회 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/plazas/{plazaNo}/invite/{friendUserNo}")
    public ResponseEntity<?> inviteToPlaza(
            @PathVariable Integer plazaNo,
            @PathVariable Integer friendUserNo) {
        try {
            UserVO loginUser = getLoginUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            Integer ownerUserNo = loginUser.getUserNo();
            neighborService.inviteToPlaza(plazaNo, ownerUserNo, friendUserNo);

            return ResponseEntity.ok(Map.of("message", "친구가 초대되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "초대 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/plazas/{plazaNo}/leave")
    public ResponseEntity<?> leavePlaza(@PathVariable Integer plazaNo) {
        try {
            UserVO loginUser = getLoginUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            Integer userNo = loginUser.getUserNo();
            neighborService.leavePlaza(plazaNo, userNo);

            return ResponseEntity.ok(Map.of("message", "광장에서 탈퇴했습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "탈퇴 중 오류가 발생했습니다."));
        }
    }

    @DeleteMapping("/plazas/{plazaNo}")
    public ResponseEntity<?> deletePlaza(@PathVariable Integer plazaNo) {
        try {
            UserVO loginUser = getLoginUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            Integer ownerUserNo = loginUser.getUserNo();
            neighborService.deletePlaza(plazaNo, ownerUserNo);

            return ResponseEntity.ok(Map.of("message", "광장이 삭제되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "광장 삭제 중 오류가 발생했습니다."));
        }
    }

    // ==================== ✅ 추가: 내 광장 조회 ====================

    /**
     * 내가 방장인 광장 조회
     * GET /NH/api/neighbor/plazas/my-owned
     */
    @GetMapping("/plazas/my-owned")
    public ResponseEntity<?> getMyOwnedPlaza() {
        try {
            UserVO loginUser = getLoginUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            Integer myUserNo = loginUser.getUserNo();
            PlazaVO myPlaza = neighborService.getMyOwnedPlaza(myUserNo);

            if (myPlaza == null) {
                return ResponseEntity.ok(null);
            }

            // 멤버 수 조회
            List<PlazaMemberDTO> members = neighborService.getPlazaMembers(myPlaza.getPlazaNo());

            Map<String, Object> response = new HashMap<>();
            response.put("plazaNo", myPlaza.getPlazaNo());
            response.put("plazaName", myPlaza.getPlazaName());
            response.put("memberCount", members.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "광장 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 내가 이웃으로 참여 중인 광장 목록 조회
     * GET /NH/api/neighbor/plazas/my-joined
     */
    @GetMapping("/plazas/my-joined")
    public ResponseEntity<?> getMyJoinedPlazas() {
        try {
            UserVO loginUser = getLoginUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            Integer myUserNo = loginUser.getUserNo();
            List<Map<String, Object>> joinedPlazas = neighborService.getMyJoinedPlazas(myUserNo);

            return ResponseEntity.ok(joinedPlazas);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "광장 조회 중 오류가 발생했습니다."));
        }
    }

    // ==================== 위치 업데이트 ====================

    @PostMapping("/location/update")
    public ResponseEntity<?> updateMyLocation(@RequestBody UpdateLocationRequestDTO request) {
        try {
            UserVO loginUser = getLoginUser();
            if (loginUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            Integer userNo = loginUser.getUserNo();
            neighborService.updateUserLocation(userNo, request.getLatitude(), request.getLongitude());

            return ResponseEntity.ok(Map.of("message", "위치가 업데이트되었습니다."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "위치 업데이트 중 오류가 발생했습니다."));
        }
    }
}
