package com.kbinvestory.backend.account.presentation.dto.request;

import com.kbinvestory.backend.account.domain.services.dto.command.CreateBrokerConnectionCommand;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateBrokerConnectionRequest {
    // TODO: JWT/Principal 도입되면 userId는 요청 바디에서 제거하고 인증 컨텍스트에서 가져오도록 변경
    private Long userId;
    private Long providerId;
    private String loginId;
    private String password;

    public CreateBrokerConnectionCommand toCommand() {
        return new CreateBrokerConnectionCommand(userId, providerId, loginId, password);
    }
}