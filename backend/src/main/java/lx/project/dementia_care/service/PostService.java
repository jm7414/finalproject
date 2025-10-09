package lx.project.dementia_care.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lx.project.dementia_care.dto.PostResponseDto;
import lx.project.dementia_care.dto.PostListDto;
import lx.project.dementia_care.dto.PostRequestDto;
import lx.project.dementia_care.entity.Post;
import lx.project.dementia_care.repository.PostRepository;


import java.util.List;                         
import java.util.stream.Collectors;             

import java.util.Collections; // 임시 댓글 목록용

@Service
public class PostService {

    private final PostRepository postRepository;

    @Autowired
    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    // post를 가져옴
    @Transactional(readOnly = true)
    public PostResponseDto findPostById(Integer postId) {
        Post postEntity = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId));
        String authorName = "김병욱";
        String authorProfileImage = "/images/Missing1.jpg";
        return PostResponseDto.builder()
                .id(postEntity.getPostId())
                .author(authorName)
                .authorProfileImage(authorProfileImage)
                .date(postEntity.getCreatedAt())
                .title(postEntity.getTitle())
                .content(postEntity.getContent())
                .image(postEntity.getImage())
                // .likes(postEntity.getLikes())    미구현
                .views(postEntity.getViews())
                .comments(Collections.emptyList()) // 댓글보이게 해야 함
                .build();
    }

    // board를 가져옴
    @Transactional(readOnly = true)
    public List<PostListDto> findAllPosts() {
        List<Post> posts = postRepository.findAll();
        return posts.stream()
                .map(post -> PostListDto.builder()
                        .id(post.getPostId())
                        .title(post.getTitle())
                        .views(post.getViews() != null ? post.getViews() : 0) // DB에 조회수가 null일 경우 0으로 처리 이것도 일단 임시
                        .createdAt(post.getCreatedAt())
                        .author("김병욱 (PostService)")
                        .comments(0)          
                        .likes(0)             
                        .build())
                .collect(Collectors.toList());
    }

    // write 하는 함수
    @Transactional
        public Integer createPost(PostRequestDto requestDto) {
            Post newPost = new Post();
            newPost.setTitle(requestDto.getTitle());
            newPost.setContent(requestDto.getContent());

            // 로그인한 사용자의 ID 설정 나중에 로그인 기능 넣기
            newPost.setUserId(1);

            Post savedPost = postRepository.save(newPost);
            return savedPost.getPostId();
        }

    // 수정 함수
    @Transactional
    public void updatePost(Integer postId, PostRequestDto requestDto) {
        Post postToUpdate = postRepository.findById(postId).orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId));
        postToUpdate.setTitle(requestDto.getTitle());
        postToUpdate.setContent(requestDto.getContent());
    }

    // 삭제 함수
    @Transactional
    public void deletePost(Integer postId) {
        if (!postRepository.existsById(postId)) {
            throw new IllegalArgumentException("해당 게시글이 없습니다. id=" + postId);
        }
        postRepository.deleteById(postId);
    }        
}