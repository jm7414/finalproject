// src/main/java/lx/project/dementia_care/dto/HospitalDTO.java
package lx.project.dementia_care.dto;

import lombok.Data;

@Data
public class HospitalDTO {

    /** 병원명 */
    private String name;

    /** 주소 */
    private String address;

    /** 전화번호 */
    private String tel;

    /** 위도(YPos) */
    private double lat;

    /** 경도(XPos) */
    private double lng;

    /** 기준점에서의 거리(km) - 서비스에서 계산해서 세팅 */
    private double distanceKm;
}
