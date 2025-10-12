package lx.project.dementia_care.controller;

import lx.project.dementia_care.vo.UserVO;
import lx.project.dementia_care.dto.PostListDto;
import lx.project.dementia_care.dto.PostRequestDto;
import lx.project.dementia_care.dto.PostResponseDto;
import lx.project.dementia_care.service.PostService;

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
    public ResponseEntity<Integer> createPost(@RequestBody PostRequestDto requestDto) {
        Integer newPostId = postService.createPost(requestDto);
        return new ResponseEntity<>(newPostId, HttpStatus.CREATED);
    }

    // 게시판 - 수정
    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Integer id, @RequestBody PostRequestDto requestDto) {
        postService.updatePost(id, requestDto);
        return ResponseEntity.ok().build(); // 200
    }

    // 게시판 - 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Integer id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();  // 204
    }    

}