package com.kbinvestory.backend.account.presentation.dto.request;

import com.kbinvestory.backend.account.domain.services.dto.query.GetBrokersQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetBrokersRequest {
    private String query;

    public GetBrokersQuery toQuery() {
        return new GetBrokersQuery(query);
    }
}
