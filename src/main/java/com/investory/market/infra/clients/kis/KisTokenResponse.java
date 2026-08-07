package com.investory.market.infra.clients.kis;

import lombok.Data;

@Data
public class KisTokenResponse {
    private String access_token;
    private String token_type;
    private Integer expires_in;
}
