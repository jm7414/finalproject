package lx.project.dementia_care.controller;

import lx.project.dementia_care.dto.MissingPostDto;
import lx.project.dementia_care.service.MissingPostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/missing-posts")
public class MissingPostController {

    private final MissingPostService missingPostService;

    public MissingPostController(MissingPostService missingPostService) {
        this.missingPostService = missingPostService;
    }

    // 실종자 목록 전체 조회 API
    @GetMapping
    public List<MissingPostDto> getAllMissingPosts() {
        return missingPostService.findAllMissingPosts();
    }

    // '함께하기' API
    @PostMapping("/{id}/join")
    public ResponseEntity<Void> joinSearch(@PathVariable Integer id) {
        // TODO: 실제 로그인한 사용자의 ID를 가져와서 두 번째 인자로 넘겨주어야 합니다.
        Integer currentUserId = 3; // '박철수'님의 ID로 임시 하드코딩
        missingPostService.joinSearch(id, currentUserId);
        return ResponseEntity.ok().build();
    }
}