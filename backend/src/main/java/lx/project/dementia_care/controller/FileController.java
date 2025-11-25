package lx.project.dementia_care.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lx.project.dementia_care.dao.UserDAO;
import lx.project.dementia_care.service.FileStorageService;
import lx.project.dementia_care.vo.UserVO;

@RestController
@RequestMapping("/api/upload")
public class FileController {

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private FileStorageService fileStorageService;

    // 기존 API (게시글 이미지용)
    @PostMapping("/post-image")
    public ResponseEntity<?> uploadPostImage(@RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "업로드할 파일을 선택해주세요."));
        }

        try {
            String imageUrl = fileStorageService.uploadPostImage(file);
            return ResponseEntity.ok(Map.of("success", true, "imageUrl", imageUrl));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "파일 업로드 중 오류가 발생했습니다."));
        }
    }

    // 지현 추가: 프로필 사진 업로드 API
    @PostMapping("/profile-photo")
    public ResponseEntity<?> uploadProfilePhoto(@RequestParam("file") MultipartFile file) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "업로드할 파일을 선택해주세요."));
        }

        try {
            // 1. 현재 로그인한 사용자 정보 가져오기
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
            }

            UserVO currentUser = (UserVO) auth.getPrincipal();
            Integer userNo = currentUser.getUserNo();

            // 2. 파일 저장 (로컬 또는 GCS)
            String imageUrl = fileStorageService.uploadProfilePhoto(file);

            // 3. DB 업데이트
            UserVO updateUser = new UserVO();
            updateUser.setUserNo(userNo);
            updateUser.setProfilePhoto(imageUrl);
            userDAO.updateById(updateUser);

            // 4. 세션 업데이트
            UserVO freshUser = userDAO.findByUserNo(userNo);
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                freshUser,
                auth.getCredentials(),
                auth.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(newAuth);

            return ResponseEntity.ok(Map.of(
                "success", true, 
                "imageUrl", imageUrl,
                "message", "프로필 사진이 업데이트되었습니다."
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "파일 업로드 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}
