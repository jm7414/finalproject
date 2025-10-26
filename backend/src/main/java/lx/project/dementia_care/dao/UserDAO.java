package lx.project.dementia_care.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lx.project.dementia_care.vo.UserVO;

@Component
public class UserDAO {

    @Autowired
    SqlSession session;

    private static String NAMESPACE = "mapper-user";
    
    public int insertUser(UserVO user) throws Exception {
        return session.insert("mapper-user.insertUser", user);
    }

    public UserVO findById(String userId) throws Exception {
        return session.selectOne("mapper-user.findById", userId);
    }
    
    public UserVO findByUserNo(int userNo) throws Exception {
        return session.selectOne("mapper-user.findByUserNo", userNo);
    }
    
    public int updateById(UserVO user) throws Exception {
        return session.update("mapper-user.updateById", user);
    }
    
    public int deleteById(int userNo) throws Exception {
        return session.delete("mapper-user.deleteById", userNo);
    }

    /**
     *  실종상태인 유저 정보를 불러옴
     */
    public List<UserVO> findMissingUsers() {
        return session.selectList(NAMESPACE + ".findMissingUsers");
    }

    /**
     * 특정 사용자의 user_status를 업데이트합니다.
     * @param userNo 상태를 변경할 사용자의 번호
     * @param status 새로운 상태 값 (0 또는 1)
     */
    public void updateUserStatus(Integer userNo, Integer status) {
        Map<String, Object> params = new HashMap<>();
        params.put("userNo", userNo);
        params.put("status", status);
        session.update("mapper-user.updateUserStatus", params);
    }
    
}