package com.investory.broker.infra.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AccountSyncBatchRow {
    private Long syncBatchId;
    private Long connectionId;
}
