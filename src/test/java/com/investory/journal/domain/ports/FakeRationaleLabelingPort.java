package com.investory.journal.domain.ports;

import com.investory.journal.domain.constant.RationaleLabelType;

public class FakeRationaleLabelingPort implements RationaleLabelingPort {

    private RationaleLabelType nextLabel = RationaleLabelType.UNCLASSIFIED;

    public void setNextLabel(RationaleLabelType label) {
        this.nextLabel = label;
    }

    @Override
    public RationaleLabelType classify(String rationaleText) {
        return nextLabel;
    }
}
