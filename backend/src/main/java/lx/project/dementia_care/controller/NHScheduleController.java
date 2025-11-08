package lx.project.dementia_care.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lx.project.dementia_care.dto.ScheduleRequest;
import lx.project.dementia_care.service.NeighborScheduleService;
import lx.project.dementia_care.vo.NeighborScheduleVO;
import lx.project.dementia_care.vo.ScheduleVO;
import lx.project.dementia_care.vo.UserVO;

@RestController
@RequestMapping("/NH/api")
public class NHScheduleController {

    @Autowired
    private NeighborScheduleService neighborScheduleService;

    // 이웃 일정 등록
    @PostMapping("/create-NH")
    public ResponseEntity<?> createNeighborSchedule(@RequestBody ScheduleRequest request) {
        try {
            // 현재 로그인한 사용자 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            UserVO currentUser = (UserVO) auth.getPrincipal();
            int userNo = currentUser.getUserNo();

            ScheduleVO schedule = new ScheduleVO();
            schedule.setScheduleTitle(request.getTitle());
            schedule.setContent(request.getContent());
            schedule.setScheduleDate(LocalDate.parse(request.getDate()));
            schedule.setStartTime(null); // null 값
            schedule.setEndTime(null); // null 값
            schedule.setUserNo(userNo);

            neighborScheduleService.saveNeighborSchedule(request, userNo);

            return ResponseEntity.ok(Map.of(
                    "message", "일정이 성공적으로 저장되었습니다."));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "일정 저장 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    @GetMapping("/plaza-schedules")
    public ResponseEntity<?> getPlazaSchedules() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            UserVO currentUser = (UserVO) auth.getPrincipal();
            int userNo = currentUser.getUserNo();

            // 1. 현재 사용자가 속한 광장명 조회
            String plazaName = neighborScheduleService.getPlazaNameByUserNo(userNo);
            
            if (plazaName == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "속한 광장이 없습니다."));
            }

            // 2. 그 광장의 모든 일정 조회
            List<NeighborScheduleVO> schedules = neighborScheduleService.getNeighborSchedulesByPlazaName(plazaName);

            return ResponseEntity.ok(schedules);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "일정 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // 오늘, 내일 일정 2개 조회
    @GetMapping("/schedule/upcoming")
    public ResponseEntity<?> getUpcomingSchedules() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "로그인이 필요합니다."));
            }

            UserVO currentUser = (UserVO) auth.getPrincipal();
            int userNo = currentUser.getUserNo();

            List<NeighborScheduleVO> schedules = neighborScheduleService.getUpcomingSchedules(userNo);

            return ResponseEntity.ok(schedules);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "일정 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
