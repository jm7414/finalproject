// src/main/java/lx/project/dementia_care/dto/TotalSupportDTO.java
package lx.project.dementia_care.dto;

import lombok.Data;

/**
 * 지자체 복지서비스(종합지원) 검색 조건을 담는 DTO
 * 실제 파라미터명은 data.go.kr 지자체복지서비스 OpenAPI 문서 기준으로 맞게 사용하면 됨.
 */
@Data
public class TotalSupportDTO {

    // 시/도, 시/군/구 이름 등 (문서에서 제공하는 필터에 맞춰 사용)
    // 필요하면 siDoCd, sggCd 같은 코드 필드도 추가 가능.
    private String localGovNm;          // 예: "서울특별시 강남구"

    // 생애주기, 가구유형, 장애유형, 대상특성 등 (API 문서 기준)
    private String lifeArray;           // 생애주기
    private String charTrgterArray;     // 가구유형
    private String obstrTyArray;        // 장애유형
    private String trgterIndvdlArray;   // 대상특성

    // 검색 키워드(서비스명 등)
    private String sprtBizNm;           // 복지서비스명 키워드

    // 페이징
    private Integer pageNo;             // 페이지 번호
    private Integer numOfRows;          // 한 페이지 건수

    // 기본값 처리용 헬퍼
    public int getSafePageNo() {
        return (pageNo == null || pageNo < 1) ? 1 : pageNo;
    }

    public int getSafeNumOfRows() {
        return (numOfRows == null || numOfRows < 1) ? 10 : numOfRows;
    }
}
