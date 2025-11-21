package lx.project.dementia_care.dto;

import lombok.Data;

/**
 * 지자체 복지서비스 조회용 DTO
 *  - 공공데이터포털: 한국사회보장정보원_지자체복지서비스
 *  - LcgvWelfarelist 파라미터 매핑
 */
@Data
public class TotalSupportDTO {

    // 지자체명 (예: "서울특별시", "서울특별시 구로구")
    private String localGovNm;

    // 생애주기
    private String lifeArray;

    // 가구유형
    private String charTrgterArray;

    // 장애유형
    private String obstrTyArray;

    // 대상자 구분
    private String trgterIndvdlArray;

    // 서비스명 검색
    private String sprtBizNm;

    // 페이지 번호
    private Integer pageNo = 1;

    // 페이지당 건수
    private Integer numOfRows = 100;
}
