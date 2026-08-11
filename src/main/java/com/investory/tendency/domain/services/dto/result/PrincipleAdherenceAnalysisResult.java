package com.investory.tendency.domain.services.dto.result;

import com.investory.tendency.domain.constant.PrincipleAdherenceType;

import java.math.BigDecimal;
import java.util.List;

public record PrincipleAdherenceAnalysisResult(
    BigDecimal totalOpportunities,
    BigDecimal totalCompliance,
    BigDecimal complianceRate,       // nullable — INDETERMINATE일 때 null
    PrincipleAdherenceType type,
    List<NumericItemResult> numericItems,
    List<AbstractItemResult> abstractItems,
    List<ExcludedItemResult> excludedItems
) {
    public static PrincipleAdherenceAnalysisResult indeterminate() {
        return indeterminate(List.of(), List.of(), List.of());
    }

    public static PrincipleAdherenceAnalysisResult indeterminate(
            List<NumericItemResult> numericItems, List<AbstractItemResult> abstractItems, List<ExcludedItemResult> excludedItems) {
        return new PrincipleAdherenceAnalysisResult(BigDecimal.ZERO, BigDecimal.ZERO, null,
                PrincipleAdherenceType.INDETERMINATE, numericItems, abstractItems, excludedItems);
    }
}
