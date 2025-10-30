// src/main/java/lx/project/dementia_care/dao/ReportDAO.java
package lx.project.dementia_care.dao;

import lx.project.dementia_care.vo.ReportVO;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ReportDAO {

	// ① 조회: 컬럼 별칭을 VO 프로퍼티와 1:1 매칭 (camelCase 의존 X)
	@Select("""
			SELECT
			  report_id              AS reportId,
			  period_id              AS periodId,
			  patient_id             AS patientId,
			  content,
			  period_type            AS periodType,
			  period_key             AS periodKey,
			  version,
			  generated_at           AS generatedAt,
			  generated_by           AS generatedBy,
			  locked,
			  source_hash            AS sourceHash,
			  metrics::text          AS metrics,       -- String으로 받기
			  sections::text         AS sections,      -- String으로 받기
			  chart_prefs::text      AS chartPrefs,    -- String으로 받기 (별칭 중요)
			  created_at             AS createdAt,
			  updated_at             AS updatedAt
			FROM report
			WHERE patient_id = #{patientId}
			  AND period_type = #{periodType}
			  AND period_key  = #{periodKey}
			LIMIT 1
			""")
	ReportVO findByPatientPeriod(@Param("patientId") Long patientId, @Param("periodType") String periodType,
			@Param("periodKey") String periodKey);

	// ② upsertReturning: @Insert 말고 **@Select** 로 바꿔야 함!
	@Select("""
			INSERT INTO report (
			  period_id, patient_id, content,
			  period_type, period_key,
			  generated_at, generated_by, locked,
			  source_hash, metrics, sections, chart_prefs
			) VALUES (
			  #{periodId}, #{patientId}, #{content},
			  #{periodType}, #{periodKey},
			  NOW(), #{generatedBy}, FALSE,
			  #{sourceHash},
			  CAST(#{metricsJson}    AS jsonb),
			  CAST(#{sectionsJson}   AS jsonb),
			  CAST(#{chartPrefsJson} AS jsonb)
			)
			ON CONFLICT (patient_id, period_type, period_key)
			DO UPDATE SET
			  content      = EXCLUDED.content,
			  source_hash  = EXCLUDED.source_hash,
			  metrics      = EXCLUDED.metrics,
			  sections     = EXCLUDED.sections,
			  chart_prefs  = EXCLUDED.chart_prefs,
			  generated_by = EXCLUDED.generated_by,
			  generated_at = NOW(),
			  version      = report.version + 1,
			  updated_at   = NOW()
			RETURNING
			  report_id              AS reportId,
			  period_id              AS periodId,
			  patient_id             AS patientId,
			  content,
			  period_type            AS periodType,
			  period_key             AS periodKey,
			  version,
			  generated_at           AS generatedAt,
			  generated_by           AS generatedBy,
			  locked,
			  source_hash            AS sourceHash,
			  metrics::text          AS metrics,
			  sections::text         AS sections,
			  chart_prefs::text      AS chartPrefs,
			  created_at             AS createdAt,
			  updated_at             AS updatedAt
			""")
	ReportVO upsertReturning(@Param("periodId") Integer periodId, @Param("patientId") Long patientId,
			@Param("content") String content, @Param("periodType") String periodType,
			@Param("periodKey") String periodKey, @Param("sourceHash") String sourceHash,
			@Param("metricsJson") String metricsJson, @Param("sectionsJson") String sectionsJson,
			@Param("chartPrefsJson") String chartPrefsJson, @Param("generatedBy") String generatedBy);

	// (선택) 별도로 insert/update를 유지한다면, 혼용하지 말고 위 upsert만 쓰세요.
}
