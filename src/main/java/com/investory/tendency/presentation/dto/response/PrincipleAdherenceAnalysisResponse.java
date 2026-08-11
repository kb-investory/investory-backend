package com.investory.tendency.presentation.dto.response;

import com.investory.tendency.domain.constant.PrincipleAdherenceType;
import com.investory.tendency.domain.services.dto.result.AbstractItemResult;
import com.investory.tendency.domain.services.dto.result.ExcludedItemResult;
import com.investory.tendency.domain.services.dto.result.NumericItemResult;
import com.investory.tendency.domain.services.dto.result.PrincipleAdherenceAnalysisResult;

import java.math.BigDecimal;
import java.util.List;

public record PrincipleAdherenceAnalysisResponse(
    BigDecimal totalOpportunities,
    BigDecimal totalCompliance,
    BigDecimal complianceRate,
    PrincipleAdherenceType type,
    List<NumericItemResult> numericItems,
    List<AbstractItemResult> abstractItems,
    List<ExcludedItemResult> excludedItems
) {
    public static PrincipleAdherenceAnalysisResponse from(PrincipleAdherenceAnalysisResult result) {
        return new PrincipleAdherenceAnalysisResponse(
                result.totalOpportunities(),
                result.totalCompliance(),
                result.complianceRate(),
                result.type(),
                result.numericItems(),
                result.abstractItems(),
                result.excludedItems()
        );
    }
}
