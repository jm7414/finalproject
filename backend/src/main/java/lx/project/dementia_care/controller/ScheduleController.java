package lx.project.dementia_care.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lx.project.dementia_care.dao.ConnectDAO;
import lx.project.dementia_care.dto.ScheduleRequest;
import lx.project.dementia_care.service.ScheduleService;
import lx.project.dementia_care.vo.RouteVO;
import lx.project.dementia_care.vo.SafeZoneVO;
import lx.project.dementia_care.vo.ScheduleLocationVO;
import lx.project.dementia_care.vo.ScheduleVO;
import lx.project.dementia_care.vo.UserVO;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ConnectDAO connectDAO;

    /**
     * 일정 저장 API
     * POST /api/schedule/create
     * 
     * 보호자가 로그인한 상태에서 관리하는 환자의 일정을 저장
     */
    @PostMapping("/create")
    public ResponseEntity<?> createSchedule(@RequestBody ScheduleRequest request) {
        try {
            // 현재 로그인한 보호자 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            UserVO currentGuardian = (UserVO) auth.getPrincipal();
            int guardianUserNo = currentGuardian.getUserNo();

            // 보호자가 관리하는 환자 조회
            UserVO patient = connectDAO.getPatientByGuardianNo(guardianUserNo);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "관리하는 환자가 없습니다."));
            }

            int patientUserNo = patient.getUserNo();

            // 일정 및 관련 데이터 저장
            scheduleService.saveScheduleWithDetails(request, patientUserNo);

            return ResponseEntity.ok(Map.of(
                    "message", "일정이 성공적으로 저장되었습니다.",
                    "patientName", patient.getName()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "일정 저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 특정 사용자의 모든 일정 조회
     * GET /api/schedule/list/{userNo}
     */
    @GetMapping("/list/{userNo}")
    public ResponseEntity<?> getSchedules(@PathVariable int userNo) {
        try {
            List<ScheduleVO> schedules = scheduleService.getSchedulesByUserNo(userNo);
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "일정 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 특정 일정의 위치 목록 조회
     * GET /api/schedule/{scheduleNo}/locations
     */
    @GetMapping("/{scheduleNo}/locations")
    public ResponseEntity<?> getScheduleLocations(@PathVariable int scheduleNo) {
        try {
            List<ScheduleLocationVO> locations = scheduleService.getLocationsByScheduleNo(scheduleNo);
            return ResponseEntity.ok(locations);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "위치 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 특정 일정의 경로 조회
     * GET /api/schedule/{scheduleNo}/route
     */
    @GetMapping("/{scheduleNo}/route")
    public ResponseEntity<?> getScheduleRoute(@PathVariable int scheduleNo) {
        try {
            RouteVO route = scheduleService.getRouteByScheduleNo(scheduleNo);
            return ResponseEntity.ok(route);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "경로 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 특정 사용자의 모든 안심존 조회
     * GET /api/schedule/safe-zones/{userNo}
     */
    @GetMapping("/safe-zones/{userNo}")
    public ResponseEntity<?> getSafeZones(@PathVariable int userNo) {
        try {
            List<SafeZoneVO> safeZones = scheduleService.getSafeZonesByUserNo(userNo);
            return ResponseEntity.ok(safeZones);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "안심존 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 기본형 안심존 조회
     * GET /api/schedule/basic-safe-zone/{userNo}
     */
    @GetMapping("/basic-safe-zone/{userNo}")
    public ResponseEntity<?> getBasicSafeZone(@PathVariable int userNo) {
        try {
            SafeZoneVO basicSafeZone = scheduleService.getBasicSafeZoneByUserNo(userNo);
            if (basicSafeZone == null) {
                return ResponseEntity.ok(Map.of("message", "기본 안심존이 설정되지 않았습니다."));
            }
            return ResponseEntity.ok(basicSafeZone);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "기본 안심존 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 기본형 안심존 저장/수정
     * POST /api/schedule/basic-safe-zone
     */
    @PostMapping("/basic-safe-zone")
    public ResponseEntity<?> saveBasicSafeZone(@RequestBody Map<String, Object> request) {
        try {
            // 현재 로그인한 보호자 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            UserVO currentGuardian = (UserVO) auth.getPrincipal();
            int guardianUserNo = currentGuardian.getUserNo();

            // 보호자가 관리하는 환자 조회
            UserVO patient = connectDAO.getPatientByGuardianNo(guardianUserNo);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "관리하는 환자가 없습니다."));
            }

            int patientUserNo = patient.getUserNo();

            // 기본 안심존 저장/수정
            scheduleService.saveOrUpdateBasicSafeZone(patientUserNo, request.get("boundaryCoordinates").toString());

            return ResponseEntity.ok(Map.of(
                    "message", "기본 안심존이 성공적으로 저장되었습니다.",
                    "patientName", patient.getName()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "기본 안심존 저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 현재 시간에 해당하는 일정 조회
     * GET /api/schedule/current/{userNo}
     */
    @GetMapping("/current/{userNo}")
    public ResponseEntity<?> getCurrentSchedule(@PathVariable int userNo) {
        try {
            ScheduleVO schedule = scheduleService.getCurrentSchedule(userNo);
            if (schedule == null) {
                return ResponseEntity.ok(Map.of("message", "현재 진행 중인 일정이 없습니다."));
            }
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "현재 일정 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 특정 일정 상세 조회
     * GET /api/schedule/{scheduleNo}
     */
    @GetMapping("/{scheduleNo}")
    public ResponseEntity<?> getScheduleDetail(@PathVariable int scheduleNo) {
        try {
            ScheduleVO schedule = scheduleService.getScheduleByNo(scheduleNo);
            if (schedule == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "일정을 찾을 수 없습니다."));
            }
            return ResponseEntity.ok(schedule);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "일정 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 일정 수정 API
     * PUT /api/schedule/update/{scheduleNo}
     */
    @PostMapping("/update/{scheduleNo}")
    public ResponseEntity<?> updateSchedule(@PathVariable int scheduleNo, @RequestBody ScheduleRequest request) {
        try {
            // 현재 로그인한 보호자 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            UserVO currentGuardian = (UserVO) auth.getPrincipal();
            int guardianUserNo = currentGuardian.getUserNo();

            // 보호자가 관리하는 환자 조회
            UserVO patient = connectDAO.getPatientByGuardianNo(guardianUserNo);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "관리하는 환자가 없습니다."));
            }

            int patientUserNo = patient.getUserNo();

            // 일정이 해당 환자의 것인지 확인
            ScheduleVO existingSchedule = scheduleService.getScheduleByNo(scheduleNo);
            if (existingSchedule == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "일정을 찾을 수 없습니다."));
            }
            if (existingSchedule.getUserNo() != patientUserNo) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "이 일정을 수정할 권한이 없습니다."));
            }

            // 일정 수정
            scheduleService.updateScheduleWithDetails(scheduleNo, request, patientUserNo);

            return ResponseEntity.ok(Map.of(
                    "message", "일정이 성공적으로 수정되었습니다.",
                    "scheduleNo", scheduleNo
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "일정 수정 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 일정 삭제 API
     * DELETE /api/schedule/delete/{scheduleNo}
     */
    @PostMapping("/delete/{scheduleNo}")
    public ResponseEntity<?> deleteSchedule(@PathVariable int scheduleNo) {
        try {
            // 현재 로그인한 보호자 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            UserVO currentGuardian = (UserVO) auth.getPrincipal();
            int guardianUserNo = currentGuardian.getUserNo();

            // 보호자가 관리하는 환자 조회
            UserVO patient = connectDAO.getPatientByGuardianNo(guardianUserNo);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "관리하는 환자가 없습니다."));
            }

            int patientUserNo = patient.getUserNo();

            // 일정이 해당 환자의 것인지 확인
            ScheduleVO existingSchedule = scheduleService.getScheduleByNo(scheduleNo);
            if (existingSchedule == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "일정을 찾을 수 없습니다."));
            }
            if (existingSchedule.getUserNo() != patientUserNo) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "이 일정을 삭제할 권한이 없습니다."));
            }

            // 일정 삭제
            scheduleService.deleteScheduleWithDetails(scheduleNo, patientUserNo);

            return ResponseEntity.ok(Map.of("message", "일정이 성공적으로 삭제되었습니다."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "일정 삭제 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

