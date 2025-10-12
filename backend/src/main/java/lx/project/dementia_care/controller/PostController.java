package lx.project.dementia_care.controller;

import lx.project.dementia_care.vo.UserVO;
import lx.project.dementia_care.dto.PostListDto;
import lx.project.dementia_care.dto.PostRequestDto;
import lx.project.dementia_care.dto.PostResponseDto;
import lx.project.dementia_care.service.PostService;

import java.nio.file.AccessDeniedException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @Autowired
    public PostController(PostService postService) {
        this.postService = postService;
    }

    // 나중에 id없거나 문제있을 때 예외처리 해주기
    @GetMapping("/{id}")
    public PostResponseDto getPostById(@PathVariable Integer id) {
        return postService.findPostById(id);
    }

    // 게시판 - board
    @GetMapping
    public List<PostListDto> getAllPosts() {
        return postService.findAllPosts();
    }

    // 게시판 - 생성
    @PostMapping
    public ResponseEntity<Integer> createPost(@RequestBody PostRequestDto requestDto, Authentication authentication) {
        // 로그인하지 않은 사용자 요청을 거부
        if (authentication == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED); // 401 에러
        }
        UserVO userVO = (UserVO) authentication.getPrincipal();
        Integer userId = userVO.getUserNo();
        Integer newPostId = postService.createPost(requestDto, userId);
        return new ResponseEntity<>(newPostId, HttpStatus.CREATED);
    }

    // 게시판 - 수정
    @PutMapping("/{id}")
    // 추가: Authentication 파라미터
    public ResponseEntity<Void> updatePost(@PathVariable Integer id, @RequestBody PostRequestDto requestDto, Authentication authentication) throws AccessDeniedException {
        // 추가: 현재 로그인한 사용자의 userNo를 알아냅니다.
        UserVO userVO = (UserVO) authentication.getPrincipal();
        Integer currentUserId = userVO.getUserNo();
        
        // 수정: 서비스에 현재 사용자 ID를 함께 전달합니다.
        postService.updatePost(id, requestDto, currentUserId);
        return ResponseEntity.ok().build();
    }

    // 게시판 - 삭제
    @DeleteMapping("/{id}")
    // 추가: Authentication 파라미터
    public ResponseEntity<Void> deletePost(@PathVariable Integer id, Authentication authentication) throws AccessDeniedException {
        // 추가: 현재 로그인한 사용자의 userNo를 알아냅니다.
        UserVO userVO = (UserVO) authentication.getPrincipal();
        Integer currentUserId = userVO.getUserNo();

        // 수정: 서비스에 현재 사용자 ID를 함께 전달합니다.
        postService.deletePost(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // 현재 로그인한 사용자의 정보를 반환
    @GetMapping("/api/user/me")
    public ResponseEntity<UserVO> getCurrentUser(Authentication authentication) {
        // 로그인 안 한 상태면 null을 반환하거나 에러
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // 신분증에서 UserVO를 꺼내서 그대로 반환
        UserVO currentUser = (UserVO) authentication.getPrincipal();
        return ResponseEntity.ok(currentUser);
    }
}   
