// package lx.project.dementia_care.controller;

// import java.io.File;
// import java.io.IOException;
// import java.util.Map;
// import java.util.UUID;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.multipart.MultipartFile;

// @RestController
// @RequestMapping("/api")
// public class FileController {

//     // application.properties 파일에서 파일 저장 경로를 읽어옵니다.
//     @Value("${file.upload-dir}")
//     private String uploadDir;

//     /**
//      * 게시물 이미지 업로드를 처리하는 API
//      * @param file 프론트엔드에서 'file'이라는 이름으로 보낸 이미지 파일
//      * @return 저장된 이미지의 웹 경로 (예: /images/uuid_파일명.jpg)
//      */
//     @PostMapping("/posts/upload-image")
//     public ResponseEntity<?> uploadPostImage(@RequestParam("file") MultipartFile file) {
//         // 1. 파일이 비어있는지 확인
//         if (file.isEmpty()) {
//             return ResponseEntity.badRequest().body("업로드할 파일을 선택해주세요.");
//         }

//         try {
//             // 2. 다른 파일과 이름이 겹치지 않도록 고유한 파일명 생성
//             String originalFileName = file.getOriginalFilename();
//             String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFileName;

//             // 3. 파일을 저장할 전체 경로 설정 (예: C:/uploads/uuid_파일명.jpg)
//             String filePath = uploadDir + File.separator + uniqueFileName;
//             File dest = new File(filePath);

//             // (선택사항) 저장할 폴더가 없으면 생성
//             dest.getParentFile().mkdirs();

//             // 4. 파일을 지정된 경로에 저장
//             file.transferTo(dest);

//             // 5. 프론트엔드에서 접근할 수 있는 웹 경로 생성
//             String webPath = "/images/" + uniqueFileName;

//             // 6. 성공 응답으로 웹 경로를 반환 (JSON 형태)
//             return ResponseEntity.ok(Map.of("imageUrl", webPath));

//         } catch (IOException e) {
//             e.printStackTrace();
//             return ResponseEntity.internalServerError().body("파일 업로드 중 오류가 발생했습니다.");
//         }
//     }
// }