package com.investory.tendency.infra.entities;

import com.investory.tendency.domain.constant.RationaleLabelType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RationaleLabelCountRow {
    private RationaleLabelType rationaleLabel;
    private long count;
}
