package com.investory.tendency.domain.services;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.List;

// 여러 카테고리(버킷)의 일수를 세어, 최빈 카테고리 비율이 θ(threshold) 이상이면 그 카테고리로,
// 미달이거나 totalDays==0이면 mixedLabel로 분류한다. 카테고리 개수·라벨 타입에 무관하게 동작.
// 3번(손실 대응)·4번(수익 대응) 공유.
//
// 동률 처리 주의: 여러 버킷이 최댓값으로 동률이면 buckets 리스트에서 먼저 나오는 버킷이 우선한다
// (sequential Stream.max는 동률 시 앞쪽 원소를 유지). 호출측은 원하는 우선순위 순서로 버킷을 넘겨야 한다.
public class ThresholdMajorityLabeler {

    private ThresholdMajorityLabeler() {
    }

    public record Bucket<T>(T label, int count) {
    }

    public static <T> T classify(List<Bucket<T>> buckets, int totalDays, BigDecimal threshold, T mixedLabel) {
        if (totalDays == 0) {
            return mixedLabel;
        }
        Bucket<T> max = buckets.stream()
                .max(Comparator.comparingInt(Bucket::count))
                .orElseThrow();
        BigDecimal ratio = BigDecimal.valueOf(max.count()).divide(BigDecimal.valueOf(totalDays), MathContext.DECIMAL64);
        if (ratio.compareTo(threshold) < 0) {
            return mixedLabel;
        }
        return max.label();
    }
}
