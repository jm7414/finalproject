package lx.project.dementia_care.dao;

import lx.project.dementia_care.vo.ReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportDAO {

	ReportVO findByPatientPeriod(@Param("patientId") Long patientId, @Param("periodType") String periodType,
			@Param("periodKey") String periodKey);

	ReportVO upsertReturning(@Param("periodId") Integer periodId, @Param("patientId") Long patientId,
			@Param("content") String content, @Param("periodType") String periodType,
			@Param("periodKey") String periodKey, @Param("sourceHash") String sourceHash,
			@Param("metricsJson") String metricsJson, @Param("sectionsJson") String sectionsJson,
			@Param("chartPrefsJson") String chartPrefsJson, @Param("generatedBy") String generatedBy);
	
}
