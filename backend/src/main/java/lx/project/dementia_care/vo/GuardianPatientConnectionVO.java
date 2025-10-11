package lx.project.dementia_care.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GuardianPatientConnectionVO {
    private Integer connectionNo;
    private Integer patientNo;
    private Integer guardian1No;
    private Integer guardian2No;
    private Integer guardian3No;

    @Override
    public String toString() {
        return "GuardianPatientConnectionVO [connectionNo=" + connectionNo 
                + ", patientNo=" + patientNo + ", guardian1No=" + guardian1No 
                + ", guardian2No=" + guardian2No + ", guardian3No=" + guardian3No + "]";
    }
}

