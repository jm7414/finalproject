package lx.project.dementia_care.vo;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor //기본생성자 만들어준대요
public class ConnectVO {
	private Integer connectionNo; // connection_no
	private Integer patientNo; // patient_no (필수)
	private Integer guardian1No; // guardian1_no (필수)
	private Integer guardian2No; // guardian2_no (선택)
	private Integer guardian3No; // guardian3_no (선택)
}
