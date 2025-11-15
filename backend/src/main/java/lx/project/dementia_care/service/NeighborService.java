package lx.project.dementia_care.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lx.project.dementia_care.dao.NeighborDAO;
import lx.project.dementia_care.dto.ActiveMemberDTO;
import lx.project.dementia_care.dto.MyPlazaDTO;
import lx.project.dementia_care.dto.PlazaInfoDTO;
import lx.project.dementia_care.dto.PlazaMemberDTO;
// 공지 기능으로 인한 추가
import lx.project.dementia_care.dto.PlazaNoticeDTO;
import lx.project.dementia_care.dto.PlazaScheduleDTO;
import lx.project.dementia_care.vo.NeighborFriendVO;
import lx.project.dementia_care.vo.PlazaVO;
import lx.project.dementia_care.vo.SafeZoneVO;
import lx.project.dementia_care.vo.LocationDataVO;

/**
 * 이웃 기능 Service
 * - Jackson ObjectMapper 사용 (org.json 대신)
 * - 친구 관리, 광장 관리, 위치 관리
 */
@Service
public class NeighborService {

    @Autowired
    private NeighborDAO neighborDAO;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 친구 관리 ====================

    /**
     * 친구 추가
     */
    @Transactional
    public void addFriend(Integer myUserNo, Integer friendUserNo) {
        // 자기 자신을 친구로 추가하는 경우 방지
        if (myUserNo.equals(friendUserNo)) {
            throw new RuntimeException("자기 자신을 친구로 추가할 수 없습니다.");
        }

        // 이미 친구인지 확인
        if (neighborDAO.isFriend(myUserNo, friendUserNo)) {
            throw new RuntimeException("이미 친구입니다.");
        }

        // 작은 번호를 user_no_1에, 큰 번호를 user_no_2에 저장
        Integer userNo1 = Math.min(myUserNo, friendUserNo);
        Integer userNo2 = Math.max(myUserNo, friendUserNo);

        Map<String, Integer> params = new HashMap<>();
        params.put("userNo1", userNo1);
        params.put("userNo2", userNo2);

        neighborDAO.insertFriendship(params);
    }

    /**
     * 친구 삭제
     */
    @Transactional
    public void removeFriend(Integer myUserNo, Integer friendUserNo) {
        // 친구 관계가 없는 경우
        if (!neighborDAO.isFriend(myUserNo, friendUserNo)) {
            throw new RuntimeException("친구 관계가 아닙니다.");
        }

        Integer userNo1 = Math.min(myUserNo, friendUserNo);
        Integer userNo2 = Math.max(myUserNo, friendUserNo);

        Map<String, Integer> params = new HashMap<>();
        params.put("userNo1", userNo1);
        params.put("userNo2", userNo2);

        neighborDAO.deleteFriendship(params);
    }

    /**
     * 내 친구 목록 조회
     */
    public List<NeighborFriendVO> getMyFriends(Integer myUserNo) {
        return neighborDAO.getMyFriends(myUserNo);
    }

    // ==================== 광장 관리 ====================

    /**
     * 광장 생성 (safe_zone과 함께)
     * 수정: 1개 광장만 허용
     */
    @Transactional
    public Integer createPlazaWithSafeZone(
            Integer ownerUserNo,
            String plazaName,
            Double centerLat,
            Double centerLng,
            Integer radiusMeters) {

        // 이웃 권한 확인 (role_no = 4)
        validateNeighborRole(ownerUserNo);

        // 수정: 이미 광장에 속해있는지 확인 (1개만 허용)
        if (isUserInAnyPlaza(ownerUserNo)) {
            throw new IllegalStateException("이미 광장에 속해 있습니다. 광장은 1개만 소속 가능합니다.");
        }

        // 다음 plaza_no 가져오기
        Integer plazaNo = neighborDAO.getNextPlazaNo();

        // 1. plaza 테이블에 방장 추가
        PlazaVO plaza = new PlazaVO();
        plaza.setPlazaNo(plazaNo);
        plaza.setPlazaName(plazaName);
        plaza.setMemberName("방장");
        plaza.setUserNo(ownerUserNo);

        neighborDAO.insertPlazaMember(plaza);

        // 2. safe_zone 테이블에 버퍼 정보 저장 (Jackson 사용)
        try {
            Map<String, Object> boundary = new HashMap<>();

            Map<String, Double> center = new HashMap<>();
            center.put("lat", centerLat);
            center.put("lng", centerLng);

            boundary.put("center", center);
            boundary.put("radius", radiusMeters);
            boundary.put("plazaNo", plazaNo);

            String boundaryJson = objectMapper.writeValueAsString(boundary);

            SafeZoneVO safeZone = new SafeZoneVO();
            safeZone.setZoneType("만남의 광장");
            safeZone.setBoundaryCoordinates(boundaryJson);
            safeZone.setUserNo(ownerUserNo);

            neighborDAO.insertSafeZone(safeZone);

        } catch (Exception e) {
            throw new RuntimeException("광장 버퍼 생성 중 오류가 발생했습니다: " + e.getMessage());
        }

        return plazaNo;
    }

    /**
     * 광장 정보 조회
     */
    public PlazaInfoDTO getPlazaInfo(Integer plazaNo) {
        PlazaVO plaza = neighborDAO.getPlazaByNo(plazaNo);
        if (plaza == null) {
            throw new RuntimeException("광장을 찾을 수 없습니다.");
        }

        SafeZoneVO safeZone = neighborDAO.getSafeZoneByPlazaNo(plazaNo);
        if (safeZone == null) {
            throw new RuntimeException("광장 버퍼 정보를 찾을 수 없습니다.");
        }

        try {
            // Jackson으로 JSON 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> boundary = objectMapper.readValue(
                    safeZone.getBoundaryCoordinates(), Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Double> center = (Map<String, Double>) boundary.get("center");
            Integer radius = (Integer) boundary.get("radius");

            PlazaInfoDTO dto = new PlazaInfoDTO();
            dto.setPlazaNo(plazaNo);
            dto.setPlazaName(plaza.getPlazaName());
            dto.setCenterLat(center.get("lat"));
            dto.setCenterLng(center.get("lng"));
            dto.setRadius(radius);

            return dto;

        } catch (Exception e) {
            throw new RuntimeException("광장 정보 파싱 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 광장 멤버 목록 조회
     */
    public List<PlazaMemberDTO> getPlazaMembers(Integer plazaNo) {
        return neighborDAO.getPlazaMembers(plazaNo);
    }

    /**
     * 광장 내 활성 멤버 조회 (버퍼 안에 있는 이웃)
     */
    public List<ActiveMemberDTO> getActiveMembersInPlaza(Integer plazaNo) {
        // 광장 버퍼 정보 가져오기
        SafeZoneVO safeZone = neighborDAO.getSafeZoneByPlazaNo(plazaNo);
        if (safeZone == null) {
            return new ArrayList<>();
        }

        try {
            // Jackson으로 버퍼 중심 좌표와 반경 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> boundary = objectMapper.readValue(
                    safeZone.getBoundaryCoordinates(), Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Double> center = (Map<String, Double>) boundary.get("center");
            Integer radius = (Integer) boundary.get("radius");

            Double centerLat = center.get("lat");
            Double centerLng = center.get("lng");

            // 광장 멤버들의 최근 위치 조회 (5분 이내)
            List<ActiveMemberDTO> allMembers = neighborDAO.getPlazaMembersRecentLocation(plazaNo);

            // 버퍼 안에 있는 멤버만 필터링
            List<ActiveMemberDTO> activeMembers = new ArrayList<>();
            for (ActiveMemberDTO member : allMembers) {
                double distance = calculateDistance(
                        centerLat, centerLng,
                        member.getLatitude().doubleValue(),
                        member.getLongitude().doubleValue());

                if (distance <= radius) {
                    member.setDistanceFromCenter(distance);
                    activeMembers.add(member);
                }
            }

            return activeMembers;

        } catch (Exception e) {
            throw new RuntimeException("활성 멤버 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 광장에 친구 초대
     * 수정: 1개 광장만 허용 (초대할 친구도 확인)
     */
    @Transactional
    public void inviteToPlaza(Integer plazaNo, Integer ownerUserNo, Integer friendUserNo) {
        // 방장 권한 확인
        if (!neighborDAO.isPlazaOwner(plazaNo, ownerUserNo)) {
            throw new RuntimeException("방장만 초대할 수 있습니다.");
        }

        // 친구 관계 확인
        if (!neighborDAO.isFriend(ownerUserNo, friendUserNo)) {
            throw new RuntimeException("친구만 초대할 수 있습니다.");
        }

        // 수정: 초대할 친구가 이미 광장에 속해있는지 확인
        if (isUserInAnyPlaza(friendUserNo)) {
            throw new IllegalStateException("이미 다른 광장에 속해 있어 초대할 수 없습니다.");
        }

        // 이미 멤버인지 확인
        if (neighborDAO.isPlazaMember(plazaNo, friendUserNo)) {
            throw new RuntimeException("이미 광장 멤버입니다.");
        }

        // 광장 정보 가져오기
        PlazaVO plaza = neighborDAO.getPlazaByNo(plazaNo);

        // 멤버 추가
        PlazaVO newMember = new PlazaVO();
        newMember.setPlazaNo(plazaNo);
        newMember.setPlazaName(plaza.getPlazaName());
        newMember.setMemberName("이웃");
        newMember.setUserNo(friendUserNo);

        neighborDAO.insertPlazaMember(newMember);
    }

    /**
     * 광장 탈퇴
     */
    @Transactional
    public void leavePlaza(Integer plazaNo, Integer userNo) {
        // 멤버인지 확인
        if (!neighborDAO.isPlazaMember(plazaNo, userNo)) {
            throw new RuntimeException("광장 멤버가 아닙니다.");
        }

        // 방장은 탈퇴 불가 (광장 삭제만 가능)
        if (neighborDAO.isPlazaOwner(plazaNo, userNo)) {
            throw new RuntimeException("방장은 탈퇴할 수 없습니다. 광장 삭제를 이용해주세요.");
        }

        neighborDAO.deletePlazaMember(plazaNo, userNo);
    }

    /**
     * 광장 삭제 (방장만 가능)
     */
    @Transactional
    public void deletePlaza(Integer plazaNo, Integer ownerUserNo) {
        // 방장 권한 확인
        if (!neighborDAO.isPlazaOwner(plazaNo, ownerUserNo)) {
            throw new RuntimeException("방장만 광장을 삭제할 수 있습니다.");
        }

        // safe_zone 삭제
        neighborDAO.deleteSafeZoneByPlazaNo(plazaNo);

        // plaza 전체 삭제 (모든 멤버)
        neighborDAO.deletePlazaByNo(plazaNo);
    }

    // ==================== 위치 관리 ====================

    /**
     * 사용자 위치 업데이트
     */
    @Transactional
    public void updateUserLocation(Integer userNo, Double latitude, Double longitude) {
        LocationDataVO location = new LocationDataVO();
        location.setUserNo(userNo);
        location.setLatitude(new BigDecimal(latitude));
        location.setLongitude(new BigDecimal(longitude));

        neighborDAO.insertUserLocation(location);
    }

    // ==================== 내 광장 조회 ====================

    /**
     * 내가 속한 광장 조회 (1개)
     */
    public MyPlazaDTO getMyPlaza(Integer userNo) {
        return neighborDAO.getMyPlaza(userNo);
    }

    /**
     * 사용자가 광장에 속해있는지 확인
     */
    public boolean isUserInAnyPlaza(Integer userNo) {
        Integer count = neighborDAO.countUserPlazas(userNo);
        return count != null && count > 0;
    }

    // ==================== 공지 기능으로 인한 추가 ====================

    /**
     * 내 광장의 공지 목록 조회
     */
    public List<PlazaNoticeDTO> getMyPlazaNotices(Integer userNo) {
        // 1. 내가 속한 광장 조회
        MyPlazaDTO myPlaza = getMyPlaza(userNo);

        if (myPlaza == null) {
            return new ArrayList<>();
        }

        // 2. 해당 광장의 공지 조회
        return neighborDAO.getPlazaNotices(myPlaza.getPlazaNo());
    }

    /**
     * 공지 작성 (방장만 가능)
     */
    @Transactional
    public void createPlazaNotice(Integer userNo, String title, String content) {
        // 1. 내가 속한 광장 조회
        MyPlazaDTO myPlaza = getMyPlaza(userNo);

        if (myPlaza == null) {
            throw new RuntimeException("속한 광장이 없습니다.");
        }

        // 2. 방장 권한 확인
        if (!myPlaza.isOwner()) {
            throw new IllegalStateException("방장만 공지를 작성할 수 있습니다.");
        }

        // 3. 공지 작성
        Map<String, Object> params = new HashMap<>();
        params.put("title", title);
        params.put("content", content);
        params.put("userNo", userNo);
        params.put("plazaNo", myPlaza.getPlazaNo());

        neighborDAO.insertPlazaNotice(params);
    }

    // ===== 공지 기능 =====

    /**
     * 공지 수정 (방장만 가능)
     */
    @Transactional
    public void updatePlazaNotice(Integer noticeNo, Integer userNo, String title, String content) {
        // 1. 내가 속한 광장 조회
        MyPlazaDTO myPlaza = getMyPlaza(userNo);
        if (myPlaza == null) {
            throw new RuntimeException("속한 광장이 없습니다.");
        }

        // 2. 방장 권한 확인
        if (!myPlaza.isOwner()) {
            throw new IllegalStateException("방장만 공지를 수정할 수 있습니다.");
        }

        // 3. 수정 파라미터 설정 및 DAO 호출
        Map<String, Object> params = new HashMap<>();
        params.put("noticeNo", noticeNo);
        params.put("title", title);
        params.put("content", content);
        params.put("plazaNo", myPlaza.getPlazaNo());

        neighborDAO.updatePlazaNotice(params);
    }

    /**
     * 공지 삭제 (방장만 가능)
     */
    @Transactional
    public void deletePlazaNotice(Integer noticeNo, Integer userNo) {
        // 1. 내가 속한 광장 조회
        MyPlazaDTO myPlaza = getMyPlaza(userNo);
        if (myPlaza == null) {
            throw new RuntimeException("속한 광장이 없습니다.");
        }

        // 2. 방장 권한 확인
        if (!myPlaza.isOwner()) {
            throw new IllegalStateException("방장만 공지를 삭제할 수 있습니다.");
        }

        neighborDAO.deletePlazaNotice(noticeNo);
    }

    /**
     * 내 광장에서의 역할 조회 (방장/이웃)
     */
    public String getMyPlazaRole(Integer userNo) {
        MyPlazaDTO myPlaza = getMyPlaza(userNo);

        if (myPlaza == null) {
            return null;
        }

        return myPlaza.getMemberName();
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 이웃 권한 확인 (role_no = 4)
     */
    private void validateNeighborRole(Integer userNo) {
        // 실제로는 UserDAO를 통해 role_no를 확인해야 하지만,
        // 여기서는 간단히 로직만 표시
        // TODO: UserDAO에서 role_no 확인 로직 추가
    }

    /**
     * 두 좌표 사이의 거리 계산 (Haversine formula)
     * 
     * @return 거리 (미터)
     */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final int EARTH_RADIUS = 6371000; // 지구 반지름 (미터)

        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    // ==================== 일정 기능으로 인한 추가 ====================

    /**
     * 내 광장의 일정 목록 조회
     */
    public List<PlazaScheduleDTO> getMyPlazaSchedules(Integer userNo) {
        // 1. 내가 속한 광장 조회
        MyPlazaDTO myPlaza = getMyPlaza(userNo);

        if (myPlaza == null) {
            return new ArrayList<>();
        }

        // 2. 해당 광장의 일정 조회
        return neighborDAO.getPlazaSchedules(myPlaza.getPlazaNo());
    }

    /**
     * 일정 작성 (광장 멤버 모두 가능)
     */
    @Transactional
    public void createPlazaSchedule(Integer userNo, String title, String content, String scheduleDate) {
        // 1. 내가 속한 광장 조회
        MyPlazaDTO myPlaza = getMyPlaza(userNo);

        if (myPlaza == null) {
            throw new RuntimeException("속한 광장이 없습니다.");
        }

        // 2. 광장 멤버인지 확인 (방장과 이웃 모두 가능)
        if (!neighborDAO.isPlazaMember(myPlaza.getPlazaNo(), userNo)) {
            throw new IllegalStateException("광장 멤버만 일정을 작성할 수 있습니다.");
        }

        // 3. 일정 작성
        Map<String, Object> params = new HashMap<>();
        params.put("scheduleTitle", title);
        params.put("content", content);
        params.put("scheduleDate", Date.valueOf(scheduleDate)); // String을 Date로 변환
        params.put("userNo", userNo);
        params.put("plazaNo", myPlaza.getPlazaNo());

        neighborDAO.insertPlazaSchedule(params);
    }

    /**
     * 일정 삭제 (작성자만 가능)
     */
    @Transactional
    public void deletePlazaSchedule(Integer scheduleNo, Integer userNo) {
        // 1. 일정 작성자 확인
        PlazaScheduleDTO schedule = neighborDAO.getPlazaScheduleByNo(scheduleNo);

        if (schedule == null) {
            throw new RuntimeException("일정을 찾을 수 없습니다.");
        }

        // 2. 작성자 본인인지 확인
        if (!schedule.getUserNo().equals(userNo)) {
            throw new IllegalStateException("작성자만 일정을 삭제할 수 있습니다.");
        }

        // 3. 일정 삭제
        neighborDAO.deletePlazaSchedule(scheduleNo);
    }

}
