package com.investory.market.infra.save;


import com.investory.market.domain.constant.MarketType;
import com.investory.market.domain.model.Security;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * KIS kospi_code.mst / kosdaq_code.mst 한 줄(raw line)을 파싱해서
 *
 * 앞부분(front) = line[0 : length-228] -> 단축코드(9) / 표준코드(12, 사용 안 함) / 한글명
 * 뒷부분(rear)  = 마지막 228byte, KIS 공식 field_specs 순서대로 고정폭 파싱 (상장일자만 사용)
 */
public final class SecurityMstLineParser {

    /** front(단축코드/표준코드/한글명)를 제외한 뒷부분 고정폭 바이트 수 */
    private static final int REAR_LENGTH = 228;

    /**
     * KIS kospi_code.mst / kosdaq_code.mst 뒷부분(228byte) 필드 폭 정의.
     * (그룹코드, 시가총액규모, 지수업종대분류, 지수업종중분류, 지수업종소분류, ... 순서 그대로)
     */
    private static final int[] FIELD_WIDTHS = {
            2, 1, 4, 4, 4,
            1, 1, 1, 1, 1,
            1, 1, 1, 1, 1,
            1, 1, 1, 1, 1,
            1, 1, 1, 1, 1,
            1, 1, 1, 1, 1,
            1, 9, 5, 5, 1,
            1, 1, 2, 1, 1,
            1, 2, 2, 2, 3,
            1, 3, 12, 12, 8,
            15, 21, 2, 7, 1,
            1, 1, 1, 1, 9,
            9, 9, 5, 9, 8,
            9, 3, 1, 1, 1
    };

    // FIELD_WIDTHS 상에서 실제로 사용하는 필드의 인덱스 (0-based)
    private static final int IDX_LISTED_DATE = 49; // 상장일자 (yyyyMMdd)

    private SecurityMstLineParser() {
        // 인스턴스화 방지
    }

    /**
     * 한 줄을 파싱해서 Security 도메인 객체로 변환한다.
     * 라인 형식이 올바르지 않거나 단축코드가 비어있으면 null을 반환한다.
     *
     * 주의: mst 파일에는 sector_code/sector_name(업종), industry_name(표준산업분류), delisted_date에
     * 대응하는 원본 데이터가 없어 null로 채워진다. active는 마스터파일에 등재된
     * 종목이므로 true로 기본 설정한다. securityId(내부 숫자 PK)는 DB가 채워준다.
     */
    public static Security parse(String line, MarketType marketType) {
        if (line == null || line.length() <= REAR_LENGTH) {
            return null;
        }

        int splitPoint = line.length() - REAR_LENGTH;
        String front = line.substring(0, splitPoint);
        String rear = line.substring(splitPoint);

        String securityCode = front.length() >= 9 ? front.substring(0, 9).trim() : "";
        String securityName = front.length() > 21 ? front.substring(21).trim() : "";

        if (securityCode.isEmpty()) {
            return null;
        }

        var fields = FixedWidthParser.splitFixedWidth(rear, FIELD_WIDTHS);
        LocalDate listedDate = FixedWidthParser.parseYyyyMmDd(
                (IDX_LISTED_DATE >= 0 && IDX_LISTED_DATE < fields.size()) ? fields.get(IDX_LISTED_DATE) : "");

        LocalDateTime now = LocalDateTime.now();
        return Security.of(
                null, securityCode, securityName, marketType,
                null, null, null, // sector_code / sector_name / industry_name: mst 파일에 없음
                listedDate, null,    // delisted_date: mst 파일에 없음 -> null
                true, now, now
        );
    }
}
