package lx.project.dementia_care.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserLocationDAO {
    
    /**
     * 사용자 위치 정보 삽입
     */
    @Insert("INSERT INTO user_location (user_no, latitude, longitude, record_time) " +
            "VALUES (#{userNo}, #{latitude}, #{longitude}, CURRENT_TIMESTAMP)")
    int insertLocation(@Param("userNo") Integer userNo, 
                      @Param("latitude") Double latitude, 
                      @Param("longitude") Double longitude);
}
