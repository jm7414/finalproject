// src/main/java/lx/project/dementia_care/dto/HeartCareDTO.java
package lx.project.dementia_care.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class HeartCareDTO {
    @JsonProperty("기관명")
    private String institutionName;
    
    @JsonProperty("기관구분")
    private String institutionType;
    
    @JsonProperty("주소")
    private String address;
    
    @JsonProperty("전화번호")
    private String phone;
    
    @JsonProperty("홈페이지주소")
    private String homepage;
    
    // 거리 계산용
    private Double distance;
    
    // 카카오맵 좌표
    private String x; // 경도
    private String y; // 위도
}
