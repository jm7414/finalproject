// src/main/java/lx/project/dementia_care/dao/ReportDAO.java
package lx.project.dementia_care.dao;

import lx.project.dementia_care.vo.ReportVO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ReportDAO {

	// 1) 조회 : JSONB 컬럼은 문자열로 읽어오도록 ::text 캐스팅
	@Select("""
			    SELECT
			      report_id,
			      period_id,
			      patient_id,
			      content,
			      period_type,
			      period_key,
			      version,
			      generated_at,
			      generated_by,
			      locked,
			      source_hash,
			      metrics::text      AS metrics,
			      sections::text     AS sections,
			      chart_prefs::text  AS chart_prefs,
			      created_at,
			      updated_at
			    FROM report
			    WHERE patient_id = #{patientId}
			      AND period_type = #{periodType}
			      AND period_key  = #{periodKey}
			    LIMIT 1
			""")
	ReportVO findByPatientPeriod(@Param("patientId") Long patientId, @Param("periodType") String periodType,
			@Param("periodKey") String periodKey);

	// 2) INSERT : JSONB 컬럼에 문자열을 넣을 때는 반드시 CAST(... AS jsonb)
	@Insert("""
			    INSERT INTO report (
			      period_id, patient_id, content,
			      period_type, period_key,
			      generated_at, generated_by, locked,
			      source_hash, metrics, sections, chart_prefs
			    ) VALUES (
			      #{periodId}, #{patientId}, #{content},
			      #{periodType}, #{periodKey},
			      NOW(), #{generatedBy}, TRUE,
			      #{sourceHash},
			      CAST(#{metricsJson}     AS jsonb),
			      CAST(#{sectionsJson}    AS jsonb),
			      CAST(#{chartPrefsJson}  AS jsonb)
			    )
			""")
	int insert(@Param("periodId") Integer periodId, @Param("patientId") Long patientId,
			@Param("content") String content, @Param("periodType") String periodType,
			@Param("periodKey") String periodKey, @Param("sourceHash") String sourceHash,
			@Param("metricsJson") String metricsJson, @Param("sectionsJson") String sectionsJson,
			@Param("chartPrefsJson") String chartPrefsJson, @Param("generatedBy") String generatedBy);

	// 3) 재생성(덮어쓰기) : 필요한 필드만 업데이트 + 버전/시간 갱신
	@Update("""
			    UPDATE report
			       SET source_hash  = #{sourceHash},
			           metrics      = CAST(#{metricsJson}     AS jsonb),
			           sections     = CAST(#{sectionsJson}    AS jsonb),
			           chart_prefs  = CAST(#{chartPrefsJson}  AS jsonb),
			           generated_by = #{generatedBy},
			           generated_at = NOW(),
			           version      = version + 1,
			           updated_at   = NOW()
			     WHERE report_id = #{reportId}
			""")
	int updateForRegenerate(@Param("reportId") Integer reportId, @Param("sourceHash") String sourceHash,
			@Param("metricsJson") String metricsJson, @Param("sectionsJson") String sectionsJson,
			@Param("chartPrefsJson") String chartPrefsJson, @Param("generatedBy") String generatedBy);
}
