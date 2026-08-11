package com.investory.journal.infra.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RationaleLabelCountRow {
    private String rationaleLabel;
    private long count;
}
