package lx.project.dementia_care.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lx.project.dementia_care.dto.PostResponseDto;
import lx.project.dementia_care.dto.PostListDto;
import lx.project.dementia_care.dto.PostRequestDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lx.project.dementia_care.dao.PostDAO;

@Service
public class PostService {

    private final PostDAO postDAO;

    @Autowired
    public PostService(PostDAO postDAO) {
        this.postDAO = postDAO;
    }

    /**
     * 게시물 상세 조회
     */
    @Transactional
    public PostResponseDto findPostById(Integer postId) {
        postDAO.incrementViewCount(postId);
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
        return postDAO.findAllPosts();
    }

    /**
     * 게시물 생성
     */
    @Transactional
    public Integer createPost(PostRequestDto requestDto, Integer userId) {
        requestDto.setUserId(userId);
        postDAO.createPost(requestDto);
        return requestDto.getPostId();
    }

    /**
     * 게시물 수정
     */
    @Transactional
    public void updatePost(Integer postId, PostRequestDto requestDto, Integer currentUserId) throws AccessDeniedException {
        PostResponseDto post = postDAO.findPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId);
        }

        if (!post.getUserId().equals(currentUserId) && currentUserId != 1) {
            throw new AccessDeniedException("수정할 권한이 없습니다.");
        }

        // --- 파일 처리 로직 시작 ---
        // getImageUrl() -> getImage()로 수정
        String oldImage = post.getImage();
        String newImage = requestDto.getImage();

        // 기존 이미지가 있고, 새로 들어온 이미지와 다른 경우 기존 이미지 삭제
        if (oldImage != null && !oldImage.isEmpty() && !oldImage.equals(newImage)) {
            deleteImageFile(oldImage);
        }
        // --- 파일 처리 로직 끝 ---

        postDAO.updatePost(postId, requestDto);
    }

    /**
     * 게시물 삭제
     */
    @Transactional
    public void deletePost(Integer postId, Integer currentUserId) throws AccessDeniedException {
        PostResponseDto post = postDAO.findPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId);
        }

        if (!post.getUserId().equals(currentUserId) && currentUserId != 1) {
            throw new AccessDeniedException("삭제할 권한이 없습니다.");
        }
        
        // --- 파일 처리 로직 시작 ---
        // getImageUrl() -> getImage()로 수정
        String image = post.getImage();
        if (image != null && !image.isEmpty()) {
            deleteImageFile(image);
        }
        // --- 파일 처리 로직 끝 ---
        
        postDAO.deletePost(postId);
    }

    /**
     * 사용자가 특정 게시물의 좋아요를 토글합니다.
     */
    @Transactional
    public int toggleLike(Integer postId, Integer currentUserId) {
        int likeCount = postDAO.checkLike(postId, currentUserId);
        if (likeCount > 0) {
            postDAO.removeLike(postId, currentUserId);
        } else {
            postDAO.addLike(postId, currentUserId);
        }
        PostResponseDto updatedPost = postDAO.findPostById(postId);
        return updatedPost.getLikes();
    }
    
    // --- 이미지 파일 삭제를 위한 private 헬퍼 메소드 ---
    private void deleteImageFile(String imageUrl) {
        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            String projectPath = new File("").getAbsolutePath();
            Path filePath = Paths.get(projectPath + "/src/main/resources/uploads/images/" + fileName);

            Files.deleteIfExists(filePath);
            System.out.println("파일 삭제 성공: " + fileName);

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("파일 삭제 실패: " + imageUrl);
        }
    }
}

