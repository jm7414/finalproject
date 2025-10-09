package lx.project.dementia_care.service;

import lx.project.dementia_care.dto.MissingPostDto;
import lx.project.dementia_care.entity.MissingPost;
import lx.project.dementia_care.repository.MissingPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MissingPostService {

    private final MissingPostRepository missingPostRepository;
    // TODO: private final UserRepository userRepository; // 나중에 사용자 정보 조회를 위해 추가

    public MissingPostService(MissingPostRepository missingPostRepository) {
        this.missingPostRepository = missingPostRepository;
    }

    @Transactional(readOnly = true)
    public List<MissingPostDto> findAllMissingPosts() {
        List<MissingPost> posts = missingPostRepository.findAll();

        return posts.stream().map(post -> {
            // TODO: post.getPatientUserNo()로 실제 환자 정보를 조회해서 이름, 나이를 가져와야 합니다.
            String patientName = "김순자 (임시)";
            int patientAge = 78;

            return MissingPostDto.builder()
                .id(post.getMissingPostId())
                .name(patientName)
                .age(patientAge)
                .photoPath(post.getPhotoPath())
                .reportedAt(post.getReportedAt())
                .viewCount(0) // TODO: 조회수 기능 구현 필요
                .build();
        }).collect(Collectors.toList());
    }

    // TODO: '함께하기' 참여자 추가 로직 구현
    public void joinSearch(Integer postId, Integer userId) {
        System.out.println(userId + "번 사용자가 " + postId + "번 실종자 찾기에 참여했습니다.");
        // search_participants 테이블에 데이터를 INSERT하는 로직이 들어갑니다.
    }
    
    // TODO: 상세 정보 조회 로직 구현
}