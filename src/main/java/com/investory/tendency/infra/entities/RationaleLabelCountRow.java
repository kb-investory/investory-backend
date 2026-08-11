package com.investory.tendency.infra.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RationaleLabelCountRow {
    // DB 원본 문자열을 그대로 받는다(대소문자가 섞여 저장될 수 있어 enum으로 바로 매핑하지 않음).
    // 대소문자 정규화 + enum 변환은 RationaleLabelStatsRepositoryImpl에서 처리한다.
    private String rationaleLabel;
    private long count;
}
