package com.investory.market.infra.save;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * KIS 마스터파일(kospi_code.mst / kosdaq_code.mst)과 같은 고정폭(fixed-width)
 * 텍스트 라인을 지정된 width 배열에 따라 잘라내기 위한 유틸리티.
 */
public final class FixedWidthParser {

    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private FixedWidthParser() {
        // 인스턴스화 방지
    }

    /**
     * 문자열 line 을 widths 순서대로 잘라 trim 된 값 리스트로 반환한다.
     * line 길이가 widths 총합보다 짧을 경우, 남은 필드는 빈 문자열로 채운다.
     */
    public static List<String> splitFixedWidth(String line, int[] widths) {
        List<String> result = new ArrayList<>(widths.length);
        int pos = 0;
        int length = line.length();

        for (int width : widths) {
            if (pos >= length) {
                result.add("");
                continue;
            }
            int end = Math.min(pos + width, length);
            String token = line.substring(pos, end);
            result.add(token.trim());
            pos = end;
        }
        return result;
    }

    /**
     * "yyyyMMdd" 형식의 문자열을 LocalDate로 변환한다.
     * 값이 비어있거나 0으로만 채워진 경우 null을 반환한다.
     */
    public static LocalDate parseYyyyMmDd(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.chars().allMatch(c -> c == '0')) {
            return null;
        }
        try {
            return LocalDate.parse(trimmed, YYYYMMDD);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 빈 문자열이면 null, 아니면 원본 문자열을 반환한다.
     */
    public static String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
