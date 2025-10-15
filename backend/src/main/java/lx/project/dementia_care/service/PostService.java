package lx.project.dementia_care.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lx.project.dementia_care.dto.PostResponseDto;
import lx.project.dementia_care.dto.PostListDto;
import lx.project.dementia_care.dto.PostRequestDto;

import java.nio.file.AccessDeniedException;
import java.util.List;
import lx.project.dementia_care.dao.PostDAO; // DAO를 import 합니다.

@Service
public class PostService {

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
    // 수정: 컨트롤러로부터 userId를 직접 받도록 파라미터를 추가합니다.
    public Integer createPost(PostRequestDto requestDto, Integer userId) {
        // 추가: DTO에 전달받은 userId를 설정합니다.
        requestDto.setUserId(userId);
        
        postDAO.createPost(requestDto);
        return requestDto.getPostId();
    }

    /**
     * 게시물 수정
     */
    @Transactional
    // 수정: currentUserId 파라미터 추가
    public void updatePost(Integer postId, PostRequestDto requestDto, Integer currentUserId) throws AccessDeniedException {
        // 1. 수정하려는 게시물의 정보를 가져옵니다.
        PostResponseDto post = postDAO.findPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId);
        }

        // 2. ✨✨✨ 핵심 권한 체크 ✨✨✨
        // 만약 (현재 사용자가 글쓴이가 아니고) AND (현재 사용자가 운영자(1번)도 아니라면)
        if (!post.getUserId().equals(currentUserId) && currentUserId != 1) {
            // 예외를 발생시켜 수정을 막습니다.
            throw new AccessDeniedException("수정할 권한이 없습니다.");
        }

        // 3. 권한이 있다면 수정을 진행합니다.
        postDAO.updatePost(postId, requestDto);
    }

    /**
     * 게시물 삭제
     */
    @Transactional
    // 수정: currentUserId 파라미터 추가
    public void deletePost(Integer postId, Integer currentUserId) throws AccessDeniedException {
        // 1. 삭제하려는 게시물의 정보를 가져옵니다.
        PostResponseDto post = postDAO.findPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId);
        }

        // 2. ✨✨✨ 핵심 권한 체크 ✨✨✨
        // 만약 (현재 사용자가 글쓴이가 아니고) AND (현재 사용자가 운영자(1번)도 아니라면)
        if (!post.getUserId().equals(currentUserId) && currentUserId != 1) {
            // 예외를 발생시켜 삭제를 막습니다.
            throw new AccessDeniedException("삭제할 권한이 없습니다.");
        }
        
        // 3. 권한이 있다면 삭제를 진행합니다.
        postDAO.deletePost(postId);
    }

    /**
     * 사용자가 특정 게시물의 좋아요를 토글합니다.
     * @return 작업 후의 최종 좋아요 개수
     */
    @Transactional
    public int toggleLike(Integer postId, Integer currentUserId) {
        // 1. 먼저 이 사용자가 이미 좋아요를 눌렀는지 확인합니다.
        int likeCount = postDAO.checkLike(postId, currentUserId);

        if (likeCount > 0) {
            // 2-1. 이미 좋아요를 눌렀다면(likeCount가 1), 좋아요를 취소합니다.
            postDAO.removeLike(postId, currentUserId);
        } else {
            // 2-2. 아직 좋아요를 안 눌렀다면(likeCount가 0), 좋아요를 추가합니다.
            postDAO.addLike(postId, currentUserId);
        }

        // 3. 작업이 끝난 후, 해당 게시물의 최종 좋아요 개수를 다시 조회해서 반환합니다.
        //    이렇게 하면 프론트엔드에서 화면을 바로 업데이트할 수 있습니다.
        PostResponseDto updatedPost = postDAO.findPostById(postId);
        return updatedPost.getLikes();
    }
}