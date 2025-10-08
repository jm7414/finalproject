package lx.project.dementia_care.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lx.project.dementia_care.dao.UserDAO;
import lx.project.dementia_care.vo.UserVO;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserDAO userDAO;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            UserVO user = userDAO.findById(username);
            if (user == null) {
                throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
            }
            return user;
        } catch (Exception e) {
            throw new UsernameNotFoundException("사용자 조회 중 오류 발생: " + username, e);
        }
    }
}
