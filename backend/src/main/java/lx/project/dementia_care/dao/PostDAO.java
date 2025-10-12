package lx.project.dementia_care.dao;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import lx.project.dementia_care.dto.PostListDto;
import lx.project.dementia_care.dto.PostRequestDto;
import lx.project.dementia_care.dto.PostResponseDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository // 이 클래스가 DB와 통신하는 DAO임을 Spring에게 알려줍니다.
public class PostDAO {

    @Autowired
    private SqlSessionTemplate sqlSession; // SQL 실행을 담당하는 핵심 객체

    // XML Mapper의 namespace를 상수로 정의해두면 오타를 방지하고 관리가 편합니다.
    private static final String NAMESPACE = "lx.project.dementia_care.mapper.PostMapper";

    public PostResponseDto findPostById(Integer postId) {
        // sqlSession.selectOne("namespace.id", 파라미터);
        return sqlSession.selectOne(NAMESPACE + ".findPostById", postId);
    }

    public List<PostListDto> findAllPosts() {
        // sqlSession.selectList("namespace.id");
        return sqlSession.selectList(NAMESPACE + ".findAllPosts");
    }

    public void createPost(PostRequestDto requestDto) {
        // sqlSession.insert("namespace.id", 파라미터);
        sqlSession.insert(NAMESPACE + ".createPost", requestDto);
    }

    public void updatePost(Integer postId, PostRequestDto requestDto) {
        // 파라미터가 2개 이상일 때는 Map으로 묶어서 보내는 것이 일반적입니다.
        Map<String, Object> params = new HashMap<>();
        params.put("postId", postId);
        params.put("dto", requestDto);
        // sqlSession.update("namespace.id", 파라미터);
        sqlSession.update(NAMESPACE + ".updatePost", params);
    }

    public void deletePost(Integer postId) {
        // sqlSession.delete("namespace.id", 파라미터);
        sqlSession.delete(NAMESPACE + ".deletePost", postId);
    }

    public void incrementViewCount(Integer postId) {
        sqlSession.update(NAMESPACE + ".incrementViewCount", postId);
    }
}