package lx.project.dementia_care.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lx.project.dementia_care.dto.PostListDto;
import lx.project.dementia_care.dto.PostRequestDto;
import lx.project.dementia_care.dto.PostResponseDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PostDAO {

    @Autowired
    private SqlSessionTemplate sqlSession; // SQL 실행을 담당하는 핵심 객체

    private static final String NAMESPACE = "lx.project.dementia_care.mapper.PostMapper";

    public PostResponseDto findPostById(Integer postId) {
        return sqlSession.selectOne(NAMESPACE + ".findPostById", postId);
    }

    public List<PostListDto> findAllPosts() {
        return sqlSession.selectList(NAMESPACE + ".findAllPosts");
    }

    // 지겸
    // 공지만 조회
    public List<PostListDto> findNotices() {
        return sqlSession.selectList(NAMESPACE + ".findNotices");
    }

    public void createPost(PostRequestDto requestDto) {
        sqlSession.insert(NAMESPACE + ".createPost", requestDto);
    }

    public void updatePost(Integer postId, PostRequestDto requestDto) {
        // 파라미터가 2개 이상일 때는 Map으로 묶어서 보내는 것이 일반적
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        params.put("dto", requestDto);
        // sqlSession.update("namespace.id", 파라미터);
        sqlSession.update(NAMESPACE + ".updatePost", params);
    }

    public void deletePost(Integer postId) {
        sqlSession.delete(NAMESPACE + ".deletePost", postId);
    }

    public void incrementViewCount(Integer postId) {
        sqlSession.update(NAMESPACE + ".incrementViewCount", postId);
    }

    /**
     * 댓글에 좋아요를 눌렀는지 확인
     */
    public int checkLike(Integer postId, Integer userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        params.put("userId", userId);
        return sqlSession.selectOne(NAMESPACE + ".checkLike", params);
    }
    
    /**
     * DB에 좋아요 기록
     */
    public void addLike(Integer postId, Integer userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        params.put("userId", userId);
        sqlSession.insert(NAMESPACE + ".addLike", params);
    }
    
    /**
     * DB에 좋아요 기록 삭제
     */
    public void removeLike(Integer postId, Integer userId) {
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        params.put("userId", userId);
        sqlSession.delete(NAMESPACE + ".removeLike", params);
    }
}