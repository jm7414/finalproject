package lx.project.dementia_care.dao;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lx.project.dementia_care.vo.UserVO;

@Component
public class UserDAO {

    @Autowired
    SqlSession session;
    
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
    
}