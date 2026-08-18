package com.investory.market.presentation.dto.response;


import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Security;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SecurityResponse(
        Long securityId,
        String securityCode,
        String securityName,
        MarketType marketType,
        String sectorCode,
        String sectorName,
        String industryName,
        LocalDate listedDate,
        LocalDate delistedDate,
        boolean active,
        LocalDateTime updatedAt
) {
    public static SecurityResponse from(Security security) {
        return new SecurityResponse(
                security.getSecurityId(), security.getSecurityCode(), security.getSecurityName(), security.getMarketType(),
                security.getSectorCode(), security.getSectorName(), security.getStdIdstClsfName(),
                security.getListedDate(), security.getDelistedDate(), security.isActive(), security.getUpdatedAt()
        );
    }
}
