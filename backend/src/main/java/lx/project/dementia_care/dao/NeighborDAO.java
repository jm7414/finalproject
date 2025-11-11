package lx.project.dementia_care.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import lx.project.dementia_care.dto.ActiveMemberDTO;
import lx.project.dementia_care.dto.PlazaMemberDTO;
import lx.project.dementia_care.vo.NeighborFriendVO;
import lx.project.dementia_care.vo.PlazaVO;
import lx.project.dementia_care.vo.SafeZoneVO;
import lx.project.dementia_care.vo.LocationDataVO;

/**
 * 이웃 기능 DAO
 * - 친구 관계 관리
 * - 광장 멤버 관리
 * - 위치 정보 관리
 */
@Repository
public class NeighborDAO {

    @Autowired
    private SqlSession sqlSession;

    private static final String NAMESPACE = "lx.project.dementia_care.mapper.NeighborMapper";

    // ==================== 친구 관계 ====================

    /**
     * 친구 관계 추가
     */
    public void insertFriendship(Map<String, Integer> params) {
        sqlSession.insert(NAMESPACE + ".insertFriendship", params);
    }

    /**
     * 친구 관계 삭제
     */
    public void deleteFriendship(Map<String, Integer> params) {
        sqlSession.delete(NAMESPACE + ".deleteFriendship", params);
    }

    /**
     * 친구 관계 존재 여부 확인
     */
    public boolean isFriend(Integer userNo1, Integer userNo2) {
        Integer minUser = Math.min(userNo1, userNo2);
        Integer maxUser = Math.max(userNo1, userNo2);

        Map<String, Integer> params = Map.of("userNo1", minUser, "userNo2", maxUser);
        Integer count = sqlSession.selectOne(NAMESPACE + ".isFriend", params);
        return count != null && count > 0;
    }

    /**
     * 내 친구 목록 조회
     */
    public List<NeighborFriendVO> getMyFriends(Integer userNo) {
        return sqlSession.selectList(NAMESPACE + ".getMyFriends", userNo);
    }

    // ==================== 광장 관리 ====================

    /**
     * 다음 plaza_no 가져오기
     */
    public Integer getNextPlazaNo() {
        Integer nextNo = sqlSession.selectOne(NAMESPACE + ".getNextPlazaNo");
        return nextNo != null ? nextNo : 1;
    }

    /**
     * 광장 멤버 추가
     */
    public void insertPlazaMember(PlazaVO plaza) {
        sqlSession.insert(NAMESPACE + ".insertPlazaMember", plaza);
    }

    /**
     * 광장 정보 조회 (첫 번째 멤버 기준)
     */
    public PlazaVO getPlazaByNo(Integer plazaNo) {
        return sqlSession.selectOne(NAMESPACE + ".getPlazaByNo", plazaNo);
    }

    /**
     * 광장 멤버 목록 조회
     */
    public List<PlazaMemberDTO> getPlazaMembers(Integer plazaNo) {
        return sqlSession.selectList(NAMESPACE + ".getPlazaMembers", plazaNo);
    }

    /**
     * 광장 멤버인지 확인
     */
    public boolean isPlazaMember(Integer plazaNo, Integer userNo) {
        Map<String, Integer> params = Map.of("plazaNo", plazaNo, "userNo", userNo);
        Integer count = sqlSession.selectOne(NAMESPACE + ".isPlazaMember", params);
        return count != null && count > 0;
    }

    /**
     * 광장 방장인지 확인
     */
    public boolean isPlazaOwner(Integer plazaNo, Integer userNo) {
        Map<String, Integer> params = Map.of("plazaNo", plazaNo, "userNo", userNo);
        Integer count = sqlSession.selectOne(NAMESPACE + ".isPlazaOwner", params);
        return count != null && count > 0;
    }

    /**
     * 사용자가 이미 방장인 광장이 있는지 확인
     */
    public boolean hasOwnedPlaza(Integer userNo) {
        Integer count = sqlSession.selectOne(NAMESPACE + ".countOwnedPlaza", userNo);
        return count != null && count > 0;
    }

    /**
     * 광장에서 멤버 삭제
     */
    public void deletePlazaMember(Integer plazaNo, Integer userNo) {
        Map<String, Integer> params = Map.of("plazaNo", plazaNo, "userNo", userNo);
        sqlSession.delete(NAMESPACE + ".deletePlazaMember", params);
    }

    /**
     * 광장 전체 삭제 (모든 멤버 삭제)
     */
    public void deletePlazaByNo(Integer plazaNo) {
        sqlSession.delete(NAMESPACE + ".deletePlazaByNo", plazaNo);
    }

    // ==================== 위치 관리 ====================

    /**
     * 사용자 위치 추가
     */
    public void insertUserLocation(LocationDataVO location) {
        sqlSession.insert(NAMESPACE + ".insertUserLocation", location);
    }

    /**
     * 사용자 최근 위치 조회
     */
    public LocationDataVO getRecentUserLocation(Integer userNo) {
        return sqlSession.selectOne(NAMESPACE + ".getRecentUserLocation", userNo);
    }

    /**
     * 광장 멤버들의 최근 위치 조회 (5분 이내)
     */
    public List<ActiveMemberDTO> getPlazaMembersRecentLocation(Integer plazaNo) {
        return sqlSession.selectList(NAMESPACE + ".getPlazaMembersRecentLocation", plazaNo);
    }

    // ==================== Safe Zone (광장 버퍼) ====================

    /**
     * Safe Zone 추가
     */
    public void insertSafeZone(SafeZoneVO safeZone) {
        sqlSession.insert(NAMESPACE + ".insertSafeZone", safeZone);
    }

    /**
     * 광장 번호로 Safe Zone 조회
     */
    public SafeZoneVO getSafeZoneByPlazaNo(Integer plazaNo) {
        return sqlSession.selectOne(NAMESPACE + ".getSafeZoneByPlazaNo", plazaNo);
    }

    /**
     * 광장 번호로 Safe Zone 삭제
     */
    public void deleteSafeZoneByPlazaNo(Integer plazaNo) {
        sqlSession.delete(NAMESPACE + ".deleteSafeZoneByPlazaNo", plazaNo);
    }

    // ==================== ✅ 추가: 내 광장 조회 ====================

    /**
     * 내가 방장인 광장 조회
     */
    public PlazaVO getMyOwnedPlaza(Integer userNo) {
        return sqlSession.selectOne(NAMESPACE + ".getMyOwnedPlaza", userNo);
    }

    /**
     * 내가 이웃으로 참여 중인 광장 목록 조회
     */
    public List<PlazaVO> getMyJoinedPlazas(Integer userNo) {
        return sqlSession.selectList(NAMESPACE + ".getMyJoinedPlazas", userNo);

    }
}
