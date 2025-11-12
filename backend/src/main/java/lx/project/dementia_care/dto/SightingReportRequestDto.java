package lx.project.dementia_care.dto;

import lombok.Data;

/**
 * 프론트엔드(SightingReportWrite.vue)에서 '제보 생성/수정' 시
 * 백엔드로 전송하는 데이터를 담는 DTO
 */
@Data
public class SightingReportRequestDto {
    
    // SightingReportWrite.vue의 submitPost() 함수에서 보낼 데이터
    
    private String content;         // 제보 내용
    private String imagePath;       // 업로드된 이미지 경로
    private Integer missingPostId; 
    
    // (추후 예정) 제보 위치 (지도 API로 위도/경도를 받는 경우)
    // private Double latitude;
    // private Double longitude;
}