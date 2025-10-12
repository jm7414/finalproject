package lx.project.dementia_care.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lx.project.dementia_care.dto.PostResponseDto;
import lx.project.dementia_care.dto.PostListDto;
import lx.project.dementia_care.dto.PostRequestDto;


import java.util.List;
import lx.project.dementia_care.dao.PostDAO; // DAO를 import 합니다.

@Service
public class PostService {

    // PostMapper 대신 PostDAO를 주입받습니다.
    private final PostDAO postDAO;

    @Autowired
    public PostService(PostDAO postDAO) {
        this.postDAO = postDAO;
    }

    /**
     * 게시물 상세 조회 (+ 조회수 증가)
     */
    @Transactional
    public PostResponseDto findPostById(Integer postId) {
        // 1. 조회수를 1 증가시킵니다.
        postDAO.incrementViewCount(postId);

        // 2. 증가된 조회수가 반영된 게시물 정보를 가져옵니다.
        PostResponseDto post = postDAO.findPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId);
        }
        return post;
    }

    /**
     * 게시물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PostListDto> findAllPosts() {
        // 변경점 3: 엔터티를 DTO로 변환하는 스트림 로직이 사라지고, Mapper가 처음부터 DTO 목록을 반환합니다.
        return postDAO.findAllPosts();
    }

    /**
     * 게시물 생성
     */
    @Transactional
    public Integer createPost(PostRequestDto requestDto) {
        // 변경점 4: DTO를 직접 Mapper에 전달하여 DB에 삽입합니다.
        // (실제 사용자 ID는 로그인 세션에서 가져와 설정해야 합니다)
        // 예시: requestDto.setUserId(SecurityUtil.getCurrentUserId());
        requestDto.setUserId(1); // 임시 사용자 ID
        
        postDAO.createPost(requestDto);
        return requestDto.getPostId(); // Mapper에서 생성된 postId를 DTO에 다시 담아 반환
    }

    /**
     * 게시물 수정
     */
    @Transactional
    public void updatePost(Integer postId, PostRequestDto requestDto) {
        // 수정 전, 해당 게시물이 존재하는지 확인하는 로직을 추가하면 더 안정적입니다.
        if (postDAO.findPostById(postId) == null) {
            throw new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId);
        }
        postDAO.updatePost(postId, requestDto);
    }

    /**
     * 게시물 삭제
     */
    @Transactional
    public void deletePost(Integer postId) {
        // 삭제 전, 해당 게시물이 존재하는지 확인하는 로직
        if (postDAO.findPostById(postId) == null) {
            throw new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId);
        }
        postDAO.deletePost(postId);
    }
}