package com.investory.journal.domain.ports;

import com.investory.journal.domain.constant.RationaleLabelType;

public interface RationaleLabelingPort {
    RationaleLabelType classify(String rationaleText);
}
