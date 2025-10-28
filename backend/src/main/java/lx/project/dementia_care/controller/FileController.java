package lx.project.dementia_care.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/upload")
public class FileController {

    @PostMapping("/post-image")
    public ResponseEntity<?> uploadPostImage(@RequestParam("file") MultipartFile file) {
        
        System.out.println("=== 'src' 폴더에 직접 저장 시도 ===");
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "업로드할 파일을 선택해주세요."));
        }

        try {
            // 1. 프로젝트의 루트 경로를 가져옵니다. (예: C:\Study\lastProject\backend)
            String projectPath = new File("").getAbsolutePath();

            // 2. 루트 경로에 'src' 이하 경로를 직접 문자열로 조합합니다.
            Path uploadPath = Paths.get(projectPath + "/src/main/resources/uploads/images/");
            
            String originalFileName = file.getOriginalFilename();
            String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path filePath = uploadPath.resolve(uniqueFileName);
            file.transferTo(filePath.toFile());

            String webPath = "https://localhost:8080/images/" + uniqueFileName;

            System.out.println("'src' 폴더에 저장 성공: " + filePath.toString());

            return ResponseEntity.ok(Map.of("success", true, "imageUrl", webPath));

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "파일 업로드 중 오류가 발생했습니다."));
        }
    }
}