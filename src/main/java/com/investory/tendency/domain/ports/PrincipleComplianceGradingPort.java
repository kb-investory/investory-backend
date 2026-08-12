package com.investory.tendency.domain.ports;

import com.investory.tendency.domain.constant.PrincipleComplianceGrade;
import com.investory.tendency.domain.ports.dto.PrincipleTradingSummary;

public interface PrincipleComplianceGradingPort {
    PrincipleComplianceGrade grade(String principleText, PrincipleTradingSummary summary);
}
