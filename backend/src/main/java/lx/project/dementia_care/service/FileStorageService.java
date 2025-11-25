package lx.project.dementia_care.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

@Service
public class FileStorageService {

    @Value("${storage.type:local}")
    private String storageType; // local 또는 gcs

    @Value("${gcs.bucket.name:dementia-care-uploads}")
    private String bucketName;

    @Value("${gcs.project.id:}")
    private String projectId;

    private static final String UPLOAD_DIR_IMAGES = "src/main/resources/static/uploads/images/";
    private static final String UPLOAD_DIR_PROFILES = "src/main/resources/static/uploads/profiles/";

    /**
     * 게시글 이미지 업로드
     */
    public String uploadPostImage(MultipartFile file) throws IOException {
        String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());
        
        if ("gcs".equalsIgnoreCase(storageType)) {
            return uploadToGCS(file, "images/" + uniqueFileName);
        } else {
            return uploadToLocal(file, UPLOAD_DIR_IMAGES, uniqueFileName);
        }
    }

    /**
     * 프로필 사진 업로드
     */
    public String uploadProfilePhoto(MultipartFile file) throws IOException {
        String uniqueFileName = generateUniqueFileName(file.getOriginalFilename());
        
        if ("gcs".equalsIgnoreCase(storageType)) {
            return uploadToGCS(file, "profiles/" + uniqueFileName);
        } else {
            return uploadToLocal(file, UPLOAD_DIR_PROFILES, uniqueFileName);
        }
    }

    /**
     * 로컬 파일 시스템에 업로드
     */
    private String uploadToLocal(MultipartFile file, String directory, String fileName) throws IOException {
        String projectPath = new File("").getAbsolutePath();
        Path uploadPath = Paths.get(projectPath, directory);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath.toFile());

        // 로컬에서는 상대 경로 반환
        String relativePath = directory.contains("images") 
            ? "/uploads/images/" + fileName 
            : "/uploads/profiles/" + fileName;
        
        return relativePath;
    }

    /**
     * Google Cloud Storage에 업로드
     */
    private String uploadToGCS(MultipartFile file, String objectName) throws IOException {
        try {
            Storage storage = StorageOptions.newBuilder()
                .setProjectId(projectId.isEmpty() ? null : projectId)
                .build()
                .getService();

            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

            storage.create(blobInfo, file.getBytes());

            // GCS 공개 URL 반환
            return String.format("https://storage.googleapis.com/%s/%s", bucketName, objectName);
            
        } catch (Exception e) {
            throw new IOException("GCS 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 고유한 파일명 생성
     */
    private String generateUniqueFileName(String originalFilename) {
        return UUID.randomUUID().toString() + "_" + originalFilename;
    }
}


